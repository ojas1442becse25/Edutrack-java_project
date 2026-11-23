package dao;

import database.DBConnection;
import exception.DuplicateRecordException;
import exception.InvalidDataException;
import exception.RecordNotFoundException;
import model.Teacher;
import model.Teacher.TeacherStatus;

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
 * Data Access Object for the TEACHER table.
 * Encapsulates full CRUD operations and specialized departmental queries.
 */
public class TeacherDAO {

    private static final String SQL_INSERT =
            "INSERT INTO TEACHER (first_name, last_name, email, phone, department, designation, hire_date, status) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

    private static final String SQL_FIND_BY_ID =
            "SELECT teacher_id, first_name, last_name, email, phone, department, designation, hire_date, status, created_at " +
            "FROM TEACHER WHERE teacher_id = ?";

    private static final String SQL_FIND_BY_EMAIL =
            "SELECT teacher_id, first_name, last_name, email, phone, department, designation, hire_date, status, created_at " +
            "FROM TEACHER WHERE email = ?";

    private static final String SQL_FIND_ALL =
            "SELECT teacher_id, first_name, last_name, email, phone, department, designation, hire_date, status, created_at " +
            "FROM TEACHER ORDER BY teacher_id ASC";

    private static final String SQL_FIND_BY_DEPT =
            "SELECT teacher_id, first_name, last_name, email, phone, department, designation, hire_date, status, created_at " +
            "FROM TEACHER WHERE department = ? ORDER BY first_name ASC";

    private static final String SQL_UPDATE =
            "UPDATE TEACHER SET first_name = ?, last_name = ?, email = ?, phone = ?, department = ?, " +
            "designation = ?, hire_date = ?, status = ? WHERE teacher_id = ?";

    private static final String SQL_DELETE =
            "DELETE FROM TEACHER WHERE teacher_id = ?";

    /**
     * Inserts a new Teacher into the database and populates the auto-increment teacher_id.
     */
    public Teacher create(Teacher teacher) throws SQLException, DuplicateRecordException {
        if (teacher == null) {
            throw new IllegalArgumentException("Teacher entity cannot be null.");
        }

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_INSERT, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, teacher.getFirstName());
            ps.setString(2, teacher.getLastName());
            ps.setString(3, teacher.getEmail());
            ps.setString(4, teacher.getPhone());
            ps.setString(5, teacher.getDepartment());
            ps.setString(6, teacher.getDesignation());
            ps.setDate(7, teacher.getHireDate() != null ? Date.valueOf(teacher.getHireDate()) : Date.valueOf(java.time.LocalDate.now()));
            ps.setString(8, teacher.getStatus() != null ? teacher.getStatus().name() : TeacherStatus.ACTIVE.name());

            int affectedRows = ps.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Creating teacher failed, no rows affected.");
            }

            try (ResultSet generatedKeys = ps.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    teacher.setTeacherId(generatedKeys.getInt(1));
                } else {
                    throw new SQLException("Creating teacher failed, no ID obtained.");
                }
            }

            return teacher;
        } catch (SQLException e) {
            if ("23000".equals(e.getSQLState()) || e.getErrorCode() == 1062) {
                throw new DuplicateRecordException("Duplicate teacher record: Email or phone already in use. (" + e.getMessage() + ")", e);
            }
            throw e;
        }
    }

    /**
     * Finds a teacher by primary key (teacher_id).
     */
    public Optional<Teacher> findById(int teacherId) throws SQLException, InvalidDataException {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_FIND_BY_ID)) {

            ps.setInt(1, teacherId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToTeacher(rs));
                }
            }
        }
        return Optional.empty();
    }

    /**
     * Finds a teacher by unique email.
     */
    public Optional<Teacher> findByEmail(String email) throws SQLException, InvalidDataException {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_FIND_BY_EMAIL)) {

            ps.setString(1, email != null ? email.trim().toLowerCase() : "");
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToTeacher(rs));
                }
            }
        }
        return Optional.empty();
    }

    /**
     * Retrieves all teachers ordered by teacher_id.
     */
    public List<Teacher> findAll() throws SQLException, InvalidDataException {
        List<Teacher> teachers = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_FIND_ALL);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                teachers.add(mapResultSetToTeacher(rs));
            }
        }
        return teachers;
    }

    /**
     * Retrieves teachers belonging to a specific department.
     */
    public List<Teacher> findByDepartment(String department) throws SQLException, InvalidDataException {
        List<Teacher> teachers = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_FIND_BY_DEPT)) {

            ps.setString(1, department != null ? department.trim() : "");
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    teachers.add(mapResultSetToTeacher(rs));
                }
            }
        }
        return teachers;
    }

    /**
     * Updates an existing teacher record.
     */
    public boolean update(Teacher teacher) throws SQLException, RecordNotFoundException, DuplicateRecordException {
        if (teacher == null || teacher.getTeacherId() == null) {
            throw new IllegalArgumentException("Teacher and Teacher ID cannot be null for update.");
        }

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_UPDATE)) {

            ps.setString(1, teacher.getFirstName());
            ps.setString(2, teacher.getLastName());
            ps.setString(3, teacher.getEmail());
            ps.setString(4, teacher.getPhone());
            ps.setString(5, teacher.getDepartment());
            ps.setString(6, teacher.getDesignation());
            ps.setDate(7, Date.valueOf(teacher.getHireDate()));
            ps.setString(8, teacher.getStatus() != null ? teacher.getStatus().name() : TeacherStatus.ACTIVE.name());
            ps.setInt(9, teacher.getTeacherId());

            int affectedRows = ps.executeUpdate();
            if (affectedRows == 0) {
                throw new RecordNotFoundException("Teacher with ID " + teacher.getTeacherId() + " not found for update.");
            }
            return true;
        } catch (SQLException e) {
            if ("23000".equals(e.getSQLState()) || e.getErrorCode() == 1062) {
                throw new DuplicateRecordException("Cannot update teacher: Email or phone already in use. (" + e.getMessage() + ")", e);
            }
            throw e;
        }
    }

    /**
     * Permanently deletes a teacher record by teacher_id.
     */
    public boolean delete(int teacherId) throws SQLException, RecordNotFoundException {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_DELETE)) {

            ps.setInt(1, teacherId);
            int affectedRows = ps.executeUpdate();
            if (affectedRows == 0) {
                throw new RecordNotFoundException("Teacher with ID " + teacherId + " not found for deletion.");
            }
            return true;
        }
    }

    /**
     * Maps ResultSet row to Teacher domain model.
     */
    private Teacher mapResultSetToTeacher(ResultSet rs) throws SQLException, InvalidDataException {
        Date hireDate = rs.getDate("hire_date");
        Timestamp ts = rs.getTimestamp("created_at");
        return new Teacher(
                rs.getInt("teacher_id"),
                rs.getString("first_name"),
                rs.getString("last_name"),
                rs.getString("email"),
                rs.getString("phone"),
                rs.getString("department"),
                rs.getString("designation"),
                hireDate != null ? hireDate.toLocalDate() : null,
                TeacherStatus.fromString(rs.getString("status")),
                ts != null ? ts.toLocalDateTime() : null
        );
    }
}
