package com.military.aircraft;

import com.military.aircraft.model.V2ResponseAcItem;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.http.HttpClient;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class AdsBApiClientTest {

    private SimpleMeterRegistry registry;
    private com.sun.net.httpserver.HttpServer mockServer;

    @BeforeEach
    void setUp() throws Exception {
        registry = new SimpleMeterRegistry();
    }

    @AfterEach
    void tearDown() {
        if (mockServer != null) {
            mockServer.stop(0);
        }
    }

    @Test
    void fetchMilitaryFlights_success_returnsItems() throws Exception {
        String validJson = "{\"ac\":[{\"hex\":\"ae2902\",\"flight\":\"C6018\",\"lat\":57.57,\"lon\":-152.57}]}";
        mockServer = com.sun.net.httpserver.HttpServer.create(new InetSocketAddress(0), 0);
        mockServer.createContext("/mil", exchange -> {
            byte[] body = validJson.getBytes();
            exchange.sendResponseHeaders(200, body.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(body);
            }
        });
        mockServer.start();
        int port = mockServer.getAddress().getPort();
        String apiUrl = "http://localhost:" + port + "/mil";

        AdsBApiClient client = new AdsBApiClient(apiUrl, HttpClient.newHttpClient(), registry);
        List<V2ResponseAcItem> result = client.fetchMilitaryFlights();

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("ae2902", result.get(0).getHex());
        assertEquals("C6018", result.get(0).getFlight());
        assertEquals(1, registry.counter("adsb.api.calls.total").count());
        assertEquals(1, registry.counter("adsb.api.success.total").count());
    }

    @Test
    void fetchMilitaryFlights_failure_http500_returnsEmpty() throws Exception {
        mockServer = com.sun.net.httpserver.HttpServer.create(new InetSocketAddress(0), 0);
        mockServer.createContext("/mil", exchange -> exchange.sendResponseHeaders(500, -1));
        mockServer.start();
        int port = mockServer.getAddress().getPort();
        String apiUrl = "http://localhost:" + port + "/mil";

        AdsBApiClient client = new AdsBApiClient(apiUrl, HttpClient.newHttpClient(), registry);
        List<V2ResponseAcItem> result = client.fetchMilitaryFlights();

        assertNotNull(result);
        assertTrue(result.isEmpty());
        assertEquals(1, registry.counter("adsb.api.errors.total").count());
    }

    @Test
    void fetchMilitaryFlights_failure_malformedJson_returnsEmpty() throws Exception {
        mockServer = com.sun.net.httpserver.HttpServer.create(new InetSocketAddress(0), 0);
        mockServer.createContext("/mil", exchange -> {
            byte[] body = "not json".getBytes();
            exchange.sendResponseHeaders(200, body.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(body);
            }
        });
        mockServer.start();
        int port = mockServer.getAddress().getPort();
        String apiUrl = "http://localhost:" + port + "/mil";

        AdsBApiClient client = new AdsBApiClient(apiUrl, HttpClient.newHttpClient(), registry);
        List<V2ResponseAcItem> result = client.fetchMilitaryFlights();

        assertNotNull(result);
        assertTrue(result.isEmpty());
        assertEquals(1, registry.counter("adsb.api.errors.total").count());
    }
}
