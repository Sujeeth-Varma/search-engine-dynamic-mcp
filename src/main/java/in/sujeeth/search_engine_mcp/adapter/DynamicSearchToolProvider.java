package in.sujeeth.search_engine_mcp.adapter;

import in.sujeeth.search_engine_mcp.config.SearchToolProperties;
import io.modelcontextprotocol.spec.McpSchema;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class DynamicSearchToolProvider {
    
    private final SearchToolProperties properties;
    private final RestToolAdapter adapter;
    
    public DynamicSearchToolProvider(SearchToolProperties properties,
                                     RestToolAdapter adapter) {
        this.properties = properties;
        this.adapter = adapter;
    }
    
    public List<McpSchema.Tool> getTools() {
        return properties.getTools().stream()
            .map(this::createTool)
            .collect(Collectors.toList());
    }
    
    private McpSchema.Tool createTool(SearchToolProperties.Tool config) {
        // Construct JSON Schema as per McpSchema.JsonSchema constructor signature:
        // (type, properties, required, additionalProperties, patternProperties, definitions)
        Map<String, Object> queryProp = new HashMap<>();
        queryProp.put("type", "string");
        queryProp.put("description", "Search query string");

        Map<String, Object> props = new HashMap<>();
        props.put("query", queryProp);

        McpSchema.JsonSchema inputSchema = new McpSchema.JsonSchema(
            "object",
            props,
            List.of("query"),
            null,
            null,
            null
        );

        return new McpSchema.Tool.Builder()
            .name(config.getName())
            .description(config.getDescription())
            .inputSchema(inputSchema)
            .build();
    }
    
    public Object executeTool(String toolName, Map<String, Object> arguments) {
        SearchToolProperties.Tool config = properties.getTools().stream()
            .filter(t -> t.getName().equals(toolName))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Unknown tool: " + toolName));
        
        String result = adapter.execute(
            HttpMethod.valueOf(config.getMethod()),
            config.getUrl(),
            arguments
        );
        
        Map<String, Object> response = new HashMap<>();
        response.put("content", List.of(Map.of(
            "type", "text",
            "text", result
        )));
        
        return response;
    }
}