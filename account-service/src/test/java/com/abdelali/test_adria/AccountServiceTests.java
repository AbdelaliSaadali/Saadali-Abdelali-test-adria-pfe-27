package com.abdelali.test_adria;

import com.abdelali.test_adria.domain.AccountStatus;
import com.abdelali.test_adria.service.AccountBusinessException;
import com.abdelali.test_adria.service.AccountService;
import com.abdelali.test_adria.service.dto.*;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@Transactional
class AccountServiceTests {

    @Autowired
    private AccountService accountService;

    @Test
    void shouldCreateAnAccount() {

        AccountResponse account = accountService.create(
                new CreateAccountRequest(
                        "CUSTOMER-001",
                        "EUR",
                        new BigDecimal("5000.00")
                )
        );

        assertThat(account.id()).isNotNull();
        assertThat(account.status()).isEqualTo(AccountStatus.ACTIVE);
        assertThat(account.balance()).isEqualTo(5000.0);
    }

    @Test
    void shouldDebitAnAccount() {

        AccountResponse account = accountService.create(
                new CreateAccountRequest(
                        "CUSTOMER-002",
                        "EUR",
                        new BigDecimal("1000.00")
                )
        );

        AccountResponse updated = accountService.debit(
                account.id(),
                new BigDecimal("250.00")
        );

        assertThat(updated.balance()).isEqualTo(750.0);
    }

    @Test
    void shouldRejectInsufficientBalance() {

        AccountResponse account = accountService.create(
                new CreateAccountRequest(
                        "CUSTOMER-003",
                        "EUR",
                        new BigDecimal("100.00")
                )
        );

        assertThatThrownBy(() -> accountService.debit(
                account.id(),
                new BigDecimal("200.00")
        )).isInstanceOf(AccountBusinessException.class);
    }

    @Test
    void shouldTransferMoneyBetweenAccounts() {

        AccountResponse source = accountService.create(
                new CreateAccountRequest(
                        "CUSTOMER-004",
                        "EUR",
                        new BigDecimal("500.00")
                )
        );

        AccountResponse destination = accountService.create(
                new CreateAccountRequest(
                        "CUSTOMER-005",
                        "EUR",
                        new BigDecimal("100.00")
                )
        );

        TransferResponse result = accountService.transfer(
                source.id(),
                destination.id(),
                new BigDecimal("150.00")
        );

        assertThat(result.source().balance()).isEqualTo(350.0);
        assertThat(result.destination().balance()).isEqualTo(250.0);
    }

    @Test
    void shouldRejectDebitOnBlockedAccount() {

        AccountResponse account = accountService.create(
                new CreateAccountRequest(
                        "CUSTOMER-006",
                        "EUR",
                        new BigDecimal("500.00")
                )
        );

        accountService.changeStatus(
                account.id(),
                AccountStatus.BLOCKED
        );

        assertThatThrownBy(() -> accountService.debit(
                account.id(),
                new BigDecimal("50.00")
        )).isInstanceOf(AccountBusinessException.class);
    }
}