package com.military.ingestor;

import com.sun.net.httpserver.HttpServer;
import io.micrometer.prometheus.PrometheusMeterRegistry;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

public class MetricsServer {

    private final PrometheusMeterRegistry registry;
    private final int port;
    private HttpServer server;

    public MetricsServer(PrometheusMeterRegistry registry, int port) {
        this.registry = registry;
        this.port = port;
    }

    public void start() {
        try {
            server = HttpServer.create(new InetSocketAddress(port), 0);
            server.createContext("/metrics", exchange -> {
                if ("GET".equals(exchange.getRequestMethod())) {
                    String scrape = registry.scrape();
                    exchange.sendResponseHeaders(200, scrape.getBytes(StandardCharsets.UTF_8).length);
                    try (OutputStream os = exchange.getResponseBody()) {
                        os.write(scrape.getBytes(StandardCharsets.UTF_8));
                    }
                } else {
                    exchange.sendResponseHeaders(405, -1);
                }
            });
            server.setExecutor(null);
            server.start();
        } catch (IOException e) {
            throw new RuntimeException("Failed to start metrics server", e);
        }
    }

    public void stop() {
        if (server != null) {
            server.stop(0);
        }
    }
}
