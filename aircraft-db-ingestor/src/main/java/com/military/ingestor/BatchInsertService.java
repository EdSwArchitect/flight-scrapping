package com.military.ingestor;

import com.military.ingestor.model.FlightRecord;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;

import java.sql.*;
import java.util.List;

@Slf4j
public class BatchInsertService {

    private static final String INSERT_SQL = """
            INSERT INTO flights (hex, flight, r, t, lat, lon, alt_baro, gs, track, seen, raw)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?::jsonb)
            ON CONFLICT (hex) DO UPDATE SET
                flight = EXCLUDED.flight,
                r = EXCLUDED.r,
                t = EXCLUDED.t,
                lat = EXCLUDED.lat,
                lon = EXCLUDED.lon,
                alt_baro = EXCLUDED.alt_baro,
                gs = EXCLUDED.gs,
                track = EXCLUDED.track,
                seen = EXCLUDED.seen,
                raw = EXCLUDED.raw,
                updated_at = NOW()
            """;

    private final String jdbcUrl;
    private final String username;
    private final String password;
    private final Timer batchDuration;
    private final io.micrometer.core.instrument.Counter batchRecords;

    public BatchInsertService(String jdbcUrl, String username, String password, MeterRegistry registry) {
        this.jdbcUrl = jdbcUrl;
        this.username = username;
        this.password = password;
        this.batchDuration = registry.timer("ingestor.batch.duration");
        this.batchRecords = registry.counter("ingestor.batch.records");
    }

    public int insertBatch(List<FlightRecord> records) {
        if (records.isEmpty()) return 0;

        long start = System.nanoTime();
        try (Connection conn = DriverManager.getConnection(jdbcUrl, username, password);
             PreparedStatement ps = conn.prepareStatement(INSERT_SQL)) {

            for (FlightRecord r : records) {
                ps.setString(1, r.getHex());
                ps.setString(2, truncate(r.getFlight(), 32));
                ps.setString(3, truncate(r.getR(), 32));
                ps.setString(4, truncate(r.getT(), 16));
                ps.setObject(5, r.getLat(), Types.DOUBLE);
                ps.setObject(6, r.getLon(), Types.DOUBLE);
                ps.setObject(7, r.getAltBaro(), Types.INTEGER);
                ps.setObject(8, r.getGs(), Types.DOUBLE);
                ps.setObject(9, r.getTrack(), Types.DOUBLE);
                ps.setTimestamp(10, r.getSeen());
                ps.setString(11, r.getRaw());
                ps.addBatch();
            }
            int[] results = ps.executeBatch();
            int count = results.length;
            batchRecords.increment(count);
            batchDuration.record(java.time.Duration.ofNanos(System.nanoTime() - start));
            log.debug("Inserted batch of {} records", count);
            return count;
        } catch (SQLException e) {
            log.error("Batch insert failed", e);
            throw new RuntimeException("Batch insert failed", e);
        }
    }

    private String truncate(String s, int maxLen) {
        if (s == null) return null;
        return s.length() <= maxLen ? s : s.substring(0, maxLen);
    }
}
