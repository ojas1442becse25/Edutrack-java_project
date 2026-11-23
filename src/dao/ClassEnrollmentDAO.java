package dao;

import database.DBConnection;
import exception.DuplicateRecordException;
import exception.InvalidDataException;
import exception.RecordNotFoundException;
import model.AuditLog;
import model.AuditLog.ActionType;
import model.ClassEnrollment;
import model.ClassEnrollment.EnrollmentStatus;

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
 * ============================================================================
 * VIVA EVALUATION REFERENCE - ATOMIC JDBC TRANSACTION MANAGEMENT:
 * ============================================================================
 * 1. TRANSACTION DEMONSTRATION (ENROLLMENT + AUDIT_LOG):
 *    - In an educational management system, enrolling a student into a class is a
 *      state-changing action that requires an immutable audit trail.
 *    - If the student enrollment succeeds but the audit log fails (e.g. out of memory,
 *      constraint failure), the system would enter an unaudited inconsistent state.
 *    - By enclosing both statements inside `conn.setAutoCommit(false)` and passing
 *      the active `conn` to both operations, atomicity is guaranteed:
 *      * Success -> `conn.commit()`
 *      * Failure -> `conn.rollback()`
 * ============================================================================
 */
public class ClassEnrollmentDAO {

    private static final String SQL_INSERT =
            "INSERT INTO CLASS_ENROLLMENT (class_id, student_id, enrollment_date, attendance_percentage, grade, status) " +
            "VALUES (?, ?, ?, ?, ?, ?)";

    private static final String SQL_FIND_BY_ID =
            "SELECT enrollment_id, class_id, student_id, enrollment_date, attendance_percentage, grade, status, created_at " +
            "FROM CLASS_ENROLLMENT WHERE enrollment_id = ?";

    private static final String SQL_FIND_BY_CLASS_AND_STUDENT =
            "SELECT enrollment_id, class_id, student_id, enrollment_date, attendance_percentage, grade, status, created_at " +
            "FROM CLASS_ENROLLMENT WHERE class_id = ? AND student_id = ?";

    private static final String SQL_FIND_BY_CLASS =
            "SELECT enrollment_id, class_id, student_id, enrollment_date, attendance_percentage, grade, status, created_at " +
            "FROM CLASS_ENROLLMENT WHERE class_id = ? ORDER BY student_id ASC";

    private static final String SQL_FIND_BY_STUDENT =
            "SELECT enrollment_id, class_id, student_id, enrollment_date, attendance_percentage, grade, status, created_at " +
            "FROM CLASS_ENROLLMENT WHERE student_id = ? ORDER BY class_id ASC";

    private static final String SQL_UPDATE_ATTENDANCE_GRADE =
            "UPDATE CLASS_ENROLLMENT SET attendance_percentage = ?, grade = ? WHERE enrollment_id = ?";

    private static final String SQL_UPDATE_STATUS =
            "UPDATE CLASS_ENROLLMENT SET status = ? WHERE enrollment_id = ?";

    private static final String SQL_DELETE =
            "DELETE FROM CLASS_ENROLLMENT WHERE enrollment_id = ?";

    private final AuditLogDAO auditLogDAO = new AuditLogDAO();

