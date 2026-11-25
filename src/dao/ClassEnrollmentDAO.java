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
            // check duplicate key
            if ("23000".equals(e.getSQLState()) || e.getErrorCode() == 1062) {
                throw new DuplicateRecordException("Student is already enrolled in this class: " + e.getMessage(), e);
            }
            throw e;
        }
    }

    public ClassEnrollment enrollStudentWithAudit(ClassEnrollment enrollment, String performedBy)
            throws SQLException, DuplicateRecordException {
        return enrollStudentWithAudit(enrollment, performedBy, false);
    }

    public ClassEnrollment enrollStudentWithAudit(ClassEnrollment enrollment, String performedBy, boolean simulateFailure)
            throws SQLException, DuplicateRecordException {

        if (enrollment == null) {
            throw new IllegalArgumentException("ClassEnrollment entity cannot be null.");
        }

        Connection conn = null;
        try {
            conn = DBConnection.getConnection();

            // begin transaction
            conn.setAutoCommit(false);

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

            if (simulateFailure) {
                throw new SQLException("[TEST] Simulated database failure during enrollment transaction. Triggering rollback!");
            }

            conn.commit();
            return enrollment;

        } catch (SQLException e) {
            // rollback on error
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    System.err.println("Rollback failed: " + ex.getMessage());
                }
            }
            // check duplicate key
            if ("23000".equals(e.getSQLState()) || e.getErrorCode() == 1062) {
                throw new DuplicateRecordException("Student is already enrolled in this class: " + e.getMessage(), e);
            }
            throw e;
        } finally {
            // reset autocommit
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException ex) {
                    System.err.println("Error closing connection: " + ex.getMessage());
                }
            }
        }
    }

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
