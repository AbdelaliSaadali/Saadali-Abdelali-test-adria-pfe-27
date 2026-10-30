package com.abdelali.test_adria.web.graphql;

import com.abdelali.test_adria.domain.AccountStatus;
import com.abdelali.test_adria.service.AccountService;
import com.abdelali.test_adria.service.dto.*;

import lombok.RequiredArgsConstructor;

import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Controller
@RequiredArgsConstructor
public class AccountGraphQlController {

    private final AccountService accountService;

    @QueryMapping
    public List<AccountResponse> accounts() {
        return accountService.findAll();
    }

    @QueryMapping
    public AccountResponse account(@Argument String id) {
        return accountService.findById(UUID.fromString(id));
    }

    @MutationMapping
    public AccountResponse createAccount(
            @Argument Map<String, Object> input) {

        return accountService.create(new CreateAccountRequest(
                (String) input.get("customerReference"),
                (String) input.get("currency"),
                new BigDecimal((String) input.get("initialBalance"))
        ));
    }

    @MutationMapping
    public AccountResponse debitAccount(
            @Argument String id,
            @Argument String amount) {

        return accountService.debit(
                UUID.fromString(id),
                new BigDecimal(amount)
        );
    }

    @MutationMapping
    public AccountResponse creditAccount(
            @Argument String id,
            @Argument String amount) {

        return accountService.credit(
                UUID.fromString(id),
                new BigDecimal(amount)
        );
    }

    @MutationMapping
    public AccountResponse changeAccountStatus(
            @Argument String id,
            @Argument AccountStatus status) {

        return accountService.changeStatus(
                UUID.fromString(id),
                status
        );
    }

    @MutationMapping
    public TransferResponse transfer(
            @Argument Map<String, Object> input) {

        return accountService.transfer(
                UUID.fromString((String) input.get("sourceAccountId")),
                UUID.fromString((String) input.get("destinationAccountId")),
                new BigDecimal((String) input.get("amount"))
        );
    }
}