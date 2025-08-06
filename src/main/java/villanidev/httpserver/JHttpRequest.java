package villanidev.httpserver;

import com.sun.net.httpserver.HttpExchange;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
public class JHttpRequest {
    private final HttpExchange exchange;
    private String body;
    private final Map<String, String> pathParams = new HashMap<>();

    public JHttpRequest(HttpExchange exchange) {
        this.exchange = exchange;
    }

    public String getMethod() {
        return exchange.getRequestMethod();
    }

    public String getPath() {
        return exchange.getRequestURI().getPath();
    }

    public String getBody() {
        if (body == null) {
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(exchange.getRequestBody()))) {
                body = reader.lines().collect(Collectors.joining());
            } catch (IOException e) {
                body = "";
            }
        }
        return body;
    }

    public String getQueryParam(String name) {
        String query = exchange.getRequestURI().getQuery();
        if (query == null) return null;

        for (String param : query.split("&")) {
            String[] pair = param.split("=");
            if (pair.length > 0 && pair[0].equals(name)) {
                return pair.length > 1 ? pair[1] : "";
            }
        }
        return null;
    }

    public void addPathParam(String name, String value) {
        pathParams.put(name, value);
    }

    public String getPathParam(String name) {
        return pathParams.get(name);
    }
}
