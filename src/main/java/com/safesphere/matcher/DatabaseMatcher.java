package com.safesphere.matcher;

import com.safesphere.event.SafeSphereEventBus;
import com.safesphere.event.TimelineLogEvent;
import com.safesphere.model.Volunteer;

import java.sql.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Relational Capability Matcher (M6).
 * Executes multi-factor relational SQL queries to pair emergencies with the most capable,
 * proximity-optimized verified responders (Section 10).
 */
public class DatabaseMatcher {
    private static final String JDBC_URL = "jdbc:sqlite::memory:";
    private final SafeSphereEventBus eventBus = SafeSphereEventBus.getInstance();
    private Connection connection;
    private final List<Volunteer> fallbackRegistry = new ArrayList<>();

    public DatabaseMatcher() {
        initDatabase();
    }

    private synchronized void initDatabase() {
        try {
            Class.forName("org.sqlite.JDBC");
            this.connection = DriverManager.getConnection(JDBC_URL);
            createSchema();
            seedDefaultVolunteers();
            eventBus.publish(new TimelineLogEvent("M6_MATCHER",
                    "SQLite In-Memory Capability Matcher initialized and seeded with verified volunteers."));
        } catch (Throwable t) {
            System.err.println("[DatabaseMatcher] SQLite JDBC initialization note: " + t.getMessage() + ". Using robust in-memory matcher fallback.");
            seedFallbackRegistry();
        }
    }

