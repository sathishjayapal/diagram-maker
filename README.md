# Diagram Maker

`diagram-maker` is a Java 21 / Spring Boot 4.1 MCP server that generates summary dashboard images from run analysis data produced by `runs-ai-analyzer`.

The application exposes an MCP tool over Streamable HTTP (`/mcp`) that AI clients can call to fetch an analysis and render a PNG/SVG dashboard using [XChart](https://knowm.org/open-source/xchart/). It was originally scaffolded with [Bootify.io](https://bootify.io/app/QT88C56CKKIM).

## What it does

- Accepts MCP requests to generate run-analysis dashboards.
- Calls `runs-ai-analyzer` to retrieve run analysis documents.
- Renders dashboards containing summary text, metric charts, insights, risk flags, and a confidence gauge.
- Stores generated images in a configurable output directory and returns the file path.
- Serves a small Thymeleaf UI and REST endpoints for file uploads and posture checks.

## Tech stack

- **Backend:** Spring Boot 4.1, Java 21, Maven, Lombok, MapStruct
- **Frontend:** Thymeleaf, Webpack, Tailwind CSS, Babel
- **AI/MCP:** Spring AI MCP Server (Streamable HTTP transport)
- **Charts:** XChart
- **Testing:** JUnit 5, Mockito, RestAssured, Playwright
- **Ops:** Docker, Docker Compose, Spring Boot Actuator

## Project structure

- `src/main/java/me/sathish/diagram_maker/`
  - `config/` - Application configuration
  - `controller/` - Server-rendered Thymeleaf controllers
  - `diagram/` - MCP tools, dashboard image generation, storage, and properties
  - `model/` - Domain models (file data, run analysis data)
  - `rest/` - REST controllers (file upload, runs posture)
  - `runsai/` - Client for `runs-ai-analyzer`
  - `service/` - Business services
  - `util/` - Common utilities and error handling
- `src/main/resources/templates/` - Thymeleaf templates
- `src/main/resources/application.yml` - Application configuration
- `diagrams/` - Default output directory for generated images
- `docker-compose.yml` - Docker Compose deployment
- `litellm_config.yaml` - Copy of the VM's LiteLLM config (model fallback + MCP gateway)

## Development

Use the checked-in Maven wrapper for all builds:

```
./mvnw clean compile
./mvnw test
./mvnw package
```

Run locally:

```
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

The server starts on port `8091` by default.

### Frontend development

Node.js 24 is required. Install dependencies and start the Webpack dev server:

```
npm install
npm run devserver
```

The dev server proxies requests to the Spring Boot backend, and the app is available at `http://localhost:8081`.

### Required services

- `runs-ai-analyzer` is expected at `http://192.168.4.101:8081` (configurable via `runs-ai-analyzer.base-url`).
- For AI client access, a LiteLLM proxy runs on the VM at `http://192.168.4.101:4000` (see [LiteLLM gateway](#litellm-gateway)). Its fallback model is `qwen2.5:3b` on the VM's Ollama.

### Local configuration

Create `src/main/resources/application-local.yml` to override settings for development. Example:

```yaml
runs-ai-analyzer:
  base-url: http://localhost:8081

diagram:
  output-dir: ${user.dir}/diagrams
```

## Build and run

Build the application:

```
./mvnw clean package
```

Start the production jar:

```
java -Dspring.profiles.active=production -jar ./target/diagram-maker-0.0.1-SNAPSHOT.jar
```

Build and run with Docker:

```
./mvnw clean package -DskipTests
docker build -t me.sathish/diagram-maker .
docker run -d \
  --name diagram-maker \
  -p 8091:8091 \
  -e RUNS_AI_ANALYZER_BASE_URL=http://192.168.4.101:8081 \
  me.sathish/diagram-maker
```

Or use Docker Compose:

```
docker compose up -d
```

## Verifying the MCP endpoint

With the application running:

```bash
RESP=$(curl -s -D /tmp/init_headers \
  -X POST http://localhost:8091/mcp \
  -H "Content-Type: application/json" \
  -H "Accept: application/json, text/event-stream" \
  -d '{"jsonrpc":"2.0","id":1,"method":"initialize","params":{"protocolVersion":"2024-11-05","capabilities":{},"clientInfo":{"name":"test","version":"1.0"}}}')
SESSION=$(grep -i "mcp-session-id" /tmp/init_headers | awk '{print $2}' | tr -d '\r')

curl -s -X POST http://localhost:8091/mcp \
  -H "Content-Type: application/json" \
  -H "Accept: application/json, text/event-stream" \
  -H "Mcp-Session-Id: $SESSION" \
  -d '{"jsonrpc":"2.0","id":2,"method":"tools/list","params":{}}'
```

This lists the available MCP tools, including `generate_run_diagram`.

## LiteLLM gateway

A LiteLLM proxy runs on the VM next to the diagram-maker container (`runs-server`, `192.168.4.101:4000`). It is **not** part of `docker-compose.yml`. `litellm_config.yaml` in this repo is a copy of the VM's config. It does two jobs:

- **Model gateway** – an OpenAI-compatible `/v1/chat/completions` endpoint. The `default` model is `openai/gpt-4o`, and the router falls back to `ollama_chat/qwen2.5:3b` (the VM's Ollama) when the primary fails, including when `OPENAI_API_KEY` is blank.
- **MCP gateway** – re-exposes diagram-maker's tools at `http://192.168.4.101:4000/mcp/` as `diagram_maker-generate_run_diagram` and `diagram_maker-generate_analysis_diagram`, so any MCP or OpenAI-style client can use them with one key.

How it runs on the VM:

| What | Where |
|---|---|
| LiteLLM install | pip venv `~/litellm-venv` (1.102.1; not one of the compromised 1.82.7/1.82.8) |
| Config | `~/.config/litellm/config.yaml` (same as `litellm_config.yaml` here) |
| Secrets | `~/.config/litellm/litellm.env` (`chmod 600`): `LITELLM_MASTER_KEY`, `OPENAI_API_KEY` |
| Service | systemd user unit `~/.config/systemd/user/litellm.service`, linger enabled so it survives logout/reboot. It refuses to start without `LITELLM_MASTER_KEY`. |

```bash
systemctl --user status litellm            # on the VM
systemctl --user restart litellm           # after editing config.yaml or litellm.env
journalctl --user -u litellm -f            # logs
```

After changing `litellm_config.yaml` here, copy it to the VM and restart:

```bash
scp litellm_config.yaml vm:.config/litellm/config.yaml && ssh vm 'systemctl --user restart litellm'
```

Requests without a valid key are rejected. LiteLLM 1.102.1 without a database answers them with `500`/`400` instead of `401`, because its auth error handler needs the `prisma` package.

Verify on the VM (load the key with `set -a; . ~/.config/litellm/litellm.env; set +a`):

```bash
# Models
curl -s http://localhost:4000/v1/models -H "Authorization: Bearer $LITELLM_MASTER_KEY"

# diagram-maker tools through the gateway
curl -s http://localhost:4000/mcp/ \
  -H "Content-Type: application/json" \
  -H "Accept: application/json, text/event-stream" \
  -H "x-litellm-api-key: Bearer $LITELLM_MASTER_KEY" \
  -d '{"jsonrpc":"2.0","id":1,"method":"tools/list"}'

# Let the model pick and call the tool
curl -s http://localhost:4000/v1/chat/completions \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $LITELLM_MASTER_KEY" \
  -d '{"model":"default",
       "messages":[{"role":"user","content":"Generate a SUMMARY PNG dashboard for analysis document <documentId>"}],
       "tools":[{"type":"mcp","server_url":"litellm_proxy/mcp/diagram_maker","server_label":"diagram_maker","require_approval":"never"}]}'
```

The `x-litellm-model-group` and `x-litellm-attempted-fallbacks` response headers show which model actually answered.

## Further readings

* [Maven docs](https://maven.apache.org/guides/index.html)
* [Spring Boot reference](https://docs.spring.io/spring-boot/docs/current/reference/htmlsingle/)
* [Spring AI MCP reference](https://docs.spring.io/spring-ai/reference/mcp.html)
* [Thymeleaf docs](https://www.thymeleaf.org/documentation.html)
* [Webpack concepts](https://webpack.js.org/concepts/)
* [npm docs](https://docs.npmjs.com/)
* [Tailwind CSS](https://tailwindcss.com/)
* [XChart](https://knowm.org/open-source/xchart/)
* [Learn Spring Boot with Thymeleaf](https://www.wimdeblauwe.com/books/taming-thymeleaf/)
