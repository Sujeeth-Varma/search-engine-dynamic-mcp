package in.sujeeth.search_engine_mcp;

import in.sujeeth.search_engine_mcp.config.SearchToolProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(SearchToolProperties.class)
public class SearchEngineMcpApplication {

    public static void main(String[] args) {
        SpringApplication.run(SearchEngineMcpApplication.class, args);
    }

}
