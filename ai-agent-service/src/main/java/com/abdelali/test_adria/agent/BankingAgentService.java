package com.abdelali.test_adria.agent;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

@Service
public class BankingAgentService {

    private static final Set<String> ALLOWED_TOOLS = Set.of(
            "list_accounts",
            "get_account_by_id",
            "get_transaction_by_id",
            "get_transactions_for_account"
    );

    private static final String BANKING_SYSTEM_PROMPT = """
            You are the Adria banking assistant for a local development demo.
            Only provide banking facts that the read-only MCP tools actually return.
            Never invent balances, accounts, payments or transactions.
            Never create accounts, debit, credit, transfer funds or call a mutating tool.
            If a result is missing or the service is unavailable, state that explicitly.
            Keep answers clear and concise. The system has no user authentication yet.
            """;

    private final ChatClient chatClient;
    private final ObjectProvider<ToolCallbackProvider> providers;
    private final String configuredKey;

    public BankingAgentService(
            ChatClient.Builder builder,
            ObjectProvider<ToolCallbackProvider> providers,
            @Value("${spring.ai.openai.api-key:NOT_CONFIGURED}") String configuredKey) {
        this.chatClient = builder.build();
        this.providers = providers;
        this.configuredKey = configuredKey;
    }

    public List<String> availableTools() {
        return Arrays.stream(approvedCallbacks())
                .map(callback -> callback.getToolDefinition().name())
                .sorted()
                .toList();
    }

    public String chat(String message) {
        if (message == null || message.isBlank()) {
            throw new IllegalArgumentException("Message must not be blank");
        }
        if (message.length() > 2000) {
            throw new IllegalArgumentException("Message must be at most 2000 characters");
        }
        if (configuredKey == null || configuredKey.isBlank()
                || configuredKey.equals("NOT_CONFIGURED")) {
            throw new AgentUnavailableException(
                    "AI model not configured. Set OPENAI_API_KEY on your Mac and restart ai-agent-service.");
        }

        ToolCallback[] callbacks = approvedCallbacks();
        if (callbacks.length == 0) {
            throw new AgentUnavailableException(
                    "No approved banking MCP tools connected. Start Account-Service and Transaction-Service.");
        }

        String result = chatClient.prompt()
                .system(BANKING_SYSTEM_PROMPT)
                .user(message)
                .toolCallbacks(callbacks)
                .call()
                .content();

        return result == null ? "" : result;
    }

    private ToolCallback[] approvedCallbacks() {
        return providers.stream()
                .flatMap(provider -> Arrays.stream(provider.getToolCallbacks()))
                .filter(callback -> {
                    String name = callback.getToolDefinition().name();
                    return ALLOWED_TOOLS.contains(name) ||
                            ALLOWED_TOOLS.stream().anyMatch(n -> name.endsWith("_" + n));
                })
                .toArray(ToolCallback[]::new);
    }
}
