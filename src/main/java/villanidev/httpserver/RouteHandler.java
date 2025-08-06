package villanidev.httpserver;

@FunctionalInterface
public interface RouteHandler {
    void handle(JHttpRequest request, JHttpResponse response);
}
