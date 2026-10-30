package com.abdelali.test_adria;

import com.abdelali.test_adria.agent.AgentUnavailableException;
import com.abdelali.test_adria.agent.BankingAgentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

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
                .hasMessageContaining("GROQ_API_KEY");
    }

    @Test
    void browserChatPageIsPackagedWithTheApplication() throws IOException {
        ClassPathResource page = new ClassPathResource("static/chat.html");
        assertThat(page.exists()).isTrue();
        String html = page.getContentAsString(StandardCharsets.UTF_8);
        assertThat(html).contains("Adria Banking AI", "/agent/chat", "/agent/tools");
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
