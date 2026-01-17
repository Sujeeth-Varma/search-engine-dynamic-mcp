package in.sujeeth.search_engine_mcp.adapter;

import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

@Component
public class RestToolAdapter {

    private final WebClient webClient;

    public RestToolAdapter(WebClient webClient) {
        this.webClient = webClient;
    }

    public String execute(HttpMethod method, String url, Map<String, Object> input) {
        return webClient.method(method)
                .uri(url, input.get("query"))
                .retrieve()
                .bodyToMono(String.class)
                .block();
    }
}
