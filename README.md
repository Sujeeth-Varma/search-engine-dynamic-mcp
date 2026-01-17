# Search Engine MCP – Dynamic Config-Based Tooling

This project demonstrates how to expose HTTP search providers (e.g., Google, Yahoo, custom APIs) as Model Context Protocol (MCP) tools using Spring AI. The key idea is a dynamic, configuration-driven approach: tools are declared in configuration (YAML/env) without writing new Java code for each tool. At startup, these configs are translated into MCP tool definitions and registered with the Spring AI MCP server.

## Why a dynamic config approach?

- Add/modify tools without code changes or redeploys (just change config and restart)
- Centralize governance for tool names, descriptions, HTTP methods, and URLs
- Consistent JSON Schema for inputs, allowing LLMs to call tools reliably
- Easy to mock/swap providers between environments

## High-level architecture

- Configuration source: `application.yml` (or environment variables) defines a list of tools under `search.tools`.
- Properties binding: `SearchToolProperties` maps YAML into a strongly-typed model.
- Tool construction: `DynamicSearchToolProvider` converts each tool config into an MCP `Tool` with an input JSON Schema.
- Tool registration: `McpToolsConfiguration` registers each tool as a Spring AI `FunctionToolCallback` exposed by the MCP server.
- HTTP execution: `RestToolAdapter` executes the underlying HTTP call using Spring WebClient.
- Debug endpoints (optional): `DebugController` exposes simple REST endpoints to list and invoke tools manually during development.

## Repository layout (key files)

- `src/main/resources/application.yml` – Declare tools and MCP server settings
- `src/main/java/.../config/SearchToolProperties.java` – Binds `search.tools` config
- `src/main/java/.../adapter/DynamicSearchToolProvider.java` – Builds MCP tools and executes them via adapter
- `src/main/java/.../config/McpToolsConfiguration.java` – Registers tools with Spring AI MCP server
- `src/main/java/.../adapter/RestToolAdapter.java` – Makes HTTP calls with WebClient
- `src/main/java/.../controller/DebugController.java` – Optional debug APIs (guarded by `debug.enabled`)

## Configuration model

In `application.yml` you can define multiple tools:

```yaml
search:
  tools:
    - name: call_google
      description: Search using Google
      method: GET
      url: https://www.google.com/search?q={search_query}

    - name: call_yahoo
      description: Search using Yahoo
      method: GET
      url: https://search.yahoo.com/search?p={search_query}

debug:
  enabled: true

spring:
  ai:
    mcp:
      server:
        name: mcp-search-gateway
        version: 1.0.0
        annotation-scanner:
          enabled: false
```

Fields per tool:

- `name` – Unique identifier used by the MCP server and LLM when invoking the tool
- `description` – Human-readable description for the LLM/tooling UIs
- `method` – HTTP verb (e.g., GET, POST)
- `url` – Target URL template for the provider

Input contract (JSON):

- All tools expect an object with a single property: `query` (string). The provider uses this value to build the request.

Note: The `url` template in the examples uses `{search_query}` as a placeholder. The default `RestToolAdapter` currently passes only the raw query string as a single URI variable. If you prefer to use named placeholders (e.g., `{search_query}`), update the adapter to map `query` to that name, or change your URLs to use a single positional placeholder, such as `...?q={}`. See “Customizing request mapping” below.

## How it works under the hood

1. `SearchToolProperties` binds the `search.tools` list into Java objects at startup.
2. `DynamicSearchToolProvider.getTools()` transforms each config into an MCP `Tool` with this input schema:
   - type: object
   - properties: { query: { type: string, description: "Search query string" } }
   - required: ["query"]
3. `McpToolsConfiguration` serializes the MCP JSON Schema and registers a Spring AI `FunctionToolCallback` for each tool. The callback delegates execution to `DynamicSearchToolProvider.executeTool(...)`.
4. `executeTool` locates the tool config and calls `RestToolAdapter.execute(...)`.
5. `RestToolAdapter` uses `WebClient` to invoke the remote endpoint and returns the response body as text. The tool response is wrapped in a minimal MCP-compatible structure with `content: [{ type: "text", text: "..." }]`.

## Running the application

Prerequisites:
- Java 17+
- Maven 3.9+

Build and run:

```bash
mvn spring-boot:run
```

The MCP server will start within the Spring Boot app. You can integrate it with MCP-compatible clients that discover tools automatically.

## Trying it locally (debug endpoints)

With `debug.enabled: true`, the following endpoints are available:

- List tools: `GET http://localhost:8080/debug/tools`
- Tool details: `GET http://localhost:8080/debug/tools/{toolName}`
- Health: `GET http://localhost:8080/debug/health`
- Current config: `GET http://localhost:8080/debug/config`
- Execute a tool:

```bash
curl -X POST http://localhost:8080/debug/execute/call_google \
  -H 'Content-Type: application/json' \
  -d '{"query": "spring ai mcp"}'
```

You should see a JSON response with `success: true` and the raw text returned by the provider wrapped in `result.content[0].text`.

## Use cases

- Gateway for multiple search providers (public or internal)
- Rapidly enable/disable tools per environment by changing config
- Route LLM tool calls to HTTP APIs with uniform input/contact
- Prototype new tools without writing Java code

## Customizing request mapping

The default adapter implementation:

```java
webClient.method(method)
        .uri(url, input.get("query"))
        .retrieve()
        .bodyToMono(String.class)
        .block();
```

This means the adapter treats the URL template as having a single positional placeholder. If your URL uses a named placeholder, e.g. `...?q={search_query}`, you have two options:

1) Change your URL to use a single positional placeholder, e.g. `...?q={}`; or
2) Update the adapter to supply a named variable map, for example:

```java
uriBuilder -> uriBuilder
    .path(path)
    .build(Map.of("search_query", input.get("query")))
```

Feel free to extend `RestToolAdapter` for headers, auth, query/body parameters, or POST/PUT payloads.

## Extending the JSON Schema

Currently the input schema only includes a single `query` field. To support more complex tools:

- Update `DynamicSearchToolProvider#createTool` to add more properties and `required` fields.
- Adjust `RestToolAdapter#execute` to map inputs into query parameters, headers, or request bodies as needed.
- Optionally extend `SearchToolProperties.Tool` with additional config (e.g., headers, auth type, body templates) and map them accordingly.

## Environment overrides

All `application.yml` settings can be overridden via Spring profiles or environment variables. For example, to disable debug endpoints:

```bash
DEBUG_ENABLED=false mvn spring-boot:run
```

Or use a profile-specific YAML (e.g., `application-prod.yml`) to point tools at production endpoints.
