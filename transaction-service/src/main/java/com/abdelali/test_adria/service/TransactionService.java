package com.abdelali.test_adria.service;

import com.abdelali.test_adria.clients.AccountClient;
import com.abdelali.test_adria.clients.AccountClient.AccountSnapshot;
import com.abdelali.test_adria.clients.AccountClient.AmountPayload;
import com.abdelali.test_adria.clients.AccountClient.TransferPayload;
import com.abdelali.test_adria.domain.Transaction;
import com.abdelali.test_adria.repository.TransactionRepository;
import com.abdelali.test_adria.service.dto.CreateTransactionRequest;
import com.abdelali.test_adria.service.dto.CreateTransferRequest;
import com.abdelali.test_adria.service.dto.TransactionResponse;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Currency;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * Single business layer for all future REST, GraphQL and MCP adapters.
 *
 * Local persistence rolls back when an account operation fails. Because
 * Account-Service owns a separate database, the two services are not
 * distributed-transaction atomic; a production version requires a
 * durable saga / idempotency mechanism for failure recovery.
 */
@Service
public class TransactionService {

    private final TransactionRepository repository;
    private final AccountClient accounts;
    private final BigDecimal maxAmount;

    public TransactionService(
            TransactionRepository repository,
            AccountClient accounts,
            @Value("${bank.transaction.max-amount:1000}") BigDecimal maxAmount) {
        if (maxAmount == null || maxAmount.signum() <= 0) {
            throw new IllegalArgumentException("Maximum transaction amount must be positive");
        }
        this.repository = repository;
        this.accounts = accounts;
        this.maxAmount = maxAmount;
    }

    @Transactional
    public TransactionResponse create(CreateTransactionRequest request) {
        if (request == null) {
            throw new TransactionBusinessException("Transaction request is required");
        }

        UUID accountId = requireId(request.accountId(), "Account ID");
        BigDecimal amount = validAmount(request.amount());
        String currency = validCurrency(request.currency());
        String category = requiredText(request.merchantCategory(), "Merchant category");
        String country = validCountry(request.country());

        // Read account data before writing; never trust client-supplied balances.
        checkedAccount(accountId, currency, amount);

        Transaction saved = repository.saveAndFlush(newTransaction(
                accountId, amount, currency, category, country));

        // The account service atomically rechecks its balance under a DB lock.
        // A failed debit causes this local transaction to roll back.
        accounts.debit(accountId, new AmountPayload(amount));
        return toResponse(saved);
    }

    @Transactional
    public TransactionResponse transfer(CreateTransferRequest request) {
        if (request == null) {
            throw new TransactionBusinessException("Transfer request is required");
        }

        UUID sourceId = requireId(request.sourceAccountId(), "Source account ID");
        UUID destinationId = requireId(request.destinationAccountId(), "Destination account ID");

        if (sourceId.equals(destinationId)) {
            throw new TransactionBusinessException("Cannot transfer to the same account");
        }

        BigDecimal amount = validAmount(request.amount());
        String currency = validCurrency(request.currency());
        String country = validCountry(request.country());

        checkedAccount(sourceId, currency, amount);
        checkedAccount(destinationId, currency, BigDecimal.ZERO);

        // The mandatory transaction schema records the source account;
        // the destination is forwarded to the account-service transfer operation.
        Transaction saved = repository.saveAndFlush(newTransaction(
                sourceId, amount, currency, "TRANSFER", country));

        accounts.transfer(new TransferPayload(sourceId, destinationId, amount));
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public TransactionResponse getById(UUID id) {
        UUID validId = requireId(id, "Transaction ID");
        return repository.findById(validId)
                .map(this::toResponse)
                .orElseThrow(() -> new TransactionNotFoundException(validId));
    }

    @Transactional(readOnly = true)
    public List<TransactionResponse> history(UUID accountId) {
        return repository.findByAccountIdOrderByCreatedAtDesc(
                        requireId(accountId, "Account ID"))
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private AccountSnapshot checkedAccount(
            UUID accountId, String currency, BigDecimal requiredBalance) {

        AccountSnapshot account = accounts.getAccount(accountId);

        if (account == null || account.id() == null
                || !accountId.equals(account.id())) {
            throw new AccountDependencyException(
                    "Account-Service returned an invalid account response");
        }

        if (!"ACTIVE".equals(account.status())) {
            throw new TransactionBusinessException("Account is not ACTIVE");
        }

        if (!currency.equals(account.currency())) {
            throw new TransactionBusinessException("Account currency does not match transaction");
        }

        if (!Double.isFinite(account.balance()) || account.balance() < 0) {
            throw new AccountDependencyException("Account-Service returned an invalid balance");
        }

        if (BigDecimal.valueOf(account.balance()).compareTo(requiredBalance) < 0) {
            throw new TransactionBusinessException("Insufficient account balance");
        }

        return account;
    }

    private Transaction newTransaction(
            UUID accountId, BigDecimal amount, String currency,
            String merchantCategory, String country) {
        return Transaction.builder()
                .accountId(accountId)
                .amount(amount)
                .currency(currency)
                .merchantCategory(merchantCategory)
                .country(country)
                .build();
    }

    private BigDecimal validAmount(BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) {
            throw new TransactionBusinessException("Transaction amount must be strictly positive");
        }

        BigDecimal normalized;
        try {
            normalized = amount.setScale(2, RoundingMode.UNNECESSARY);
        } catch (ArithmeticException e) {
            throw new TransactionBusinessException("At most two decimal places are allowed");
        }

        if (normalized.compareTo(maxAmount) > 0) {
            throw new TransactionBusinessException(
                    "Transaction exceeds configured maximum: " + maxAmount);
        }
        return normalized;
    }

    private String validCurrency(String value) {
        String currency = requiredText(value, "Currency").toUpperCase(Locale.ROOT);
        try {
            Currency.getInstance(currency);
            return currency;
        } catch (IllegalArgumentException ex) {
            throw new TransactionBusinessException("Invalid ISO currency: " + value);
        }
    }

    private String validCountry(String value) {
        String country = requiredText(value, "Country").toUpperCase(Locale.ROOT);
        if (!Set.of(Locale.getISOCountries()).contains(country)) {
            throw new TransactionBusinessException("Invalid ISO country: " + value);
        }
        return country;
    }

    private String requiredText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new TransactionBusinessException(field + " is required");
        }
        return value.trim();
    }

    private UUID requireId(UUID id, String field) {
        if (id == null) {
            throw new TransactionBusinessException(field + " is required");
        }
        return id;
    }

    private TransactionResponse toResponse(Transaction transaction) {
        return new TransactionResponse(
                transaction.getId(),
                transaction.getAccountId(),
                transaction.getAmount(),
                transaction.getCurrency(),
                transaction.getMerchantCategory(),
                transaction.getCountry(),
                transaction.getCreatedAt()
        );
    }
}
