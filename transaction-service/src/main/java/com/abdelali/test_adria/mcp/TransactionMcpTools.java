package com.abdelali.test_adria.mcp;

import com.abdelali.test_adria.service.TransactionService;
import com.abdelali.test_adria.service.dto.TransactionResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

/**
 * Expose safe read-only tools to the LLM agent.
 * No AI-initiated debit or transfer until authentication and confirmation are implemented.
 */
@Component
@RequiredArgsConstructor
public class TransactionMcpTools {

    private final TransactionService transactionService;

    @McpTool(
            name = "get_transaction_by_id",
            description = "Look up a recorded banking transaction by UUID",
            annotations = @McpTool.McpAnnotations(readOnlyHint = true, destructiveHint = false)
    )
    public TransactionResponse getTransactionById(
            @McpToolParam(description = "Transaction UUID") String id) {
        return transactionService.getById(UUID.fromString(id));
    }

    @McpTool(
            name = "get_transactions_for_account",
            description = "List an account's recorded transactions in descending time order",
            annotations = @McpTool.McpAnnotations(readOnlyHint = true, destructiveHint = false)
    )
    public List<TransactionResponse> getTransactionsForAccount(
            @McpToolParam(description = "Bank account UUID") String accountId) {
        return transactionService.history(UUID.fromString(accountId));
    }
}
