package in.sujeeth.search_engine_mcp.config;

import in.sujeeth.search_engine_mcp.adapter.DynamicSearchToolProvider;
import io.modelcontextprotocol.spec.McpSchema;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.function.FunctionToolCallback;
import org.springframework.core.ParameterizedTypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Map;

@Configuration
public class McpToolsConfiguration {

    @Bean
    public List<ToolCallback> searchTools(DynamicSearchToolProvider dynamicSearchToolProvider,
                                          ObjectMapper objectMapper) {
        List<McpSchema.Tool> props = dynamicSearchToolProvider.getTools();
        return props.stream()
            .map(tool -> {
                String schemaJson;
                try {
                    // Serialize MCP JsonSchema to JSON string expected by Spring AI Tool definitions
                    schemaJson = objectMapper.writeValueAsString(tool.inputSchema());
                }
                catch (Exception e) {
                    throw new IllegalStateException("Failed to serialize tool input schema for tool: " + tool.name(), e);
                }

                return FunctionToolCallback.<Map<String, Object>, Object>builder(
                        tool.name(),
                        (args, ctx) -> dynamicSearchToolProvider.executeTool(tool.name(), args))
                    .description(tool.description())
                    .inputSchema(schemaJson)
                    .inputType(new ParameterizedTypeReference<Map<String, Object>>() {})
                    .build();
            })
            .map(cb -> (ToolCallback) cb)
            .toList();
    }
}
