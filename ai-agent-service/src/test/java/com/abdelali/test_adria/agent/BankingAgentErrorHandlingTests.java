package com.abdelali.test_adria.agent;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BankingAgentErrorHandlingTests {

    @Test
    void detectsGroqRejectedNonexistentTool() {
        RuntimeException groqResponse = new RuntimeException(
                "400: tool call validation failed: attempted to call tool 'list_addresses' "
                        + "which was not in request.tools");
        assertThat(BankingAgentService.unknownToolError(groqResponse)).isTrue();
    }

    @Test
    void detectsNestedGroqToolErrors() {
        RuntimeException error = new RuntimeException("remote request failed",
                new IllegalArgumentException("tool call validation failed"));
        assertThat(BankingAgentService.unknownToolError(error)).isTrue();
    }

    @Test
    void doesNotRetryUnrelatedProviderErrors() {
        assertThat(BankingAgentService.unknownToolError(
                new IllegalStateException("Rate limit exceeded"))).isFalse();
    }
}
