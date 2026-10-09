# MCP Implementation Plan — diagram-maker + runs-ai-analyzer

## Goal

`diagram-maker` becomes an MCP server that generates a **summary dashboard image** from a run analysis produced by `runs-ai-analyzer`.

The MCP client reaches `diagram-maker` through a **LiteLLM proxy** with a remote LLM primary and **Ollama `llama3.2:3b`** fallback.

## Target architecture

```text
┌────────────────────────────────────────────────────────────────────┐
│                       VM 192.168.4.101                             │
│                                                                    │
│   ┌──────────────┐      REST         ┌──────────────────────┐       │
│   │   runs-app   │  (button click)   │  runs-ai-analyzer    │       │
│   │    :8080     │ ─────────────────▶│       :8081          │       │
│   └──────────────┘                   └──────────┬───────────┘       │
│                                                │                    │
│                                                │ REST               │
│                                     ┌──────────┴────────────┐       │
│                                     │   diagram-maker       │       │
│                                     │   Docker container    │       │
│                                     │        :8091          │◀──────┤ MCP client
│                                     └──────────┬────────────┘       │   (via
│                                                │                    │   LiteLLM)
│                                     ┌──────────┴────────────┐       │
│                                     │   LiteLLM proxy       │       │
│                                     │   (host or container) │       │
│                                     │        :4000          │       │
│                                     └──────────┬────────────┘       │
│                                                │                    │
│                                     ┌──────────┴────────────┐       │
│                                     │   Ollama              │       │
│                                     │   llama3.2:3b         │       │
│                                     │   :11434              │       │
│                                     └─────────────────────────┘       │
└────────────────────────────────────────────────────────────────────┘
```

## Key constraints

- `runs-app` is the **primary trigger**. Analysis starts from a button click in `runs-app`.
- `runs-ai-analyzer` is reachable at `http://192.168.4.101:8081`.
- `diagram-maker` will be deployed as a **Docker image** on the VM.
- Development uses **IntelliJ**, but **Maven commands are the canonical success criteria** because they run in the headless VM and in CI/CD.
- Each phase has a **success gate**. We do not proceed until the gate passes.

## Maven vs. IntelliJ

| Maven command | IntelliJ equivalent |
|---|---|
| `./mvnw clean compile -DskipTests` | Maven tool window → `diagram-maker` → Lifecycle → `clean` then `compile` |
| `./mvnw test -Dtest=SomeTest` | Right-click `SomeTest.java` → **Run 'SomeTest'** |
| `./mvnw spring-boot:run -Dspring-boot.run.profiles=local` | Run → Edit Configurations → add Spring Boot → `me.sathish.diagram_maker.DiagramMakerApplication`, VM options `-Dspring.profiles.active=local` |
| `./mvnw clean package -DskipTests` | Maven tool window → Lifecycle → `clean` then `package` |
| `docker build ...` / `docker run ...` | Terminal tab inside IntelliJ |

## Phase 0 — Environment and network verification

| Check | Command / criteria |
|---|---|
| Java 21 + Maven | `java -version && ./mvnw -version` |
| `runs-app` reachable | `curl http://192.168.4.101:8080/actuator/health` returns `UP` |
| `runs-ai-analyzer` reachable | `curl http://192.168.4.101:8081/actuator/health` returns `UP` |
| Sample analysis exists | `curl http://192.168.4.101:8081/api/v1/rag/recent?limit=1` returns JSON |
| Docker available | `docker version` succeeds |
| Ollama with 3B model | `ollama list` shows `llama3.2:3b` or smaller |

**Gate:** All checks pass before coding starts.

## Phase 1 — Add dependencies to `diagram-maker`

Changes in `pom.xml`:

- `spring-ai-bom:2.0.1` in `dependencyManagement`.
- `spring-ai-starter-mcp-server-webmvc` in `dependencies`.
- `org.knowm.xchart:xchart` in `dependencies`.

**Success (Maven):**
```bash
./mvnw clean compile -DskipTests
```
Build succeeds with no dependency conflicts.

**Success (IntelliJ):** `clean` then `compile` lifecycle goals finish without errors.

## Phase 2 — Configure MCP Streamable HTTP transport

Changes in `src/main/resources/application.yml`:

```yaml
spring:
  ai:
    mcp:
      server:
        name: diagram-maker
        version: 0.0.1-SNAPSHOT
        protocol: STREAMABLE
        streamable-http:
          mcp-endpoint: /mcp

runs-ai-analyzer:
  base-url: http://192.168.4.101:8081

diagram:
  output-dir: ${user.dir}/diagrams
```

**Success (Maven):**
```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
# in another shell:
curl http://localhost:8091/actuator/health
curl http://localhost:8091/mcp
```
Both return HTTP 200.

**Success (IntelliJ):** Spring Boot run configuration starts and actuator health is green.

## Phase 3 — Build `RunsAiAnalyzerClient`

