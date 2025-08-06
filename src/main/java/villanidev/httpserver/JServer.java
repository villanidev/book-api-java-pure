package villanidev.httpserver;

import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class JServer {
    private final HttpServer server;

    private JServer(int port, JRouter router) throws IOException {
        this.server = HttpServer.create(new InetSocketAddress(port), 0);
        this.server.setExecutor(getVirtualThreadPerTaskExecutor());
        this.server.createContext("/", exchange -> {
            try {
                JHttpRequest request = new JHttpRequest(exchange);
                JHttpResponse response = new JHttpResponse(exchange);
                router.handle(request, response);
            } catch (Exception e) {
                exchange.sendResponseHeaders(500, 0);
                exchange.close();
            }
        });
    }

    private ExecutorService getVirtualThreadPerTaskExecutor() {
        return Executors.newVirtualThreadPerTaskExecutor();
    }

    public void start() {
        server.start();
        System.out.println("Server started on port " + server.getAddress().getPort());
    }

    public static class Builder {
        private int port;
        private final JRouter router = new JRouter();
        //private final JOptimizedRouter router = new JOptimizedRouter();
        public Builder port(int port) {
            this.port = port;
            return this;
        }

        public Builder addRoute(String method, String path, RouteHandler handler) {
            router.addRoute(method, path, handler);
            return this;
        }

        public JServer build() throws IOException {
            return new JServer(port, router);
        }
    }
}
