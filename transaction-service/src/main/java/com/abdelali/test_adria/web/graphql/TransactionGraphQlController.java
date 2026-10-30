package com.abdelali.test_adria.web.graphql;

import com.abdelali.test_adria.service.TransactionService;
import com.abdelali.test_adria.service.dto.CreateTransactionRequest;
import com.abdelali.test_adria.service.dto.CreateTransferRequest;
import com.abdelali.test_adria.service.dto.TransactionResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Controller
@RequiredArgsConstructor
public class TransactionGraphQlController {

    private final TransactionService transactionService;

    public record TransactionInput(
            String accountId,
            String amount,
            String currency,
            String merchantCategory,
            String country) { }

    public record TransferInput(
            String sourceAccountId,
            String destinationAccountId,
            String amount,
            String currency,
            String country) { }

    @QueryMapping
    public TransactionResponse transaction(@Argument String id) {
        return transactionService.getById(UUID.fromString(id));
    }

    @QueryMapping
    public List<TransactionResponse> transactionsByAccount(@Argument String accountId) {
        return transactionService.history(UUID.fromString(accountId));
    }

    @MutationMapping
    public TransactionResponse createTransaction(@Argument("input") TransactionInput input) {
        return transactionService.create(new CreateTransactionRequest(
                UUID.fromString(input.accountId()),
                new BigDecimal(input.amount()),
                input.currency(),
                input.merchantCategory(),
                input.country()
        ));
    }

    @MutationMapping
    public TransactionResponse transferFunds(@Argument("input") TransferInput input) {
        return transactionService.transfer(new CreateTransferRequest(
                UUID.fromString(input.sourceAccountId()),
                UUID.fromString(input.destinationAccountId()),
                new BigDecimal(input.amount()),
                input.currency(),
                input.country()
        ));
    }
}
