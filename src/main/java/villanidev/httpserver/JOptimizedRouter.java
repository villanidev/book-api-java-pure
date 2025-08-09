package villanidev.httpserver;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JOptimizedRouter {

    // Define HTTP methods we support
    public enum HttpMethod {
        GET, POST, PUT, DELETE, HEAD, OPTIONS
    }
    private static class Route {
        final Pattern pattern;
        final RouteHandler handler;
        final List<String> paramNames;

        Route(String pathTemplate, RouteHandler handler) {
            this.handler = handler;
            this.paramNames = new ArrayList<>();

            // Convert path like "/books/:id" to regex pattern
            String regex = pathTemplate.replaceAll(":([^/]+)", "(?<$1>[^/]+)");
            this.pattern = Pattern.compile("^" + regex + "$");

            Matcher m = Pattern.compile(":([^/]+)").matcher(pathTemplate);
            while (m.find()) {
                paramNames.add(m.group(1));
            }
        }
    }

    private final Map<String, Route>[] routeMaps;

    @SuppressWarnings("unchecked")
    public JOptimizedRouter() {
        routeMaps = new ConcurrentHashMap[HttpMethod.values().length];
        for (int i = 0; i < routeMaps.length; i++) {
            routeMaps[i] = new ConcurrentHashMap<>();
        }
    }

    public void addRoute(String method, String path, RouteHandler handler) {
        HttpMethod httpMethod;
        try {
            httpMethod = HttpMethod.valueOf(method.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Unsupported HTTP method: " + method);
        }
        routeMaps[HttpMethod.valueOf(method).ordinal()].put(path, new Route(path, handler));
    }

    public void handle(JHttpRequest request, JHttpResponse response) {
        String path = request.getPath();
        String method = request.getMethod();

        try {
            HttpMethod httpMethod = HttpMethod.valueOf(method.toUpperCase());
            Route route = findRoute(httpMethod, path);

            if (route != null) {
                extractPathParams(route, path, request);
                route.handler.handle(request, response);
            } else {
                response.status(404).send("Not Found");
            }
        } catch (IllegalArgumentException e) {
            response.status(405).send("Method Not Allowed");
        }
    }

    private Route findRoute(HttpMethod method, String path) {
        // First try exact match
        Route route = routeMaps[method.ordinal()].get(path);
        if (route != null) return route;

        // Then try pattern matching
        for (Route r : routeMaps[method.ordinal()].values()) {
            if (r.pattern.matcher(path).matches()) {
                return r;
            }
        }
        return null;
    }

    private void extractPathParams(Route route, String path, JHttpRequest request) {
        Matcher m = route.pattern.matcher(path);
        if (m.matches()) {
            for (String param : route.paramNames) {
                request.addPathParam(param, m.group(param));
            }
        }
    }
}
