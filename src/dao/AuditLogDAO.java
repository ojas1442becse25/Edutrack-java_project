package dao;

import database.DBConnection;
import exception.InvalidDataException;
import exception.RecordNotFoundException;
import model.AuditLog;
import model.AuditLog.ActionType;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class AuditLogDAO {

    private static final String SQL_INSERT =
            "INSERT INTO AUDIT_LOG (entity_name, entity_id, action_type, performed_by, details) " +
            "VALUES (?, ?, ?, ?, ?)";

    private static final String SQL_FIND_BY_ID =
            "SELECT log_id, entity_name, entity_id, action_type, performed_by, action_timestamp, details " +
            "FROM AUDIT_LOG WHERE log_id = ?";

    private static final String SQL_FIND_RECENT =
            "SELECT log_id, entity_name, entity_id, action_type, performed_by, action_timestamp, details " +
            "FROM AUDIT_LOG ORDER BY log_id DESC LIMIT ?";

    private static final String SQL_FIND_BY_ENTITY =
            "SELECT log_id, entity_name, entity_id, action_type, performed_by, action_timestamp, details " +
            "FROM AUDIT_LOG WHERE entity_name = ? AND entity_id = ? ORDER BY log_id DESC";

    private static final String SQL_DELETE =
            "DELETE FROM AUDIT_LOG WHERE log_id = ?";

        public AuditLog insert(Connection conn, AuditLog log) throws SQLException {
        if (conn == null) {
            throw new IllegalArgumentException("Connection cannot be null for transactional insert.");
        }
        if (log == null) {
            throw new IllegalArgumentException("AuditLog entity cannot be null.");
        }

        try (PreparedStatement ps = conn.prepareStatement(SQL_INSERT, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, log.getEntityName());
            ps.setInt(2, log.getEntityId());
            ps.setString(3, log.getActionType() != null ? log.getActionType().name() : ActionType.INSERT.name());
            ps.setString(4, log.getPerformedBy());
            ps.setString(5, log.getDetails());

            int affectedRows = ps.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Creating audit log failed, no rows affected.");
            }

            try (ResultSet generatedKeys = ps.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    log.setLogId(generatedKeys.getInt(1));
                }
            }
            return log;
        }
    }

        public AuditLog create(AuditLog log) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            return insert(conn, log);
        }
    }

        public Optional<AuditLog> findById(int logId) throws SQLException, InvalidDataException {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_FIND_BY_ID)) {

            ps.setInt(1, logId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToAuditLog(rs));
                }
            }
        }
        return Optional.empty();
    }

        public List<AuditLog> findRecent(int limit) throws SQLException, InvalidDataException {
        List<AuditLog> logs = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_FIND_RECENT)) {

            ps.setInt(1, limit > 0 ? limit : 20);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    logs.add(mapResultSetToAuditLog(rs));
                }
            }
        }
        return logs;
    }

        public List<AuditLog> findByEntity(String entityName, int entityId) throws SQLException, InvalidDataException {
        List<AuditLog> logs = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_FIND_BY_ENTITY)) {

            ps.setString(1, entityName != null ? entityName.trim().toUpperCase() : "");
            ps.setInt(2, entityId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    logs.add(mapResultSetToAuditLog(rs));
                }
            }
        }
        return logs;
    }

        public boolean delete(int logId) throws SQLException, RecordNotFoundException {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_DELETE)) {

            ps.setInt(1, logId);
            int affected = ps.executeUpdate();
            if (affected == 0) {
                throw new RecordNotFoundException("Audit log with ID " + logId + " not found for deletion.");
            }
            return true;
        }
    }

    private AuditLog mapResultSetToAuditLog(ResultSet rs) throws SQLException, InvalidDataException {
        Timestamp ts = rs.getTimestamp("action_timestamp");
        return new AuditLog(
                rs.getInt("log_id"),
                rs.getString("entity_name"),
                rs.getInt("entity_id"),
                ActionType.fromString(rs.getString("action_type")),
                rs.getString("performed_by"),
                ts != null ? ts.toLocalDateTime() : null,
                rs.getString("details")
        );
    }
}
