package com.assessment.app;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class WeatherDatabase {
    private final String dbPath;

    public WeatherDatabase(String dbPath) {
        this.dbPath = dbPath;
        init();
    }

    private void init() {
        try (Connection c = DriverManager.getConnection("jdbc:sqlite:" + dbPath)) {
            try (Statement st = c.createStatement()) {
                st.execute("CREATE TABLE IF NOT EXISTS weather_requests (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                        "created_at TEXT NOT NULL," +
                        "location_input TEXT NOT NULL," +
                        "resolved_location TEXT," +
                        "latitude REAL," +
                        "longitude REAL," +
                        "start_date TEXT," +
                        "end_date TEXT," +
                        "provider TEXT," +
                        "payload_json TEXT)");
            }
        } catch (SQLException e) {
            throw new RuntimeException("DB init failed: " + e.getMessage(), e);
        }
    }

    public int create(WeatherRecord r) {
        String sql = "INSERT INTO weather_requests (created_at, location_input, resolved_location, latitude, longitude, start_date, end_date, provider, payload_json) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection c = DriverManager.getConnection("jdbc:sqlite:" + dbPath);
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, r.createdAt);
            ps.setString(2, r.locationInput);
            ps.setString(3, r.resolvedLocation);
            ps.setDouble(4, r.latitude);
            ps.setDouble(5, r.longitude);
            ps.setString(6, r.startDate);
            ps.setString(7, r.endDate);
            ps.setString(8, r.provider);
            ps.setString(9, r.payloadJson);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) return rs.getInt(1);
            }
            return -1;
        } catch (SQLException e) {
            throw new RuntimeException("DB create failed: " + e.getMessage(), e);
        }
    }

    public List<WeatherRecord> readAll() {
        String sql = "SELECT * FROM weather_requests ORDER BY id DESC";
        List<WeatherRecord> out = new ArrayList<>();
        try (Connection c = DriverManager.getConnection("jdbc:sqlite:" + dbPath);
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                WeatherRecord r = new WeatherRecord();
                r.id = rs.getInt("id");
                r.createdAt = rs.getString("created_at");
                r.locationInput = rs.getString("location_input");
                r.resolvedLocation = rs.getString("resolved_location");
                r.latitude = rs.getDouble("latitude");
                r.longitude = rs.getDouble("longitude");
                r.startDate = rs.getString("start_date");
                r.endDate = rs.getString("end_date");
                r.provider = rs.getString("provider");
                r.payloadJson = rs.getString("payload_json");
                out.add(r);
            }
        } catch (SQLException e) {
            throw new RuntimeException("DB readAll failed: " + e.getMessage(), e);
        }
        return out;
    }

    public void updateBasic(int id, String newLocationInput, String newStart, String newEnd,
                            String newResolved, Double newLat, Double newLon, String newPayloadJson) {
        StringBuilder sb = new StringBuilder("UPDATE weather_requests SET ");
        boolean first = true;
        if (newLocationInput != null) { sb.append("location_input=?"); first=false; }
        if (newStart != null) { if (!first) sb.append(","); sb.append("start_date=?"); first=false; }
        if (newEnd != null) { if (!first) sb.append(","); sb.append("end_date=?"); first=false; }
        if (newResolved != null) { if (!first) sb.append(","); sb.append("resolved_location=?"); first=false; }
        if (newLat != null) { if (!first) sb.append(","); sb.append("latitude=?"); first=false; }
        if (newLon != null) { if (!first) sb.append(","); sb.append("longitude=?"); first=false; }
        if (newPayloadJson != null) { if (!first) sb.append(","); sb.append("payload_json=?"); first=false; }
        sb.append(" WHERE id=?");

        try (Connection c = DriverManager.getConnection("jdbc:sqlite:" + dbPath);
             PreparedStatement ps = c.prepareStatement(sb.toString())) {
            int idx = 1;
            if (newLocationInput != null) ps.setString(idx++, newLocationInput);
            if (newStart != null) ps.setString(idx++, newStart);
            if (newEnd != null) ps.setString(idx++, newEnd);
            if (newResolved != null) ps.setString(idx++, newResolved);
            if (newLat != null) ps.setDouble(idx++, newLat);
            if (newLon != null) ps.setDouble(idx++, newLon);
            if (newPayloadJson != null) ps.setString(idx++, newPayloadJson);
            ps.setInt(idx, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("DB update failed: " + e.getMessage(), e);
        }
    }

    public void delete(int id) {
        try (Connection c = DriverManager.getConnection("jdbc:sqlite:" + dbPath);
             PreparedStatement ps = c.prepareStatement("DELETE FROM weather_requests WHERE id=?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("DB delete failed: " + e.getMessage(), e);
        }
    }
}
