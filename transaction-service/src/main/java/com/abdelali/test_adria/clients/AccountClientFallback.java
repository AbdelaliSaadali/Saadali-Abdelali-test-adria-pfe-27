package com.abdelali.test_adria.clients;

import com.abdelali.test_adria.service.AccountDependencyException;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Never return fabricated account data or assume a debit succeeded.
 * A failed Feign call must abort the transaction.
 */
@Component
public class AccountClientFallback implements AccountClient {

    @Override
    public AccountSnapshot getAccount(UUID id) {
        throw unavailable();
    }

    @Override
    public AccountSnapshot debit(UUID id, AmountPayload request) {
        throw unavailable();
    }

    @Override
    public TransferSnapshot transfer(TransferPayload request) {
        throw unavailable();
    }

    private AccountDependencyException unavailable() {
        return new AccountDependencyException(
                "Account-Service is unavailable or rejected the request; transaction aborted"
        );
    }
}
