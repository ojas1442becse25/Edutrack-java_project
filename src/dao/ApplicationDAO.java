package dao;

import database.DBConnection;
import exception.DuplicateRecordException;
import exception.InvalidDataException;
import exception.RecordNotFoundException;
import model.Application;
import model.Application.ApplicationStatus;
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

/**
 * ============================================================================
 * VIVA EVALUATION REFERENCE - ATOMIC JDBC TRANSACTION MANAGEMENT:
 * ============================================================================
 * 1. WHAT IS AN ATOMIC TRANSACTION (ACID)?
 *    - Atomicity: Guarantees that all operations within the transaction succeed
 *      or all fail. No intermediate or partial state is visible to other threads.
 *    - In this DAO, inserting a student's job APPLICATION and writing an entry to
 *      AUDIT_LOG occur on the exact same physical Connection.
 *
 * 2. TRANSACTION WORKFLOW:
 *    - Step 1: `conn.setAutoCommit(false)` disables MySQL's default autocommit mode,
 *      marking the start of a multi-statement transaction boundary.
 *    - Step 2: Execute SQL statements (INSERT INTO APPLICATION, then INSERT INTO AUDIT_LOG).
 *    - Step 3: `conn.commit()` makes all modifications permanent in the database.
 *    - Step 4 (On Error): `conn.rollback()` in the catch block undoes all uncommitted
 *      changes, leaving the database completely clean.
 *    - Step 5: `conn.setAutoCommit(true)` in the finally block resets the connection
 *      state before returning it to the pool, preventing accidental transaction leaks.
 * ============================================================================
 */
public class ApplicationDAO {

    private static final String SQL_INSERT =
            "INSERT INTO APPLICATION (drive_id, student_id, status, interview_feedback) " +
            "VALUES (?, ?, ?, ?)";

    private static final String SQL_FIND_BY_ID =
            "SELECT application_id, drive_id, student_id, application_date, status, interview_feedback " +
            "FROM APPLICATION WHERE application_id = ?";

    private static final String SQL_FIND_BY_STUDENT =
            "SELECT application_id, drive_id, student_id, application_date, status, interview_feedback " +
            "FROM APPLICATION WHERE student_id = ? ORDER BY application_date DESC";

    private static final String SQL_FIND_BY_DRIVE =
            "SELECT application_id, drive_id, student_id, application_date, status, interview_feedback " +
            "FROM APPLICATION WHERE drive_id = ? ORDER BY application_date DESC";

    private static final String SQL_FIND_BY_DRIVE_AND_STUDENT =
            "SELECT application_id, drive_id, student_id, application_date, status, interview_feedback " +
            "FROM APPLICATION WHERE drive_id = ? AND student_id = ?";

    private static final String SQL_UPDATE_STATUS =
            "UPDATE APPLICATION SET status = ?, interview_feedback = ? WHERE application_id = ?";

    private static final String SQL_DELETE =
            "DELETE FROM APPLICATION WHERE application_id = ?";

    private final AuditLogDAO auditLogDAO = new AuditLogDAO();

