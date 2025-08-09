package villanidev.httpserver;

import com.fasterxml.jackson.core.JsonGenerator;
import com.sun.net.httpserver.HttpExchange;
import villanidev.httpserver.utils.JsonUtils;

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.OutputStream;
public class JHttpResponse {
    private final HttpExchange exchange;
    private int statusCode = 200;
    private String contentType = "text/plain";

    public JHttpResponse(HttpExchange exchange) {
        this.exchange = exchange;
    }

    public JHttpResponse status(int statusCode) {
        this.statusCode = statusCode;
        return this;
    }

    public void contentType(String contentType) {
        this.contentType = contentType;
    }

    public void send(String body) {
        /*try {
            exchange.getResponseHeaders().set("Content-Type", contentType);
            exchange.sendResponseHeaders(statusCode, body.getBytes().length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(body.getBytes());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }*/

        exchange.getResponseHeaders().set("Content-Type", contentType);
        try (BufferedOutputStream out = new BufferedOutputStream(exchange.getResponseBody())) {
            exchange.sendResponseHeaders(statusCode, 0);
            try (ByteArrayInputStream bis = new ByteArrayInputStream(body.getBytes())) {
                byte [] buffer = new byte [512];
                int count ;
                while ((count = bis.read(buffer)) != -1) {
                    out.write(buffer, 0, count);
                }
                out.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void send(String body, String contentType) {
        this.contentType(contentType);
        this.send(body);
    }

    /*public void json(Object object) {
        this.contentType("application/json");
        this.send(JsonUtils.toJson(object));
    }*/

    // For streaming output in HttpResponse
    public void json(Object object) {
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        try {
            exchange.sendResponseHeaders(statusCode, 0); // 0 for chunked transfer
            try (OutputStream os = exchange.getResponseBody()) {
                JsonUtils.toJsonStreaming(os, object);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