    private void createSchema() throws SQLException {
        String sql = """
            CREATE TABLE IF NOT EXISTS verified_volunteers (
                responder_id TEXT PRIMARY KEY,
                name TEXT NOT NULL,
                phone TEXT NOT NULL,
                latitude REAL NOT NULL,
                longitude REAL NOT NULL,
                distance_km REAL NOT NULL,
                cpr_cert INTEGER NOT NULL,
                vehicle_access INTEGER NOT NULL,
                status TEXT NOT NULL,
                specialization TEXT NOT NULL
            );
        """;
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(sql);
        }
    }

    private void seedDefaultVolunteers() throws SQLException {
        String insertSql = """
            INSERT OR REPLACE INTO verified_volunteers 
            (responder_id, name, phone, latitude, longitude, distance_km, cpr_cert, vehicle_access, status, specialization)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?);
        """;

        Object[][] sampleData = {
                // Closer, but NO CPR or Vehicle -> Score = (1/0.4)*0.4 = 1.00
                {"VOL-101", "Ramesh Sharma", "+91-98491-11001", 17.3870, 78.4890, 0.40, 0, 0, "ONLINE", "General Assistant"},
                // Further, but HAS CPR & Vehicle -> Score = (1/0.8)*0.4 + 0.3 + 0.3 = 0.50 + 0.60 = 1.10 (WINS over VOL-101!)
                {"VOL-204", "Dr. Ananya Reddy", "+91-98492-22002", 17.3910, 78.4920, 0.80, 1, 1, "ONLINE", "Doctor / CPR Certified"},
                // Very close with CPR -> Score = (1/0.25)*0.4 + 0.3 + 0.3 = 1.6 + 0.6 = 2.20
                {"VOL-305", "Vikram Singh", "+91-98493-33003", 17.3860, 78.4875, 0.25, 1, 1, "ONLINE", "EMT / First Responder"},
                // Close with CPR but BUSY -> Must be excluded!
                {"VOL-409", "Priya Nair", "+91-98494-44004", 17.3865, 78.4880, 0.30, 1, 0, "BUSY", "Nurse / First Aid"},
                // Moderate distance
                {"VOL-512", "Kiran Kumar", "+91-98495-55005", 17.3950, 78.4960, 1.20, 1, 1, "ONLINE", "Red Cross Volunteer"}
        };

        try (PreparedStatement pstmt = connection.prepareStatement(insertSql)) {
            for (Object[] row : sampleData) {
                pstmt.setString(1, (String) row[0]);
                pstmt.setString(2, (String) row[1]);
                pstmt.setString(3, (String) row[2]);
                pstmt.setDouble(4, (Double) row[3]);
                pstmt.setDouble(5, (Double) row[4]);
                pstmt.setDouble(6, (Double) row[5]);
                pstmt.setInt(7, (Integer) row[6]);
                pstmt.setInt(8, (Integer) row[7]);
                pstmt.setString(9, (String) row[8]);
                pstmt.setString(10, (String) row[9]);
                pstmt.executeUpdate();

                // Also keep fallback updated
                fallbackRegistry.add(new Volunteer(
                        (String) row[0], (String) row[1], (String) row[2],
                        (Double) row[3], (Double) row[4], (Double) row[5],
                        (Integer) row[6], (Integer) row[7], (String) row[8], (String) row[9]
                ));
            }
        }
    }

    private void seedFallbackRegistry() {
        fallbackRegistry.clear();
        fallbackRegistry.add(new Volunteer("VOL-101", "Ramesh Sharma", "+91-98491-11001", 17.3870, 78.4890, 0.40, 0, 0, "ONLINE", "General Assistant"));
        fallbackRegistry.add(new Volunteer("VOL-204", "Dr. Ananya Reddy", "+91-98492-22002", 17.3910, 78.4920, 0.80, 1, 1, "ONLINE", "Doctor / CPR Certified"));
        fallbackRegistry.add(new Volunteer("VOL-305", "Vikram Singh", "+91-98493-33003", 17.3860, 78.4875, 0.25, 1, 1, "ONLINE", "EMT / First Responder"));
        fallbackRegistry.add(new Volunteer("VOL-409", "Priya Nair", "+91-98494-44004", 17.3865, 78.4880, 0.30, 1, 0, "BUSY", "Nurse / First Aid"));
        fallbackRegistry.add(new Volunteer("VOL-512", "Kiran Kumar", "+91-98495-55005", 17.3950, 78.4960, 1.20, 1, 1, "ONLINE", "Red Cross Volunteer"));
    }

    /**
     * Executes the capability matching SQL formula from Section 10:
     * match_score = ( (1 / distance_km) * 0.4 ) + ( cpr_cert * 0.3 ) + ( vehicle_access * 0.3 )
     */
    public synchronized Volunteer findBestMatch(double incidentLat, double incidentLon) {
        if (connection != null) {
            String query = """
                SELECT responder_id, name, phone, latitude, longitude, distance_km, cpr_cert, vehicle_access, status, specialization,
                       ( (1.0 / MAX(0.05, distance_km)) * 0.4 + (cpr_cert * 0.3) + (vehicle_access * 0.3) ) AS match_score
                FROM verified_volunteers
                WHERE status = 'ONLINE'
                ORDER BY match_score DESC
                LIMIT 1;
            """;
            try (Statement stmt = connection.createStatement();
                 ResultSet rs = stmt.executeQuery(query)) {
                if (rs.next()) {
                    Volunteer best = new Volunteer(
                            rs.getString("responder_id"),
                            rs.getString("name"),
                            rs.getString("phone"),
                            rs.getDouble("latitude"),
                            rs.getDouble("longitude"),
                            rs.getDouble("distance_km"),
                            rs.getInt("cpr_cert"),
                            rs.getInt("vehicle_access"),
                            rs.getString("status"),
                            rs.getString("specialization")
                    );
                    best.setMatchScore(rs.getDouble("match_score"));

                    eventBus.publish(new TimelineLogEvent("M6_SQL_MATCH",
                            String.format("SQL Capability Match: Assigned %s (%s) [Score: %.2f | Dist: %.2fkm | CPR: %s | Vehicle: %s]",
                                    best.getResponderId(), best.getName(), best.getMatchScore(),
                                    best.getDistanceKm(), best.hasCpr() ? "YES" : "NO", best.hasVehicle() ? "YES" : "NO")));
                    return best;
                }
            } catch (SQLException e) {
                System.err.println("[DatabaseMatcher] SQL query error: " + e.getMessage() + ". Using fallback.");
            }
        }

        // Fallback computation
        return fallbackRegistry.stream()
                .filter(v -> "ONLINE".equalsIgnoreCase(v.getStatus()))
                .max(Comparator.comparingDouble(Volunteer::getMatchScore))
                .orElse(null);
    }

    /**
     * Retrieves all volunteers in the registry for dispatch table inspection.
     */
    public synchronized List<Volunteer> getAllVolunteers() {
        List<Volunteer> list = new ArrayList<>();
        if (connection != null) {
            String query = "SELECT * FROM verified_volunteers ORDER BY distance_km ASC";
            try (Statement stmt = connection.createStatement();
                 ResultSet rs = stmt.executeQuery(query)) {
                while (rs.next()) {
                    Volunteer v = new Volunteer(
                            rs.getString("responder_id"),
                            rs.getString("name"),
                            rs.getString("phone"),
                            rs.getDouble("latitude"),
                            rs.getDouble("longitude"),
                            rs.getDouble("distance_km"),
                            rs.getInt("cpr_cert"),
                            rs.getInt("vehicle_access"),
                            rs.getString("status"),
                            rs.getString("specialization")
                    );
                    list.add(v);
                }
                return list;
            } catch (SQLException e) {
                System.err.println("[DatabaseMatcher] Query error: " + e.getMessage());
            }
        }
        return new ArrayList<>(fallbackRegistry);
    }

    /**
     * Updates volunteer status (e.g. from ONLINE to DISPATCHED/BUSY).
     */
    public synchronized void updateVolunteerStatus(String responderId, String newStatus) {
        if (connection != null) {
            String sql = "UPDATE verified_volunteers SET status = ? WHERE responder_id = ?";
            try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                pstmt.setString(1, newStatus);
                pstmt.setString(2, responderId);
                pstmt.executeUpdate();
            } catch (SQLException e) {
                System.err.println("[DatabaseMatcher] Update status error: " + e.getMessage());
            }
        }
        fallbackRegistry.stream()
                .filter(v -> v.getResponderId().equalsIgnoreCase(responderId))
                .findFirst()
                .ifPresent(v -> v.setStatus(newStatus));
    }
}
