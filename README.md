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
- `litellm_config.yaml` - LiteLLM proxy configuration with Ollama fallback

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
- For AI client access via LiteLLM, run the proxy with `litellm --config litellm_config.yaml --port 4000` and ensure Ollama has `llama3.2:3b` available.

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
