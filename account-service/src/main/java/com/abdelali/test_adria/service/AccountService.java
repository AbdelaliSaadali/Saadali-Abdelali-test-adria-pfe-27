package com.abdelali.test_adria.service;

import com.abdelali.test_adria.domain.Account;
import com.abdelali.test_adria.domain.AccountStatus;
import com.abdelali.test_adria.repository.AccountRepository;
import com.abdelali.test_adria.service.dto.*;

import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Currency;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AccountService {

    private final AccountRepository repository;

    @Transactional(readOnly = true)
    public List<AccountResponse> findAll() {
        return repository.findAll(Sort.by("customerReference"))
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public AccountResponse findById(UUID id) {
        return toResponse(findAccount(id));
    }

    @Transactional
    public AccountResponse create(CreateAccountRequest request) {

        if (request == null ||
                request.customerReference() == null ||
                request.customerReference().isBlank()) {
            throw new AccountBusinessException(
                    "Customer reference is required"
            );
        }

        String currency = normalizeCurrency(request.currency());
        BigDecimal initialBalance = money(request.initialBalance(), true);

        Account account = Account.builder()
                .customerReference(request.customerReference().trim())
                .currency(currency)
                .status(AccountStatus.ACTIVE)
                .balance(toDouble(initialBalance))
                .build();

        return toResponse(repository.saveAndFlush(account));
    }

    @Transactional
    public AccountResponse changeStatus(UUID id, AccountStatus status) {

        if (status == null) {
            throw new AccountBusinessException("Status is required");
        }

        Account account = findLockedAccount(id);
        account.setStatus(status);

        return toResponse(repository.saveAndFlush(account));
    }

    @Transactional
    public AccountResponse debit(UUID id, BigDecimal amount) {

        BigDecimal value = money(amount, false);
        Account account = findLockedAccount(id);

        ensureActive(account);

        BigDecimal balance = balanceOf(account);

        if (balance.compareTo(value) < 0) {
            throw new AccountBusinessException("Insufficient balance");
        }

        account.setBalance(toDouble(balance.subtract(value)));

        return toResponse(repository.saveAndFlush(account));
    }

    @Transactional
    public AccountResponse credit(UUID id, BigDecimal amount) {

        BigDecimal value = money(amount, false);
        Account account = findLockedAccount(id);

        ensureActive(account);

        account.setBalance(
                toDouble(balanceOf(account).add(value))
        );

        return toResponse(repository.saveAndFlush(account));
    }

    @Transactional
    public TransferResponse transfer(
            UUID sourceId,
            UUID destinationId,
            BigDecimal amount) {

        BigDecimal value = money(amount, false);

        if (sourceId == null || destinationId == null) {
            throw new AccountBusinessException(
                    "Both account identifiers are required"
            );
        }

        if (sourceId.equals(destinationId)) {
            throw new AccountBusinessException(
                    "Source and destination must be different"
            );
        }

        // Lock accounts in a consistent order to reduce deadlocks.
        UUID firstId = sourceId.compareTo(destinationId) < 0
                ? sourceId : destinationId;

        UUID secondId = sourceId.compareTo(destinationId) < 0
                ? destinationId : sourceId;

        Account first = findLockedAccount(firstId);
        Account second = findLockedAccount(secondId);

        Account source = first.getId().equals(sourceId) ? first : second;
        Account destination = first.getId().equals(destinationId)
                ? first : second;

        ensureActive(source);
        ensureActive(destination);

        if (!source.getCurrency().equals(destination.getCurrency())) {
            throw new AccountBusinessException(
                    "Account currencies must match"
            );
        }

        BigDecimal sourceBalance = balanceOf(source);

        if (sourceBalance.compareTo(value) < 0) {
            throw new AccountBusinessException("Insufficient balance");
        }

        source.setBalance(toDouble(sourceBalance.subtract(value)));
        destination.setBalance(
                toDouble(balanceOf(destination).add(value))
        );

        repository.saveAndFlush(source);
        repository.saveAndFlush(destination);

        return new TransferResponse(
                toResponse(source),
                toResponse(destination),
                value
        );
    }

    private Account findAccount(UUID id) {
        if (id == null) {
            throw new AccountBusinessException("Account ID is required");
        }

        return repository.findById(id)
                .orElseThrow(() -> new AccountNotFoundException(id));
    }

    private Account findLockedAccount(UUID id) {
        if (id == null) {
            throw new AccountBusinessException("Account ID is required");
        }

        return repository.findByIdForUpdate(id)
                .orElseThrow(() -> new AccountNotFoundException(id));
    }

    private void ensureActive(Account account) {
        if (account.getStatus() != AccountStatus.ACTIVE) {
            throw new AccountBusinessException(
                    "Account is blocked: " + account.getId()
            );
        }
    }

    private String normalizeCurrency(String currency) {

        if (currency == null || currency.isBlank()) {
            throw new AccountBusinessException("Currency is required");
        }

        String normalized = currency.trim().toUpperCase(Locale.ROOT);

        try {
            Currency.getInstance(normalized);
            return normalized;
        } catch (IllegalArgumentException exception) {
            throw new AccountBusinessException(
                    "Invalid currency: " + currency
            );
        }
    }

    private BigDecimal money(BigDecimal amount, boolean allowZero) {

        if (amount == null) {
            throw new AccountBusinessException("Amount is required");
        }

        if (amount.signum() < 0 || (!allowZero && amount.signum() == 0)) {
            throw new AccountBusinessException(
                    "Amount must be " +
                    (allowZero ? "non-negative" : "strictly positive")
            );
        }

        try {
            return amount.stripTrailingZeros()
                    .setScale(2, RoundingMode.UNNECESSARY);
        } catch (ArithmeticException exception) {
            throw new AccountBusinessException(
                    "Amount cannot have more than two decimal places"
            );
        }
    }

    private BigDecimal balanceOf(Account account) {
        return BigDecimal.valueOf(account.getBalance())
                .setScale(2, RoundingMode.HALF_UP);
    }

    private double toDouble(BigDecimal amount) {
        double value = amount.doubleValue();

        if (!Double.isFinite(value)) {
            throw new AccountBusinessException(
                    "Amount exceeds supported balance range"
            );
        }

        return value;
    }

    private AccountResponse toResponse(Account account) {
        return new AccountResponse(
                account.getId(),
                account.getCustomerReference(),
                account.getStatus(),
                account.getCurrency(),
                account.getBalance()
        );
    }
}