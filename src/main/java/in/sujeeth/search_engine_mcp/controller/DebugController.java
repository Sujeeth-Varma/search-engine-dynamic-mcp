package in.sujeeth.search_engine_mcp.controller;

import in.sujeeth.search_engine_mcp.adapter.DynamicSearchToolProvider;
import in.sujeeth.search_engine_mcp.config.SearchToolProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/debug")
@ConditionalOnProperty(name = "debug.enabled", havingValue = "true")
public class DebugController {
    private final DynamicSearchToolProvider toolProvider;
    private final SearchToolProperties properties;

    public DebugController(DynamicSearchToolProvider toolProvider,
                           SearchToolProperties properties) {
        this.toolProvider = toolProvider;
        this.properties = properties;
    }

    @GetMapping("/tools")
    public Map<String, Object> listTools() {
        Map<String, Object> response = new HashMap<>();
        response.put("tools", toolProvider.getTools());
        response.put("count", properties.getTools().size());
        return response;
    }


    @GetMapping("/tools/{toolName}")
    public Map<String, Object> getToolDetails(@PathVariable String toolName) {
        SearchToolProperties.Tool config = properties.getTools().stream()
                .filter(t -> t.getName().equals(toolName))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Tool not found: " + toolName));

        Map<String, Object> response = new HashMap<>();
        response.put("name", config.getName());
        response.put("description", config.getDescription());
        response.put("method", config.getMethod());
        response.put("url", config.getUrl());
        return response;
    }

    @PostMapping("/execute/{toolName}")
    public Map<String, Object> executeTool(
            @PathVariable String toolName,
            @RequestBody Map<String, Object> arguments) {

        try {
            Object result = toolProvider.executeTool(toolName, arguments);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("toolName", toolName);
            response.put("arguments", arguments);
            response.put("result", result);
            return response;
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", e.getMessage());
            return response;
        }
    }

    // Health check
    @GetMapping("/health")
    public Map<String, Object> health() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "UP");
        response.put("toolsRegistered", properties.getTools().size());
        response.put("toolNames", properties.getTools().stream()
                .map(SearchToolProperties.Tool::getName)
                .collect(Collectors.toList()));
        return response;
    }

    @GetMapping("/config")
    public Map<String, Object> getConfig() {
        Map<String, Object> response = new HashMap<>();
        response.put("tools", properties.getTools());
        return response;
    }
}
