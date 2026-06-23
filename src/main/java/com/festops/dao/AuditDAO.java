package com.festops.dao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Plain-JDBC data access for the audit log, backed by the embedded H2 database.
 *
 * <p>Uses {@link DriverManager}, {@link PreparedStatement} and {@link ResultSet}
 * with try-with-resources for connection handling. The {@code DB_CLOSE_DELAY=-1}
 * flag keeps the in-memory database alive for the lifetime of the JVM even
 * between connections.</p>
 */
public class AuditDAO {

    private static final String URL = "jdbc:h2:mem:festops;DB_CLOSE_DELAY=-1";
    private static final String USER = "sa";
    private static final String PASSWORD = "";

    public AuditDAO() {
        initSchema();
    }

    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }

    /** Creates the audit table if it does not yet exist. */
    public void initSchema() {
        String ddl = "CREATE TABLE IF NOT EXISTS audit_log ("
                + "id BIGINT AUTO_INCREMENT PRIMARY KEY, "
                + "incident_id VARCHAR(64) NOT NULL, "
                + "from_state VARCHAR(32) NOT NULL, "
                + "to_state VARCHAR(32) NOT NULL, "
                + "ts TIMESTAMP NOT NULL)";
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(ddl);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to initialise audit schema", e);
        }
    }

    /** Records a single state transition. */
    public void record(String incidentId, String fromState, String toState) {
        String sql = "INSERT INTO audit_log (incident_id, from_state, to_state, ts) "
                + "VALUES (?, ?, ?, ?)";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, incidentId);
            ps.setString(2, fromState);
            ps.setString(3, toState);
            ps.setTimestamp(4, Timestamp.from(Instant.now()));
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to record audit entry", e);
        }
    }

    /** Returns every audit entry, oldest first, as human-readable lines. */
    public List<String> findAll() {
        String sql = "SELECT incident_id, from_state, to_state, ts "
                + "FROM audit_log ORDER BY id";
        List<String> rows = new ArrayList<>();
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                rows.add(String.format("%-8s %-12s -> %-12s @ %s",
                        rs.getString("incident_id"),
                        rs.getString("from_state"),
                        rs.getString("to_state"),
                        rs.getTimestamp("ts")));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to read audit log", e);
        }
        return rows;
    }

    /** Returns the audit trail for a single incident, oldest first. */
    public List<String> findByIncident(String incidentId) {
        String sql = "SELECT from_state, to_state, ts "
                + "FROM audit_log WHERE incident_id = ? ORDER BY id";
        List<String> rows = new ArrayList<>();
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, incidentId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    rows.add(String.format("%s -> %s @ %s",
                            rs.getString("from_state"),
                            rs.getString("to_state"),
                            rs.getTimestamp("ts")));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to read audit trail for " + incidentId, e);
        }
        return rows;
    }

    /** Total number of audit entries recorded. */
    public int count() {
        String sql = "SELECT COUNT(*) FROM audit_log";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getInt(1) : 0;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to count audit log", e);
        }
    }
}
