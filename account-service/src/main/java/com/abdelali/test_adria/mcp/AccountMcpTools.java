package com.abdelali.test_adria.mcp;

import com.abdelali.test_adria.service.AccountService;
import com.abdelali.test_adria.service.dto.AccountResponse;

import lombok.RequiredArgsConstructor;

import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class AccountMcpTools {

    private final AccountService accountService;

    @McpTool(
            name = "list_accounts",
            description = "List all bank accounts",
            annotations = @McpTool.McpAnnotations(
                    readOnlyHint = true,
                    destructiveHint = false
            )
    )
    public List<AccountResponse> listAccounts() {
        return accountService.findAll();
    }

    @McpTool(
            name = "get_account_by_id",
            description = "Get a bank account by its UUID",
            annotations = @McpTool.McpAnnotations(
                    readOnlyHint = true,
                    destructiveHint = false
            )
    )
    public AccountResponse getAccountById(
            @McpToolParam(description = "Bank account UUID")
            String accountId) {

        return accountService.findById(
                UUID.fromString(accountId)
        );
    }
}