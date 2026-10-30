package com.abdelali.test_adria;

import com.abdelali.test_adria.agent.AgentUnavailableException;
import com.abdelali.test_adria.agent.BankingAgentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(properties = {
        "server.port=0",
        "spring.ai.mcp.client.enabled=false",
        "spring.ai.openai.api-key=NOT_CONFIGURED"
})
class AiAgentServiceApplicationTests {

    @Autowired
    private BankingAgentService agent;

    @Test
    void contextLoads() {
        assertThat(agent).isNotNull();
    }

    @Test
    void noMcpConnectionsMeansNoAdvertisedBankingTools() {
        assertThat(agent.availableTools()).isEmpty();
    }

    @Test
    void chatFailsClearlyWithoutApiKeyRatherThanFabricatingBankData() {
        assertThatThrownBy(() -> agent.chat("What is my account balance?"))
                .isInstanceOf(AgentUnavailableException.class)
                .hasMessageContaining("OPENAI_API_KEY");
    }

    @Test
    void rejectsBlankAndOversizedMessages() {
        assertThatThrownBy(() -> agent.chat("  "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("blank");

        assertThatThrownBy(() -> agent.chat("a".repeat(2001)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("2000");
    }
}
