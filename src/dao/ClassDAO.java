package dao;

import database.DBConnection;
import exception.DuplicateRecordException;
import exception.InvalidDataException;
import exception.RecordNotFoundException;
import model.ClassEntity;
import model.ClassEntity.ClassStatus;

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
 * Data Access Object for the CLASS table.
 * Escapes `CLASS` with backticks in SQL queries to prevent keyword collision.
 */
public class ClassDAO {

    private static final String SQL_INSERT =
            "INSERT INTO `CLASS` (class_code, course_name, teacher_id, semester, academic_year, credits, max_capacity, status) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

    private static final String SQL_FIND_BY_ID =
            "SELECT class_id, class_code, course_name, teacher_id, semester, academic_year, credits, max_capacity, status, created_at " +
            "FROM `CLASS` WHERE class_id = ?";

    private static final String SQL_FIND_BY_CODE =
            "SELECT class_id, class_code, course_name, teacher_id, semester, academic_year, credits, max_capacity, status, created_at " +
            "FROM `CLASS` WHERE class_code = ?";

    private static final String SQL_FIND_ALL =
            "SELECT class_id, class_code, course_name, teacher_id, semester, academic_year, credits, max_capacity, status, created_at " +
            "FROM `CLASS` ORDER BY class_id ASC";

    private static final String SQL_FIND_BY_TEACHER =
            "SELECT class_id, class_code, course_name, teacher_id, semester, academic_year, credits, max_capacity, status, created_at " +
            "FROM `CLASS` WHERE teacher_id = ? ORDER BY class_code ASC";

    private static final String SQL_FIND_BY_SEMESTER =
            "SELECT class_id, class_code, course_name, teacher_id, semester, academic_year, credits, max_capacity, status, created_at " +
            "FROM `CLASS` WHERE semester = ? ORDER BY class_code ASC";

    private static final String SQL_UPDATE =
            "UPDATE `CLASS` SET class_code = ?, course_name = ?, teacher_id = ?, semester = ?, " +
            "academic_year = ?, credits = ?, max_capacity = ?, status = ? WHERE class_id = ?";

    private static final String SQL_DELETE =
            "DELETE FROM `CLASS` WHERE class_id = ?";

    /**
     * Inserts a new ClassEntity into the database and populates the generated class_id.
     */
    public ClassEntity create(ClassEntity classEntity) throws SQLException, DuplicateRecordException {
        if (classEntity == null) {
            throw new IllegalArgumentException("ClassEntity cannot be null.");
        }

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_INSERT, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, classEntity.getClassCode());
            ps.setString(2, classEntity.getCourseName());
            ps.setInt(3, classEntity.getTeacherId());
            ps.setInt(4, classEntity.getSemester());
            ps.setString(5, classEntity.getAcademicYear());
            ps.setInt(6, classEntity.getCredits());
            ps.setInt(7, classEntity.getMaxCapacity());
            ps.setString(8, classEntity.getStatus() != null ? classEntity.getStatus().name() : ClassStatus.SCHEDULED.name());

