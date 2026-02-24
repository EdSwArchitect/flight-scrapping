package com.military.watcher;

import com.military.watcher.model.Flight;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class FlightRepository {

    private final String jdbcUrl;
    private final String username;
    private final String password;

    public FlightRepository(String jdbcUrl, String username, String password) {
        this.jdbcUrl = jdbcUrl;
        this.username = username;
        this.password = password;
    }

    public List<Flight> listFlights(int page, int size) {
        String sql = "SELECT id, hex, flight, r, t, lat, lon, alt_baro, gs, track, seen, updated_at FROM flights ORDER BY updated_at DESC LIMIT ? OFFSET ?";
        try (Connection conn = DriverManager.getConnection(jdbcUrl, username, password);
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, size);
            ps.setInt(2, page * size);
            return mapResults(ps.executeQuery());
        } catch (SQLException e) {
            throw new RuntimeException("Failed to list flights", e);
        }
    }

    public Flight getById(String id) {
        String sql = "SELECT id, hex, flight, r, t, lat, lon, alt_baro, gs, track, seen, updated_at FROM flights WHERE hex = ? OR id::text = ?";
        try (Connection conn = DriverManager.getConnection(jdbcUrl, username, password);
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, id);
            ps.setString(2, id);
            List<Flight> results = mapResults(ps.executeQuery());
            return results.isEmpty() ? null : results.get(0);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to get flight", e);
        }
    }

    public List<Flight> listByGeobox(double minLat, double maxLat, double minLon, double maxLon) {
        String sql = "SELECT id, hex, flight, r, t, lat, lon, alt_baro, gs, track, seen, updated_at FROM flights WHERE lat BETWEEN ? AND ? AND lon BETWEEN ? AND ? ORDER BY updated_at DESC";
        try (Connection conn = DriverManager.getConnection(jdbcUrl, username, password);
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDouble(1, minLat);
            ps.setDouble(2, maxLat);
            ps.setDouble(3, minLon);
            ps.setDouble(4, maxLon);
            return mapResults(ps.executeQuery());
        } catch (SQLException e) {
            throw new RuntimeException("Failed to list flights by geobox", e);
        }
    }

    private List<Flight> mapResults(ResultSet rs) throws SQLException {
        List<Flight> list = new ArrayList<>();
        while (rs.next()) {
            list.add(Flight.builder()
                    .id(rs.getLong("id"))
                    .hex(rs.getString("hex"))
                    .flight(rs.getString("flight"))
                    .r(rs.getString("r"))
                    .t(rs.getString("t"))
                    .lat(rs.getObject("lat", Double.class))
                    .lon(rs.getObject("lon", Double.class))
                    .altBaro(rs.getObject("alt_baro", Integer.class))
                    .gs(rs.getObject("gs", Double.class))
                    .track(rs.getObject("track", Double.class))
                    .seen(rs.getTimestamp("seen"))
                    .updatedAt(rs.getTimestamp("updated_at"))
                    .build());
        }
        return list;
    }
}
