package com.abdelali.test_adria.web;

import com.abdelali.test_adria.agent.BankingAgentService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/agent")
public class BankingAgentController {

    private final BankingAgentService agent;

    public BankingAgentController(BankingAgentService agent) {
        this.agent = agent;
    }

    @GetMapping("/tools")
    public Map<String, Object> tools() {
        List<String> tools = agent.availableTools();
        return Map.of("agent", "adria-banking-agent", "toolCount", tools.size(), "tools", tools);
    }

    public record ChatRequest(String message) { }
    public record ChatResponse(String reply) { }

    @PostMapping("/chat")
    public ChatResponse chat(@RequestBody ChatRequest request) {
        return new ChatResponse(agent.chat(request == null ? null : request.message()));
    }
}
