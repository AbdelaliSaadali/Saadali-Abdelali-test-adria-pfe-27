# Adria Banking AI (Groq + Spring AI + MCP)

This service serves the local demo chat UI at **http://localhost:8084/chat.html**.
It uses Spring AI's OpenAI-compatible client with Groq's
`https://api.groq.com/openai/v1` API and the `qwen/qwen3.8-27b` model.

## Run locally

Start Account-Service (8081) and Transaction-Service (8082) before the agent.
Both must expose their read-only `/mcp` endpoints.

Provide the Groq key through the `GROQ_API_KEY` **environment variable**.
Do not add secrets to application properties, Git or screenshots.

From the project root:

```bash
# On the development Mac, the key was stored outside the Git repository.
set -a
source ~/.config/adria/groq.env
set +a
./mvnw -pl ai-agent-service spring-boot:run
```

Open `http://localhost:8084/chat.html`. The UI lists connected MCP tools
and sends questions to `POST /agent/chat`.

## Verify

```bash
./mvnw -pl ai-agent-service -am clean verify
curl http://localhost:8084/agent/tools
curl -H 'Content-Type: application/json' \
  -d '{"message":"Use list_accounts to count the demo accounts"}' \
  http://localhost:8084/agent/chat
```

## Security and delivery limitations

- Demo is accessible on **localhost only**, with **no user login**.
- Only four **read-only MCP tools** are approved: list accounts, get account,
  get transaction, and get transaction history.
- Do **not** use real banking/customer data in this development configuration;
  MCP tool results are sent to the external Groq model.
- Production requires authentication, tenant-scoped authorization, audit,
  secret management, rate limiting and monitoring.
- No chat history is persisted; each request is independent.
