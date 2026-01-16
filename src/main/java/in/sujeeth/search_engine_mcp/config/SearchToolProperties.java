package in.sujeeth.search_engine_mcp.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@Data
@ConfigurationProperties(prefix = "search")
public class SearchToolProperties {

    private List<Tool> tools;

    @Data
    public static class Tool {
        private String name;
        private String description;
        private String method;
        private String url;
    }
}
