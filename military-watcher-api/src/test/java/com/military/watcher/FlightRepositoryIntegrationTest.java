package com.military.watcher;

import com.military.watcher.model.Flight;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@Testcontainers
class FlightRepositoryIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("military_watcher")
            .withUsername("military")
            .withPassword("military")
            .withInitScript("init.sql");

    private FlightRepository repository;

    @BeforeEach
    void setUp() throws Exception {
        repository = new FlightRepository(
                postgres.getJdbcUrl(),
                postgres.getUsername(),
                postgres.getPassword());

        // Insert test data before each test
        try (Connection conn = DriverManager.getConnection(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
             Statement stmt = conn.createStatement()) {
            stmt.execute("""
                    INSERT INTO flights (hex, flight, lat, lon) VALUES
                    ('ae2902', 'C6018', 57.57, -152.57),
                    ('ae2903', 'C6019', 58.0, -153.0),
                    ('ae2904', 'C6020', 35.5, -110.5)
                    ON CONFLICT (hex) DO UPDATE SET flight = EXCLUDED.flight, lat = EXCLUDED.lat, lon = EXCLUDED.lon
                    """);
        }
    }


    @Test
    void listFlights_returnsPaginatedResults() {
        List<Flight> page0 = repository.listFlights(0, 2);
        assertEquals(2, page0.size());

        List<Flight> page1 = repository.listFlights(1, 2);
        assertEquals(1, page1.size());

        List<Flight> empty = repository.listFlights(10, 10);
        assertTrue(empty.isEmpty());
    }

    @Test
    void getById_hexExists_returnsFlight() {
        Flight flight = repository.getById("ae2902");
        assertNotNull(flight);
        assertEquals("ae2902", flight.getHex());
        assertEquals("C6018", flight.getFlight());
        assertEquals(57.57, flight.getLat(), 0.001);
        assertEquals(-152.57, flight.getLon(), 0.001);
    }

    @Test
    void getById_notFound_returnsNull() {
        Flight flight = repository.getById("nonexistent");
        assertNull(flight);
    }

    @Test
    void listByGeobox_returnsFlightsInBounds() {
        // Geobox: lat 57-59, lon -154 to -151 (covers ae2902 and ae2903)
        List<Flight> results = repository.listByGeobox(57.0, 59.0, -154.0, -151.0);
        assertEquals(2, results.size());
        assertTrue(results.stream().anyMatch(f -> "ae2902".equals(f.getHex())));
        assertTrue(results.stream().anyMatch(f -> "ae2903".equals(f.getHex())));

        // Geobox: lat 35-36, lon -111 to -110 (covers ae2904)
        List<Flight> single = repository.listByGeobox(35.0, 36.0, -111.0, -110.0);
        assertEquals(1, single.size());
        assertEquals("ae2904", single.get(0).getHex());
    }

    @Test
    void listByGeobox_noMatches_returnsEmpty() {
        List<Flight> results = repository.listByGeobox(0.0, 1.0, 0.0, 1.0);
        assertTrue(results.isEmpty());
    }
}
