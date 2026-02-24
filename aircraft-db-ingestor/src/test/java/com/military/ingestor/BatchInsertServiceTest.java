package com.military.ingestor;

import com.military.ingestor.model.FlightRecord;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@Testcontainers
class BatchInsertServiceTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("military_watcher")
            .withUsername("military")
            .withPassword("military")
            .withInitScript("init.sql");

    @Test
    void insertBatch_emptyList_returnsZero() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        BatchInsertService service = new BatchInsertService(
                postgres.getJdbcUrl(),
                postgres.getUsername(),
                postgres.getPassword(),
                registry);

        int result = service.insertBatch(List.of());
        assertEquals(0, result);
    }

    @Test
    void insertBatch_noDb_throwsException() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        BatchInsertService service = new BatchInsertService(
                "jdbc:postgresql://invalid:9999/nonexistent",
                "u", "p", registry);

        FlightRecord r = FlightRecord.builder()
                .hex("ae2902")
                .flight("C6018")
                .lat(57.57)
                .lon(-152.57)
                .build();

        assertThrows(RuntimeException.class, () -> service.insertBatch(List.of(r)));
    }

    @Test
    void insertBatch_withRecords_insertsAndReturnsCount() throws Exception {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        BatchInsertService service = new BatchInsertService(
                postgres.getJdbcUrl(),
                postgres.getUsername(),
                postgres.getPassword(),
                registry);

        FlightRecord r1 = FlightRecord.builder()
                .hex("ae2902")
                .flight("C6018")
                .lat(57.57)
                .lon(-152.57)
                .build();
        FlightRecord r2 = FlightRecord.builder()
                .hex("ae2903")
                .flight("C6019")
                .lat(58.0)
                .lon(-153.0)
                .build();

        int result = service.insertBatch(List.of(r1, r2));

        assertEquals(2, result);

        try (Connection conn = DriverManager.getConnection(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT hex, flight, lat, lon FROM flights ORDER BY hex")) {
            assertTrue(rs.next());
            assertEquals("ae2902", rs.getString("hex"));
            assertEquals("C6018", rs.getString("flight"));
            assertEquals(57.57, rs.getDouble("lat"), 0.001);
            assertEquals(-152.57, rs.getDouble("lon"), 0.001);
            assertTrue(rs.next());
            assertEquals("ae2903", rs.getString("hex"));
            assertEquals("C6019", rs.getString("flight"));
            assertFalse(rs.next());
        }
    }
}