Changes:

- New package `me.sathish.diagram_maker.runsai`.
- DTOs mirroring the fields needed from `RunAnalysisResponse`.
- `RunsAiAnalyzerClient` with methods:
  - `getAnalysisByDocumentId(UUID)`
  - `getAnalysesByActivityId(String)`

**Success (Maven):**
```bash
./mvnw test -Dtest=RunsAiAnalyzerClientTest
```
Unit test using `MockRestServiceServer` passes.

**Success (IntelliJ):** `RunsAiAnalyzerClientTest` run from the IDE passes.

## Phase 4 — Build `DashboardImageService` with XChart

Changes:

- New `DashboardImageService` that accepts a `RunAnalysisResponse` and produces a PNG/SVG dashboard.
- Dashboard includes:
  - summary text panel
  - metrics bar chart
  - insights list
  - risk flags
  - confidence score gauge

**Success (Maven):**
```bash
./mvnw test -Dtest=DashboardImageServiceTest
ls target/test-diagrams/
```
A non-empty PNG/SVG file is created and assertions pass.

**Success (IntelliJ):** `DashboardImageServiceTest` passes and test output directory contains an image.

## Phase 5 — Create `DiagramMakerMcpTools`

Changes:

- New `me.sathish.diagram_maker.mcp.DiagramMakerMcpTools` Spring service.
- `@Tool` method:
  ```java
  RunDiagramResult generateRunDiagram(
      String activityId,
      DiagramType type,
      ImageFormat format,
      String outputPath)
  ```

**Success (Maven):**
```bash
./mvnw test -Dtest=DiagramMakerMcpToolsTest
```
Tool bean is discovered and returns a result with `fileName`, `uid`, and `filePath`.

**Success (IntelliJ):** `DiagramMakerMcpToolsTest` passes.

## Phase 6 — Storage path validation

Changes:

- New `DiagramStorageService` validates and sanitizes `outputPath`.
- Rejects paths outside the configured root directory.
- Sanitizes file names.

**Success tests:**
- Valid relative path accepted.
- `../etc/passwd` rejected.
- Absolute path outside root rejected.
- File name with special characters sanitized.

## Phase 7 — Local end-to-end test

Steps:

1. Start `runs-ai-analyzer`.
2. Start `diagram-maker` locally.
3. Call the MCP tool with a real `activityId`.
4. Verify a dashboard image is created.

**Success (Maven):**
```bash
./mvnw spring-boot:run
# in another shell, use MCP inspector or a Python test client
```
A dashboard image is created from real analysis data and the tool returns its path.

## Phase 8 — Dockerize `diagram-maker`

Changes:

- Update `Dockerfile` to use a Java 21 base image.
- Add health check.
- Expose port `8091`.

Build and run:

```bash
./mvnw clean package -DskipTests
docker build -t me.sathish/diagram-maker .
docker run -d \
  --name diagram-maker \
  -p 8091:8091 \
  -e RUNS_AI_ANALYZER_BASE_URL=http://192.168.4.101:8081 \
  me.sathish/diagram-maker
```

**Success:**
```bash
curl http://192.168.4.101:8091/actuator/health
docker logs diagram-maker
```
Both show the service is healthy.

## Phase 9 — Container-to-VM networking test

From inside the `diagram-maker` container:

```bash
docker exec diagram-maker curl http://192.168.4.101:8081/actuator/health
```

If this fails, use Docker host networking or a custom bridge:

```bash
docker run -d --network host --name diagram-maker me.sathish/diagram-maker
```

**Success:** Container reaches `runs-ai-analyzer` and returns `UP`.

## Phase 10 — LiteLLM proxy + Ollama fallback

Create `litellm_config.yaml`:

```yaml
model_list:
  - model_name: default
    litellm_params:
      model: openai/gpt-4o
      api_key: os.environ/OPENAI_API_KEY
  - model_name: fallback
    litellm_params:
      model: ollama/llama3.2:3b
      api_base: http://192.168.4.101:11434

router_settings:
  fallbacks: [{default: [fallback]}]
```

Run:

```bash
litellm --config litellm_config.yaml --port 4000
```

**Success:**
```bash
curl http://192.168.4.101:4000/v1/models
```
Returns the configured models.

## Phase 11 — Full integration

1. Import a run in `runs-app`.
2. Click the analysis button in `runs-app`; `runs-app` calls `runs-ai-analyzer`.
3. MCP client asks LiteLLM: *"Generate a dashboard for activity 12345."*
4. LiteLLM routes to `diagram-maker` at `http://192.168.4.101:8091/mcp`.
5. `diagram-maker` fetches the analysis from `runs-ai-analyzer`.
6. `diagram-maker` generates and stores a dashboard image.
7. File path returned to the user.

**Success:**
- Dashboard image exists on disk.
- File path is returned through the MCP client.
- Fallback to Ollama works when the primary remote LLM is unavailable.