            int affectedRows = ps.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Creating class failed, no rows affected.");
            }

            try (ResultSet generatedKeys = ps.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    classEntity.setClassId(generatedKeys.getInt(1));
                } else {
                    throw new SQLException("Creating class failed, no ID obtained.");
                }
            }

            return classEntity;
        } catch (SQLException e) {
            if ("23000".equals(e.getSQLState()) || e.getErrorCode() == 1062) {
                throw new DuplicateRecordException("Duplicate class record: Class code already exists. (" + e.getMessage() + ")", e);
            }
            throw e;
        }
    }

    /**
     * Finds a class by primary key (class_id).
     */
    public Optional<ClassEntity> findById(int classId) throws SQLException, InvalidDataException {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_FIND_BY_ID)) {

            ps.setInt(1, classId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToClass(rs));
                }
            }
        }
        return Optional.empty();
    }

    /**
     * Finds a class by unique class code (e.g., 'CS301').
     */
    public Optional<ClassEntity> findByClassCode(String classCode) throws SQLException, InvalidDataException {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_FIND_BY_CODE)) {

            ps.setString(1, classCode != null ? classCode.trim().toUpperCase() : "");
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToClass(rs));
                }
            }
        }
        return Optional.empty();
    }

    /**
     * Retrieves all classes ordered by class_id.
     */
    public List<ClassEntity> findAll() throws SQLException, InvalidDataException {
        List<ClassEntity> classes = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_FIND_ALL);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                classes.add(mapResultSetToClass(rs));
            }
        }
        return classes;
    }

    /**
     * Retrieves all classes assigned to a specific teacher.
     */
    public List<ClassEntity> findClassesByTeacherId(int teacherId) throws SQLException, InvalidDataException {
        List<ClassEntity> classes = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_FIND_BY_TEACHER)) {

            ps.setInt(1, teacherId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    classes.add(mapResultSetToClass(rs));
                }
            }
        }
        return classes;
    }

    /**
     * Retrieves classes by semester.
     */
    public List<ClassEntity> findBySemester(int semester) throws SQLException, InvalidDataException {
        List<ClassEntity> classes = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_FIND_BY_SEMESTER)) {

            ps.setInt(1, semester);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    classes.add(mapResultSetToClass(rs));
                }
            }
        }
        return classes;
    }

    /**
     * Updates an existing class record.
     */
    public boolean update(ClassEntity classEntity) throws SQLException, RecordNotFoundException, DuplicateRecordException {
        if (classEntity == null || classEntity.getClassId() == null) {
            throw new IllegalArgumentException("ClassEntity and Class ID cannot be null for update.");
        }

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_UPDATE)) {

            ps.setString(1, classEntity.getClassCode());
            ps.setString(2, classEntity.getCourseName());
            ps.setInt(3, classEntity.getTeacherId());
            ps.setInt(4, classEntity.getSemester());
            ps.setString(5, classEntity.getAcademicYear());
            ps.setInt(6, classEntity.getCredits());
            ps.setInt(7, classEntity.getMaxCapacity());
            ps.setString(8, classEntity.getStatus() != null ? classEntity.getStatus().name() : ClassStatus.SCHEDULED.name());
            ps.setInt(9, classEntity.getClassId());

            int affectedRows = ps.executeUpdate();
            if (affectedRows == 0) {
                throw new RecordNotFoundException("Class with ID " + classEntity.getClassId() + " not found for update.");
            }
            return true;
        } catch (SQLException e) {
            if ("23000".equals(e.getSQLState()) || e.getErrorCode() == 1062) {
                throw new DuplicateRecordException("Cannot update class: Class code already in use. (" + e.getMessage() + ")", e);
            }
            throw e;
        }
    }

    /**
     * Permanently deletes a class record by class_id.
     */
    public boolean delete(int classId) throws SQLException, RecordNotFoundException {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_DELETE)) {

            ps.setInt(1, classId);
            int affectedRows = ps.executeUpdate();
            if (affectedRows == 0) {
                throw new RecordNotFoundException("Class with ID " + classId + " not found for deletion.");
            }
            return true;
        }
    }

    /**
     * Maps ResultSet row to ClassEntity domain model.
     */
    private ClassEntity mapResultSetToClass(ResultSet rs) throws SQLException, InvalidDataException {
        Timestamp ts = rs.getTimestamp("created_at");
        return new ClassEntity(
                rs.getInt("class_id"),
                rs.getString("class_code"),
                rs.getString("course_name"),
                rs.getInt("teacher_id"),
                rs.getInt("semester"),
                rs.getString("academic_year"),
                rs.getInt("credits"),
                rs.getInt("max_capacity"),
                ClassStatus.fromString(rs.getString("status")),
                ts != null ? ts.toLocalDateTime() : null
        );
    }
}
