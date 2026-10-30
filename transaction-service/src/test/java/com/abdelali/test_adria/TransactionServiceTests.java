package com.abdelali.test_adria;

import com.abdelali.test_adria.clients.AccountClient;
import com.abdelali.test_adria.clients.AccountClient.AccountSnapshot;
import com.abdelali.test_adria.clients.AccountClient.AmountPayload;
import com.abdelali.test_adria.clients.AccountClient.TransferPayload;
import com.abdelali.test_adria.domain.Transaction;
import com.abdelali.test_adria.repository.TransactionRepository;
import com.abdelali.test_adria.service.TransactionBusinessException;
import com.abdelali.test_adria.service.TransactionService;
import com.abdelali.test_adria.service.dto.CreateTransactionRequest;
import com.abdelali.test_adria.service.dto.CreateTransferRequest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTests {

    @Mock TransactionRepository repository;
    @Mock AccountClient accounts;

    private TransactionService service;

    private final UUID accountId = UUID.randomUUID();
    private final UUID destinationId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new TransactionService(repository, accounts, new BigDecimal("1000"));
    }

    @Test
    void successfulTransactionPersistsBeforeDebiting() {
        when(accounts.getAccount(accountId)).thenReturn(active(accountId, "EUR", 500));
        simulatePersistence();

        var result = service.create(request(accountId, "250.00", "EUR"));

        assertThat(result.id()).isNotNull();
        assertThat(result.amount()).isEqualByComparingTo("250.00");

        InOrder order = inOrder(accounts, repository);
        order.verify(accounts).getAccount(accountId);
        order.verify(repository).saveAndFlush(any(Transaction.class));
        order.verify(accounts).debit(eq(accountId), eq(new AmountPayload(new BigDecimal("250.00"))));
    }

    @Test
    void zeroAndNegativeAmountsAreRejectedBeforeCallingTheAccountService() {
        assertThatThrownBy(() -> service.create(request(accountId, "0", "EUR")))
                .isInstanceOf(TransactionBusinessException.class)
                .hasMessageContaining("strictly positive");
        assertThatThrownBy(() -> service.create(request(accountId, "-1", "EUR")))
                .isInstanceOf(TransactionBusinessException.class);
        verifyNoInteractions(repository, accounts);
    }

    @Test
    void configuredMaximumIsEnforcedAndInclusive() {
        assertThatThrownBy(() -> service.create(request(accountId, "1000.01", "EUR")))
                .isInstanceOf(TransactionBusinessException.class)
                .hasMessageContaining("configured maximum");
        verifyNoInteractions(repository, accounts);

        when(accounts.getAccount(accountId)).thenReturn(active(accountId, "EUR", 2000));
        simulatePersistence();
        var result = service.create(request(accountId, "1000.00", "EUR"));
        assertThat(result.amount()).isEqualByComparingTo("1000.00");
    }

    @Test
    void tooManyDecimalPlacesAreRejected() {
        assertThatThrownBy(() -> service.create(request(accountId, "12.345", "EUR")))
                .isInstanceOf(TransactionBusinessException.class)
                .hasMessageContaining("decimal places");
        verifyNoInteractions(repository, accounts);
    }

    @Test
    void blockedAccountIsRejectedWithoutPersistence() {
        when(accounts.getAccount(accountId))
                .thenReturn(new AccountSnapshot(accountId, "CUSTOMER", "BLOCKED", "EUR", 500));
        assertThatThrownBy(() -> service.create(request(accountId, "100", "EUR")))
                .isInstanceOf(TransactionBusinessException.class)
                .hasMessageContaining("not ACTIVE");
        verifyNoInteractions(repository);
        verify(accounts, never()).debit(any(), any());
    }

    @Test
    void wrongCurrencyIsRejectedWithoutPersistence() {
        when(accounts.getAccount(accountId)).thenReturn(active(accountId, "MAD", 500));
        assertThatThrownBy(() -> service.create(request(accountId, "100", "EUR")))
                .isInstanceOf(TransactionBusinessException.class)
                .hasMessageContaining("currency");
        verifyNoInteractions(repository);
    }

    @Test
    void insufficientBalanceIsRejectedWithoutPersistence() {
        when(accounts.getAccount(accountId)).thenReturn(active(accountId, "EUR", 20));
        assertThatThrownBy(() -> service.create(request(accountId, "100", "EUR")))
                .isInstanceOf(TransactionBusinessException.class)
                .hasMessageContaining("Insufficient");
        verifyNoInteractions(repository);
    }

    @Test
    void invalidCountryIsRejectedBeforeRemoteCall() {
        var bad = new CreateTransactionRequest(accountId, new BigDecimal("10"),
                "EUR", "SHOPPING", "INVALID");
        assertThatThrownBy(() -> service.create(bad))
                .isInstanceOf(TransactionBusinessException.class)
                .hasMessageContaining("country");
        verifyNoInteractions(repository, accounts);
    }

    @Test
    void transfersCallTheAtomicAccountTransferEndpoint() {
        when(accounts.getAccount(accountId)).thenReturn(active(accountId, "EUR", 500));
        when(accounts.getAccount(destinationId)).thenReturn(active(destinationId, "EUR", 50));
        simulatePersistence();

        var result = service.transfer(new CreateTransferRequest(
                accountId, destinationId, new BigDecimal("120.00"), "EUR", "MA"));

        assertThat(result.merchantCategory()).isEqualTo("TRANSFER");
        assertThat(result.accountId()).isEqualTo(accountId);
        verify(accounts).transfer(new TransferPayload(
                accountId, destinationId, new BigDecimal("120.00")));
        verify(accounts, never()).debit(any(), any());
    }

    @Test
    void transferToSameAccountIsRejected() {
        assertThatThrownBy(() -> service.transfer(new CreateTransferRequest(
                accountId, accountId, new BigDecimal("100"), "EUR", "MA")))
                .isInstanceOf(TransactionBusinessException.class)
                .hasMessageContaining("same account");
        verifyNoInteractions(accounts, repository);
    }

    @Test
    void ifAccountDebitFailsTransactionReportsFailure() {
        when(accounts.getAccount(accountId)).thenReturn(active(accountId, "EUR", 500));
        simulatePersistence();
        doThrow(new IllegalStateException("remote debit failed"))
                .when(accounts).debit(eq(accountId), any(AmountPayload.class));

        assertThatThrownBy(() -> service.create(request(accountId, "100", "EUR")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("remote debit failed");
    }

    private CreateTransactionRequest request(UUID id, String amount, String currency) {
        return new CreateTransactionRequest(id, new BigDecimal(amount), currency, "SHOPPING", "MA");
    }

    private AccountSnapshot active(UUID id, String currency, double balance) {
        return new AccountSnapshot(id, "CUSTOMER", "ACTIVE", currency, balance);
    }

    private void simulatePersistence() {
        when(repository.saveAndFlush(any(Transaction.class))).thenAnswer(invocation -> {
            Transaction t = invocation.getArgument(0);
            t.setId(UUID.randomUUID());
            t.onCreate();
            return t;
        });
    }
}