## Deviations

If any phase fails, we stop and fix it before moving to the next. This plan is the source of truth. Any shortcut must still pass the success gate of its phase.

## Implementation notes / deviations

### Spring Boot 4.1 compatibility

The project uses **Spring Boot 4.1.1** / Spring Framework 7.0. Several APIs differ from earlier Spring Boot versions:

- Jackson classes moved to the `tools.jackson.*` package (`tools.jackson.databind.ObjectMapper`, etc.).
- `UriComponentsBuilder.fromHttpUrl(String)` is no longer available; use `UriComponentsBuilder.fromUriString(String)`.
- `RestTemplateBuilder` is not on the classpath; `RestTemplate` is configured manually with `SimpleClientHttpRequestFactory`.

### MCP tool annotations

Spring AI MCP server 2.0.1 uses the MCP-specific annotations, not the generic Spring AI `@Tool` annotation:

- `@org.springframework.ai.mcp.annotation.McpTool`
- `@org.springframework.ai.mcp.annotation.McpToolParam`

### Test execution

The `maven-surefire-plugin` was configured with `<skipTests>${skipITs}</skipTests>`, which skipped all unit tests by default. It was changed to `<skipTests>false</skipTests>` so `./mvnw test` runs unit tests by default.

### Local end-to-end verification command

With the application running, the MCP Streamable HTTP transport requires both `application/json` and `text/event-stream` in the `Accept` header, and a session ID returned from `initialize`.

```bash
# Start the server
./mvnw spring-boot:run

# Initialize and capture the session ID
RESP=$(curl -s -D /tmp/init_headers \
  -X POST http://localhost:8091/mcp \
  -H "Content-Type: application/json" \
  -H "Accept: application/json, text/event-stream" \
  -d '{"jsonrpc":"2.0","id":1,"method":"initialize","params":{"protocolVersion":"2024-11-05","capabilities":{},"clientInfo":{"name":"test","version":"1.0"}}}')
SESSION=$(grep -i "mcp-session-id" /tmp/init_headers | awk '{print $2}' | tr -d '\r')

# List tools
curl -s -X POST http://localhost:8091/mcp \
  -H "Content-Type: application/json" \
  -H "Accept: application/json, text/event-stream" \
  -H "Mcp-Session-Id: $SESSION" \
  -d '{"jsonrpc":"2.0","id":2,"method":"tools/list","params":{}}'
```

This confirms the `generate_run_diagram` tool is exposed. A full tool invocation that produces a real dashboard image requires `runs-ai-analyzer` to be running with sample data and is validated on the VM.

### Phase 10/11 as built (2026-10-01)

- LiteLLM runs directly on the VM (`runs-server`, 192.168.4.101) from the pip venv `~/litellm-venv` (1.102.1, checked to not be the compromised 1.82.7/1.82.8 and to contain no `litellm_init.pth`). It runs as the systemd **user** service `litellm.service` with linger enabled. Config is in `~/.config/litellm/config.yaml` (copied to `litellm_config.yaml` in this repo), and secrets are in `~/.config/litellm/litellm.env` (`chmod 600`). The original `~/litellm_config.yaml` is root-owned and no longer used.
- A master key is now required (`general_settings.master_key`), and the unit refuses to start without it. Before this, the proxy would have been open to the LAN.
- The fallback model is `ollama_chat/qwen2.5:3b`. `llama3.2:3b` was never pulled on the VM's Ollama. The `ollama_chat/` prefix uses `/api/chat`, which supports native tool calling.
- LiteLLM does **not** route to diagram-maker as a model. diagram-maker is registered under `mcp_servers` in `litellm_config.yaml`, and LiteLLM acts as an MCP gateway at `:4000/mcp/`. The architecture diagram above draws LiteLLM between diagram-maker and Ollama, but the actual flow is client → LiteLLM → (model picks tool) → diagram-maker `/mcp`.
- The runs-app button does not go through LiteLLM. runs-ai-analyzer already knows the `documentId`, so it calls the MCP tool directly with `McpSyncClient`. LiteLLM is only used for the conversational path, where a model chooses the tool.
- Phase 10/11 gates passed on the VM: `/v1/models` lists `default` and `fallback`, and requests without a valid key are rejected. MCP `tools/list` through `:4000/mcp/` returns both diagram-maker tools. `default` fell back to `qwen2.5:3b` (`x-litellm-attempted-fallbacks: 1`). Asked in plain English, the model called `diagram_maker-generate_analysis_diagram` for a real analysis document, and the VM's diagram-maker container saved a 73,850-byte PNG.

## Open decisions

- Should `runs-app` also have a direct **"Generate diagram"** button that calls `diagram-maker` via REST, or is diagram generation **only** exposed through the MCP tool for the AI client? If both, a thin REST controller will reuse the same `DiagramMakerMcpTools` / `DashboardImageService`.
