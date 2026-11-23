package dao;

import database.DBConnection;
import exception.InvalidDataException;
import exception.RecordNotFoundException;
import model.ClassProgressLog;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Data Access Object for the CLASS_PROGRESS_LOG table.
 */
public class ClassProgressLogDAO {

    private static final String SQL_INSERT =
            "INSERT INTO CLASS_PROGRESS_LOG (class_id, log_date, topic_covered, hours_spent, remarks) " +
            "VALUES (?, ?, ?, ?, ?)";

    private static final String SQL_FIND_BY_ID =
            "SELECT log_id, class_id, log_date, topic_covered, hours_spent, remarks, created_at " +
            "FROM CLASS_PROGRESS_LOG WHERE log_id = ?";

    private static final String SQL_FIND_BY_CLASS =
            "SELECT log_id, class_id, log_date, topic_covered, hours_spent, remarks, created_at " +
            "FROM CLASS_PROGRESS_LOG WHERE class_id = ? ORDER BY log_date DESC";

    private static final String SQL_DELETE =
            "DELETE FROM CLASS_PROGRESS_LOG WHERE log_id = ?";

    /**
     * Inserts a new teaching progress log entry.
     */
    public ClassProgressLog create(ClassProgressLog log) throws SQLException {
        if (log == null) {
            throw new IllegalArgumentException("ClassProgressLog cannot be null.");
        }

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_INSERT, Statement.RETURN_GENERATED_KEYS)) {

            ps.setInt(1, log.getClassId());
            ps.setDate(2, Date.valueOf(log.getLogDate()));
            ps.setString(3, log.getTopicCovered());
            ps.setDouble(4, log.getHoursSpent());
            ps.setString(5, log.getRemarks());

            int affectedRows = ps.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Creating progress log failed, no rows affected.");
            }

            try (ResultSet generatedKeys = ps.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    log.setLogId(generatedKeys.getInt(1));
                }
            }
            return log;
        }
    }

    /**
     * Finds a progress log by ID.
     */
    public Optional<ClassProgressLog> findById(int logId) throws SQLException, InvalidDataException {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_FIND_BY_ID)) {

            ps.setInt(1, logId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToLog(rs));
                }
            }
        }
        return Optional.empty();
    }

    /**
     * Retrieves all progress logs recorded for a specific class.
     */
    public List<ClassProgressLog> findByClassId(int classId) throws SQLException, InvalidDataException {
        List<ClassProgressLog> list = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_FIND_BY_CLASS)) {

            ps.setInt(1, classId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToLog(rs));
                }
            }
        }
        return list;
    }

    /**
     * Deletes a progress log by ID.
     */
    public boolean delete(int logId) throws SQLException, RecordNotFoundException {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_DELETE)) {

            ps.setInt(1, logId);
            int affectedRows = ps.executeUpdate();
            if (affectedRows == 0) {
                throw new RecordNotFoundException("Progress log with ID " + logId + " not found for deletion.");
            }
            return true;
        }
    }

    private ClassProgressLog mapResultSetToLog(ResultSet rs) throws SQLException, InvalidDataException {
        Date logDate = rs.getDate("log_date");
        Timestamp ts = rs.getTimestamp("created_at");

        return new ClassProgressLog(
                rs.getInt("log_id"),
                rs.getInt("class_id"),
                logDate != null ? logDate.toLocalDate() : null,
                rs.getString("topic_covered"),
                rs.getDouble("hours_spent"),
                rs.getString("remarks"),
                ts != null ? ts.toLocalDateTime() : null
        );
    }
}
