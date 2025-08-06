package villanidev.httpserver;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class JRouter {

    private record RouteKey(String method, String path) {}
    private final Map<RouteKey, RouteHandler> routes = new ConcurrentHashMap<>();

    public void addRoute(String method, String path, RouteHandler handler) {
        //routes.put(new RouteKey(method, path), handler);
        routes.put(new RouteKey(method, normalizePath(path)), handler);
    }

    public void handle(JHttpRequest request, JHttpResponse response) {
        String path = normalizePath(request.getPath());
        String method = request.getMethod();

        // Try exact match first
        RouteHandler handler = routes.get(new RouteKey(method, path));

        // If not found, try pattern matching
        if (handler == null) {
            for (Map.Entry<RouteKey, RouteHandler> entry : routes.entrySet()) {
                if (entry.getKey().method().equals(method) && matchesPattern(entry.getKey().path(), path)) {
                    handler = entry.getValue();
                    extractPathParams(entry.getKey().path(), path, request);
                    break;
                }
            }
        }

        if (handler != null) {
            handler.handle(request, response);
        } else {
            response.status(404).send("Not Found");
        }
    }

    private boolean matchesPattern(String routePath, String requestPath) {
        String[] routeParts = routePath.split("/");
        String[] requestParts = requestPath.split("/");

        if (routeParts.length != requestParts.length) return false;

        for (int i = 0; i < routeParts.length; i++) {
            if (routeParts[i].startsWith(":") && !requestParts[i].isEmpty()) {
                continue;
            }
            if (!routeParts[i].equals(requestParts[i])) {
                return false;
            }
        }
        return true;
    }

    private void extractPathParams(String routePath, String requestPath, JHttpRequest request) {
        String[] routeParts = routePath.split("/");
        String[] requestParts = requestPath.split("/");

        for (int i = 0; i < routeParts.length; i++) {
            if (routeParts[i].startsWith(":")) {
                String paramName = routeParts[i].substring(1);
                request.addPathParam(paramName, requestParts[i]);
            }
        }
    }

    private String normalizePath(String path) {
        return path.replaceAll("/+", "/").replaceAll("/$", "");
    }

    /*public void handle(JHttpRequest request, JHttpResponse response) {
        RouteKey key = new RouteKey(request.getMethod(), request.getPath());
        RouteHandler handler = routes.get(key);

        if (handler != null) {
            handler.handle(request, response);
        } else {
            response.status(404).send("Not Found");
        }
    }*/
}
