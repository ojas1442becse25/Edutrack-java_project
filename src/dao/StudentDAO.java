package dao;

import database.DBConnection;
import exception.DuplicateRecordException;
import exception.InvalidDataException;
import exception.RecordNotFoundException;
import model.Student;
import model.Student.StudentStatus;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class StudentDAO {

    private static final String SQL_INSERT =
            "INSERT INTO STUDENT (roll_number, first_name, last_name, email, phone, department, semester, cgpa, enrollment_year, status) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

    private static final String SQL_FIND_BY_ID =
            "SELECT student_id, roll_number, first_name, last_name, email, phone, department, semester, cgpa, enrollment_year, status, created_at " +
            "FROM STUDENT WHERE student_id = ?";

    private static final String SQL_FIND_BY_ROLL =
            "SELECT student_id, roll_number, first_name, last_name, email, phone, department, semester, cgpa, enrollment_year, status, created_at " +
            "FROM STUDENT WHERE roll_number = ?";

    private static final String SQL_FIND_BY_EMAIL =
            "SELECT student_id, roll_number, first_name, last_name, email, phone, department, semester, cgpa, enrollment_year, status, created_at " +
            "FROM STUDENT WHERE email = ?";

    private static final String SQL_FIND_ALL =
            "SELECT student_id, roll_number, first_name, last_name, email, phone, department, semester, cgpa, enrollment_year, status, created_at " +
            "FROM STUDENT ORDER BY student_id ASC";

    private static final String SQL_FIND_BY_DEPT =
            "SELECT student_id, roll_number, first_name, last_name, email, phone, department, semester, cgpa, enrollment_year, status, created_at " +
            "FROM STUDENT WHERE department = ? ORDER BY roll_number ASC";

    private static final String SQL_FIND_BY_MIN_CGPA =
            "SELECT student_id, roll_number, first_name, last_name, email, phone, department, semester, cgpa, enrollment_year, status, created_at " +
            "FROM STUDENT WHERE cgpa >= ? ORDER BY cgpa DESC";

    private static final String SQL_UPDATE =
            "UPDATE STUDENT SET roll_number = ?, first_name = ?, last_name = ?, email = ?, phone = ?, " +
            "department = ?, semester = ?, cgpa = ?, enrollment_year = ?, status = ? WHERE student_id = ?";

    private static final String SQL_DELETE =
            "DELETE FROM STUDENT WHERE student_id = ?";

    private static final String SQL_SOFT_DELETE =
            "UPDATE STUDENT SET status = 'DROPPED' WHERE student_id = ?";

    public Student create(Student student) throws SQLException, DuplicateRecordException {
        if (student == null) {
            throw new IllegalArgumentException("Student entity cannot be null.");
        }

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_INSERT, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, student.getRollNumber());
            ps.setString(2, student.getFirstName());
            ps.setString(3, student.getLastName());
            ps.setString(4, student.getEmail());
            ps.setString(5, student.getPhone());
            ps.setString(6, student.getDepartment());
            ps.setInt(7, student.getSemester());
            ps.setDouble(8, student.getCgpa());
            ps.setInt(9, student.getEnrollmentYear());
            ps.setString(10, student.getStatus() != null ? student.getStatus().name() : StudentStatus.ACTIVE.name());

            int affectedRows = ps.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Creating student failed, no rows affected.");
            }

            try (ResultSet generatedKeys = ps.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    student.setStudentId(generatedKeys.getInt(1));
                } else {
                    throw new SQLException("Creating student failed, no ID obtained.");
                }
            }

            return student;
        } catch (SQLException e) {
            // check duplicate key
            if ("23000".equals(e.getSQLState()) || e.getErrorCode() == 1062) {
                throw new DuplicateRecordException("Duplicate student record: Roll number or email already exists. (" + e.getMessage() + ")", e);
            }
            throw e;
        }
    }

    public Optional<Student> findById(int studentId) throws SQLException, InvalidDataException {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_FIND_BY_ID)) {

            ps.setInt(1, studentId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToStudent(rs));
                }
            }
        }
        return Optional.empty();
    }

    public Optional<Student> findByRollNumber(String rollNumber) throws SQLException, InvalidDataException {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_FIND_BY_ROLL)) {

            ps.setString(1, rollNumber != null ? rollNumber.trim().toUpperCase() : "");
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToStudent(rs));
                }
            }
        }
        return Optional.empty();
    }

    public Optional<Student> findByEmail(String email) throws SQLException, InvalidDataException {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_FIND_BY_EMAIL)) {

            ps.setString(1, email != null ? email.trim().toLowerCase() : "");
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToStudent(rs));
                }
            }
        }
        return Optional.empty();
    }

    public List<Student> findAll() throws SQLException, InvalidDataException {
        List<Student> students = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_FIND_ALL);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                students.add(mapResultSetToStudent(rs));
            }
        }
        return students;
    }

    public List<Student> findByDepartment(String department) throws SQLException, InvalidDataException {
        List<Student> students = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_FIND_BY_DEPT)) {

            ps.setString(1, department != null ? department.trim() : "");
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    students.add(mapResultSetToStudent(rs));
                }
            }
        }
        return students;
    }

    public List<Student> findByMinCgpa(double minCgpa) throws SQLException, InvalidDataException {
        List<Student> students = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_FIND_BY_MIN_CGPA)) {

            ps.setDouble(1, minCgpa);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    students.add(mapResultSetToStudent(rs));
                }
            }
        }
        return students;
    }

    public boolean update(Student student) throws SQLException, RecordNotFoundException, DuplicateRecordException {
        if (student == null || student.getStudentId() == null) {
            throw new IllegalArgumentException("Student and Student ID cannot be null for update.");
        }

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_UPDATE)) {

            ps.setString(1, student.getRollNumber());
            ps.setString(2, student.getFirstName());
            ps.setString(3, student.getLastName());
            ps.setString(4, student.getEmail());
            ps.setString(5, student.getPhone());
            ps.setString(6, student.getDepartment());
            ps.setInt(7, student.getSemester());
            ps.setDouble(8, student.getCgpa());
            ps.setInt(9, student.getEnrollmentYear());
            ps.setString(10, student.getStatus() != null ? student.getStatus().name() : StudentStatus.ACTIVE.name());
            ps.setInt(11, student.getStudentId());

            int affectedRows = ps.executeUpdate();
            if (affectedRows == 0) {
                throw new RecordNotFoundException("Student with ID " + student.getStudentId() + " not found for update.");
            }
            return true;
        } catch (SQLException e) {
            // check duplicate key
            if ("23000".equals(e.getSQLState()) || e.getErrorCode() == 1062) {
                throw new DuplicateRecordException("Cannot update student: Roll number or email already in use. (" + e.getMessage() + ")", e);
            }
            throw e;
        }
    }

    public boolean delete(int studentId) throws SQLException, RecordNotFoundException {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_DELETE)) {

            ps.setInt(1, studentId);
            int affectedRows = ps.executeUpdate();
            if (affectedRows == 0) {
                throw new RecordNotFoundException("Student with ID " + studentId + " not found for deletion.");
            }
            return true;
        }
    }

    public boolean softDelete(int studentId) throws SQLException, RecordNotFoundException {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_SOFT_DELETE)) {

            ps.setInt(1, studentId);
            int affectedRows = ps.executeUpdate();
            if (affectedRows == 0) {
                throw new RecordNotFoundException("Student with ID " + studentId + " not found for soft-delete.");
            }
            return true;
        }
    }

    private Student mapResultSetToStudent(ResultSet rs) throws SQLException, InvalidDataException {
        Timestamp ts = rs.getTimestamp("created_at");
        return new Student(
                rs.getInt("student_id"),
                rs.getString("roll_number"),
                rs.getString("first_name"),
                rs.getString("last_name"),
                rs.getString("email"),
                rs.getString("phone"),
                rs.getString("department"),
                rs.getInt("semester"),
                rs.getDouble("cgpa"),
                rs.getInt("enrollment_year"),
                StudentStatus.fromString(rs.getString("status")),
                ts != null ? ts.toLocalDateTime() : null
        );
    }
}
