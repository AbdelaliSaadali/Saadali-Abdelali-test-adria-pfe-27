package com.abdelali.test_adria.agent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger log = LoggerFactory.getLogger(BankingAgentService.class);

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
            Tool calls MUST use only the exact tool names supplied in this request.
            For an account count use list_accounts and count the returned entries.
            Never invent names such as list_addresses or list_customers.
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
                    "Groq not configured. Set GROQ_API_KEY on your Mac and restart ai-agent-service.");
        }

        ToolCallback[] callbacks = approvedCallbacks();
        if (callbacks.length == 0) {
            throw new AgentUnavailableException(
                    "No approved banking MCP tools connected. Start Account-Service and Transaction-Service.");
        }

        String toolNames = Arrays.stream(callbacks)
                .map(callback -> callback.getToolDefinition().name())
                .sorted()
                .reduce((a, b) -> a + ", " + b)
                .orElse("");

        try {
            return askModel(message, callbacks, toolNames, false);
        } catch (RuntimeException firstError) {
            if (unknownToolError(firstError)) {
                log.warn("Groq rejected an invented tool name; retrying once");
                try {
                    return askModel(message, callbacks, toolNames, true);
                } catch (RuntimeException retryError) {
                    log.warn("Groq retry failed: {}", retryError.getClass().getSimpleName());
                    throw new AgentUnavailableException(
                            "The AI model could not complete that tool request. Please try again.");
                }
            }
            log.warn("AI provider request failed: {}", firstError.getClass().getSimpleName());
            throw new AgentUnavailableException(
                    "The AI provider could not complete the request. Please try again.");
        }
    }

    private String askModel(String message, ToolCallback[] callbacks,
                            String toolNames, boolean retry) {
        String instruction = BANKING_SYSTEM_PROMPT
                + "\nThe ONLY callable tools are: " + toolNames + "."
                + (retry ? "\nCRITICAL: The provider rejected a nonexistent tool. "
                + "Call only an exact tool name from the list. "
                + "Use list_accounts for any account count." : "");
        String result = chatClient.prompt()
                .system(instruction)
                .user(message)
                .toolCallbacks(callbacks)
                .call()
                .content();
        return result == null ? "" : result;
    }

    static boolean unknownToolError(Throwable error) {
        for (Throwable cause = error; cause != null; cause = cause.getCause()) {
            String message = cause.getMessage();
            if (message != null && (message.contains("tool call validation failed")
                    || message.contains("not in request.tools"))) {
                return true;
            }
        }
        return false;
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