    /**
     * Standard non-transactional enroll method.
     */
    public ClassEnrollment enroll(ClassEnrollment enrollment) throws SQLException, DuplicateRecordException {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_INSERT, Statement.RETURN_GENERATED_KEYS)) {

            ps.setInt(1, enrollment.getClassId());
            ps.setInt(2, enrollment.getStudentId());
            ps.setDate(3, Date.valueOf(enrollment.getEnrollmentDate()));
            ps.setDouble(4, enrollment.getAttendancePercentage());
            ps.setString(5, enrollment.getGrade());
            ps.setString(6, enrollment.getStatus() != null ? enrollment.getStatus().name() : EnrollmentStatus.ENROLLED.name());

            int affectedRows = ps.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Creating enrollment failed, no rows affected.");
            }

            try (ResultSet generatedKeys = ps.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    enrollment.setEnrollmentId(generatedKeys.getInt(1));
                }
            }
            return enrollment;
        } catch (SQLException e) {
            if ("23000".equals(e.getSQLState()) || e.getErrorCode() == 1062) {
                throw new DuplicateRecordException("Student is already enrolled in this class: " + e.getMessage(), e);
            }
            throw e;
        }
    }

    /**
     * ATOMIC TRANSACTION: Enrolls a student into a class AND writes an AUDIT_LOG entry
     * using the same Connection.
     */
    public ClassEnrollment enrollStudentWithAudit(ClassEnrollment enrollment, String performedBy)
            throws SQLException, DuplicateRecordException {
        return enrollStudentWithAudit(enrollment, performedBy, false);
    }

    /**
     * ATOMIC TRANSACTION with optional simulated failure to demonstrate and verify ROLLBACK.
     *
     * @param enrollment      the enrollment entity.
     * @param performedBy     the user/officer performing enrollment.
     * @param simulateFailure if true, throws an exception prior to commit to trigger rollback.
     */
    public ClassEnrollment enrollStudentWithAudit(ClassEnrollment enrollment, String performedBy, boolean simulateFailure)
            throws SQLException, DuplicateRecordException {

        if (enrollment == null) {
            throw new IllegalArgumentException("ClassEnrollment entity cannot be null.");
        }

        Connection conn = null;
        try {
            conn = DBConnection.getConnection();

            /*
             * VIVA POINT: Step 1 - Disable auto-commit to begin multi-statement transaction boundary.
             */
            conn.setAutoCommit(false);

            // Step 2: Insert into CLASS_ENROLLMENT
            try (PreparedStatement ps = conn.prepareStatement(SQL_INSERT, Statement.RETURN_GENERATED_KEYS)) {
                ps.setInt(1, enrollment.getClassId());
                ps.setInt(2, enrollment.getStudentId());
                ps.setDate(3, Date.valueOf(enrollment.getEnrollmentDate()));
                ps.setDouble(4, enrollment.getAttendancePercentage());
                ps.setString(5, enrollment.getGrade());
                ps.setString(6, enrollment.getStatus() != null ? enrollment.getStatus().name() : EnrollmentStatus.ENROLLED.name());

                int affectedRows = ps.executeUpdate();
                if (affectedRows == 0) {
                    throw new SQLException("Enrolling student failed, zero rows inserted.");
                }

                try (ResultSet generatedKeys = ps.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        enrollment.setEnrollmentId(generatedKeys.getInt(1));
                    }
                }
            }

            // Step 3: Insert into AUDIT_LOG on the EXACT SAME connection
            AuditLog auditLog = new AuditLog();
            try {
                auditLog.setEntityName("CLASS_ENROLLMENT");
                auditLog.setEntityId(enrollment.getEnrollmentId());
                auditLog.setActionType(ActionType.INSERT);
                auditLog.setPerformedBy(performedBy != null ? performedBy : "ACADEMIC_ADMIN");
                auditLog.setDetails("Enrolled Student #" + enrollment.getStudentId() + " into Class #" + enrollment.getClassId());
            } catch (InvalidDataException e) {
                throw new SQLException("Failed to create audit log payload: " + e.getMessage(), e);
            }

            auditLogDAO.insert(conn, auditLog);

            // Simulated failure trigger for testing transaction rollback
            if (simulateFailure) {
                throw new SQLException("[TEST] Simulated database failure during enrollment transaction. Triggering rollback!");
            }

            /*
             * VIVA POINT: Step 4 - Commit atomically persisted changes.
             */
            conn.commit();
            return enrollment;

        } catch (SQLException e) {
            /*
             * VIVA POINT: Step 5 - Rollback on error leaves database in clean initial state.
             */
            if (conn != null) {
                try {
                    conn.rollback();
                    System.out.println("[ClassEnrollmentDAO] Transaction rolled back successfully: " + e.getMessage());
                } catch (SQLException ex) {
                    System.err.println("[ClassEnrollmentDAO] Critical: Rollback failed: " + ex.getMessage());
                }
            }
            if ("23000".equals(e.getSQLState()) || e.getErrorCode() == 1062) {
                throw new DuplicateRecordException("Student is already enrolled in this class: " + e.getMessage(), e);
            }
            throw e;
        } finally {
            /*
             * VIVA POINT: Step 6 - Restore auto-commit state and close connection.
             */
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException ex) {
                    System.err.println("[ClassEnrollmentDAO] Error restoring auto-commit or closing connection: " + ex.getMessage());
                }
            }
        }
    }

    /**
     * Finds enrollment by ID.
     */
    public Optional<ClassEnrollment> findById(int enrollmentId) throws SQLException, InvalidDataException {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_FIND_BY_ID)) {

            ps.setInt(1, enrollmentId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToEnrollment(rs));
                }
            }
        }
        return Optional.empty();
    }

    /**
     * Finds enrollment by class ID and student ID.
     */
    public Optional<ClassEnrollment> findByClassAndStudent(int classId, int studentId) throws SQLException, InvalidDataException {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_FIND_BY_CLASS_AND_STUDENT)) {

            ps.setInt(1, classId);
            ps.setInt(2, studentId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToEnrollment(rs));
                }
            }
        }
        return Optional.empty();
    }

    /**
     * Retrieves all student enrollments for a given class.
     */
    public List<ClassEnrollment> findEnrollmentsByClassId(int classId) throws SQLException, InvalidDataException {
        List<ClassEnrollment> list = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_FIND_BY_CLASS)) {

            ps.setInt(1, classId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToEnrollment(rs));
                }
            }
        }
        return list;
    }

    /**
     * Retrieves all class enrollments for a given student.
     */
    public List<ClassEnrollment> findEnrollmentsByStudentId(int studentId) throws SQLException, InvalidDataException {
        List<ClassEnrollment> list = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_FIND_BY_STUDENT)) {

            ps.setInt(1, studentId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToEnrollment(rs));
                }
            }
        }
        return list;
    }

    /**
     * Updates attendance percentage and final grade for an enrollment.
     */
    public boolean updateAttendanceAndGrade(int enrollmentId, double attendance, String grade)
            throws SQLException, RecordNotFoundException {

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_UPDATE_ATTENDANCE_GRADE)) {

            ps.setDouble(1, attendance);
            ps.setString(2, grade);
            ps.setInt(3, enrollmentId);

            int affectedRows = ps.executeUpdate();
            if (affectedRows == 0) {
                throw new RecordNotFoundException("Enrollment with ID " + enrollmentId + " not found for attendance/grade update.");
            }
            return true;
        }
    }

    /**
     * Updates enrollment status (ENROLLED, COMPLETED, DROPPED).
     */
    public boolean updateStatus(int enrollmentId, EnrollmentStatus status) throws SQLException, RecordNotFoundException {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_UPDATE_STATUS)) {

            ps.setString(1, status != null ? status.name() : EnrollmentStatus.ENROLLED.name());
            ps.setInt(2, enrollmentId);

            int affectedRows = ps.executeUpdate();
            if (affectedRows == 0) {
                throw new RecordNotFoundException("Enrollment with ID " + enrollmentId + " not found for status update.");
            }
            return true;
        }
    }

    /**
     * Permanently deletes an enrollment by ID.
     */
    public boolean delete(int enrollmentId) throws SQLException, RecordNotFoundException {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_DELETE)) {

            ps.setInt(1, enrollmentId);
            int affectedRows = ps.executeUpdate();
            if (affectedRows == 0) {
                throw new RecordNotFoundException("Enrollment with ID " + enrollmentId + " not found for deletion.");
            }
            return true;
        }
    }

    private ClassEnrollment mapResultSetToEnrollment(ResultSet rs) throws SQLException, InvalidDataException {
        Date eDate = rs.getDate("enrollment_date");
        Timestamp ts = rs.getTimestamp("created_at");

        return new ClassEnrollment(
                rs.getInt("enrollment_id"),
                rs.getInt("class_id"),
                rs.getInt("student_id"),
                eDate != null ? eDate.toLocalDate() : null,
                rs.getDouble("attendance_percentage"),
                rs.getString("grade"),
                EnrollmentStatus.fromString(rs.getString("status")),
                ts != null ? ts.toLocalDateTime() : null
        );
    }
}