    /**
     * Standard non-transactional apply method.
     */
    public Application apply(Application app) throws SQLException, DuplicateRecordException {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_INSERT, Statement.RETURN_GENERATED_KEYS)) {

            ps.setInt(1, app.getDriveId());
            ps.setInt(2, app.getStudentId());
            ps.setString(3, app.getStatus() != null ? app.getStatus().name() : ApplicationStatus.APPLIED.name());
            ps.setString(4, app.getInterviewFeedback());

            int affectedRows = ps.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Creating application failed, no rows affected.");
            }

            try (ResultSet generatedKeys = ps.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    app.setApplicationId(generatedKeys.getInt(1));
                }
            }
            return app;
        } catch (SQLException e) {
            if ("23000".equals(e.getSQLState()) || e.getErrorCode() == 1062) {
                throw new DuplicateRecordException("Student has already applied to this placement drive: " + e.getMessage(), e);
            }
            throw e;
        }
    }

    /**
     * ATOMIC TRANSACTION: Submits a placement APPLICATION AND creates an AUDIT_LOG entry
     * on the same connection.
     *
     * @param app         the application to submit.
     * @param performedBy the user/service executing the action.
     * @return the persisted Application with populated ID.
     */
    public Application applyWithAudit(Application app, String performedBy) throws SQLException, DuplicateRecordException {
        return applyWithAudit(app, performedBy, false);
    }

    /**
     * ATOMIC TRANSACTION with optional simulated failure to test ROLLBACK behavior.
     *
     * @param app             the application to submit.
     * @param performedBy     the user executing the application.
     * @param simulateFailure if true, throws an exception after application insert but before commit.
     */
    public Application applyWithAudit(Application app, String performedBy, boolean simulateFailure)
            throws SQLException, DuplicateRecordException {

        if (app == null) {
            throw new IllegalArgumentException("Application entity cannot be null.");
        }

        Connection conn = null;
        try {
            conn = DBConnection.getConnection();

            /*
             * VIVA POINT: Disabling auto-commit begins an explicit atomic transaction.
             * None of the following statements are persisted until commit() is invoked.
             */
            conn.setAutoCommit(false);

            // Step 1: Insert Application
            try (PreparedStatement ps = conn.prepareStatement(SQL_INSERT, Statement.RETURN_GENERATED_KEYS)) {
                ps.setInt(1, app.getDriveId());
                ps.setInt(2, app.getStudentId());
                ps.setString(3, app.getStatus() != null ? app.getStatus().name() : ApplicationStatus.APPLIED.name());
                ps.setString(4, app.getInterviewFeedback());

                int affectedRows = ps.executeUpdate();
                if (affectedRows == 0) {
                    throw new SQLException("Creating application failed, zero rows inserted.");
                }

                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        app.setApplicationId(rs.getInt(1));
                    }
                }
            }

            // Step 2: Insert Audit Log on the SAME connection
            AuditLog auditEntry = new AuditLog();
            try {
                auditEntry.setEntityName("APPLICATION");
                auditEntry.setEntityId(app.getApplicationId());
                auditEntry.setActionType(ActionType.INSERT);
                auditEntry.setPerformedBy(performedBy != null ? performedBy : "STUDENT_PORTAL");
                auditEntry.setDetails("Student #" + app.getStudentId() + " applied to Placement Drive #" + app.getDriveId());
            } catch (InvalidDataException e) {
                throw new SQLException("Audit log payload validation failed: " + e.getMessage(), e);
            }

            auditLogDAO.insert(conn, auditEntry);

            // Simulated failure trigger for testing transaction rollback
            if (simulateFailure) {
                throw new SQLException("[TEST] Simulated database failure occurred prior to commit. Triggering rollback!");
            }

            /*
             * VIVA POINT: commit() persists both the application and audit log atomically.
             */
            conn.commit();
            return app;

        } catch (SQLException e) {
            /*
             * VIVA POINT: rollback() undoes all changes made during this transaction.
             * Neither the application record nor the audit log record will remain in MySQL.
             */
            if (conn != null) {
                try {
                    conn.rollback();
                    System.out.println("[ApplicationDAO] Transaction rolled back successfully due to error: " + e.getMessage());
                } catch (SQLException ex) {
                    System.err.println("[ApplicationDAO] Critical: Rollback failed: " + ex.getMessage());
                }
            }
            if ("23000".equals(e.getSQLState()) || e.getErrorCode() == 1062) {
                throw new DuplicateRecordException("Student has already applied to this placement drive: " + e.getMessage(), e);
            }
            throw e;
        } finally {
            /*
             * VIVA POINT: Always reset autoCommit back to true and safely close the connection.
             */
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException ex) {
                    System.err.println("[ApplicationDAO] Error resetting auto-commit or closing connection: " + ex.getMessage());
                }
            }
        }
    }

    /**
     * Finds an application by ID.
     */
    public Optional<Application> findById(int applicationId) throws SQLException, InvalidDataException {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_FIND_BY_ID)) {

            ps.setInt(1, applicationId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToApplication(rs));
                }
            }
        }
        return Optional.empty();
    }

    /**
     * Finds applications submitted by a specific student.
     */
    public List<Application> findByStudentId(int studentId) throws SQLException, InvalidDataException {
        List<Application> list = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_FIND_BY_STUDENT)) {

            ps.setInt(1, studentId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToApplication(rs));
                }
            }
        }
        return list;
    }

    /**
     * Finds applications for a specific placement drive.
     */
    public List<Application> findByDriveId(int driveId) throws SQLException, InvalidDataException {
        List<Application> list = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_FIND_BY_DRIVE)) {

            ps.setInt(1, driveId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToApplication(rs));
                }
            }
        }
        return list;
    }

    /**
     * Finds a specific application by drive ID and student ID.
     */
    public Optional<Application> findByDriveAndStudent(int driveId, int studentId) throws SQLException, InvalidDataException {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_FIND_BY_DRIVE_AND_STUDENT)) {

            ps.setInt(1, driveId);
            ps.setInt(2, studentId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToApplication(rs));
                }
            }
        }
        return Optional.empty();
    }

    /**
     * Updates an application's review/interview status and feedback.
     */
    public boolean updateStatus(int applicationId, ApplicationStatus status, String feedback)
            throws SQLException, RecordNotFoundException {

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_UPDATE_STATUS)) {

            ps.setString(1, status != null ? status.name() : ApplicationStatus.APPLIED.name());
            ps.setString(2, feedback);
            ps.setInt(3, applicationId);

            int affectedRows = ps.executeUpdate();
            if (affectedRows == 0) {
                throw new RecordNotFoundException("Application with ID " + applicationId + " not found for status update.");
            }
            return true;
        }
    }

    /**
     * Permanently deletes an application by ID.
     */
    public boolean delete(int applicationId) throws SQLException, RecordNotFoundException {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_DELETE)) {

            ps.setInt(1, applicationId);
            int affectedRows = ps.executeUpdate();
            if (affectedRows == 0) {
                throw new RecordNotFoundException("Application with ID " + applicationId + " not found for deletion.");
            }
            return true;
        }
    }

    private Application mapResultSetToApplication(ResultSet rs) throws SQLException, InvalidDataException {
        Timestamp ts = rs.getTimestamp("application_date");
        return new Application(
                rs.getInt("application_id"),
                rs.getInt("drive_id"),
                rs.getInt("student_id"),
                ts != null ? ts.toLocalDateTime() : null,
                ApplicationStatus.fromString(rs.getString("status")),
                rs.getString("interview_feedback")
        );
    }
}
