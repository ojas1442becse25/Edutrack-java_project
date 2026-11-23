package model;

import exception.InvalidDataException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Domain model representing the CLASS_ENROLLMENT table.
 */
public class ClassEnrollment {

    public enum EnrollmentStatus {
        ENROLLED, COMPLETED, DROPPED;

        public static EnrollmentStatus fromString(String val) throws InvalidDataException {
            if (val == null || val.trim().isEmpty()) {
                return ENROLLED;
            }
            try {
                return EnrollmentStatus.valueOf(val.trim().toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new InvalidDataException("Invalid Enrollment status: " + val + ". Allowed: ENROLLED, COMPLETED, DROPPED");
            }
        }
    }

    private static final Set<String> VALID_GRADES = new HashSet<>(
            Arrays.asList("A+", "A", "B+", "B", "C+", "C", "D", "F", "I")
    );

    private Integer enrollmentId;
    private int classId;
    private int studentId;
    private LocalDate enrollmentDate;
    private double attendancePercentage;
    private String grade;
    private EnrollmentStatus status;
    private LocalDateTime createdAt;

    public ClassEnrollment() {
        this.enrollmentDate = LocalDate.now();
        this.attendancePercentage = 0.00;
        this.status = EnrollmentStatus.ENROLLED;
    }

    public ClassEnrollment(int classId, int studentId, LocalDate enrollmentDate,
                           double attendancePercentage, String grade, EnrollmentStatus status) throws InvalidDataException {
        setClassId(classId);
        setStudentId(studentId);
        setEnrollmentDate(enrollmentDate);
        setAttendancePercentage(attendancePercentage);
        setGrade(grade);
        setStatus(status);
    }

    public ClassEnrollment(Integer enrollmentId, int classId, int studentId, LocalDate enrollmentDate,
                           double attendancePercentage, String grade, EnrollmentStatus status,
                           LocalDateTime createdAt) throws InvalidDataException {
        this(classId, studentId, enrollmentDate, attendancePercentage, grade, status);
        this.enrollmentId = enrollmentId;
        this.createdAt = createdAt;
    }

    public Integer getEnrollmentId() {
        return enrollmentId;
    }

    public void setEnrollmentId(Integer enrollmentId) {
        this.enrollmentId = enrollmentId;
    }

    public int getClassId() {
        return classId;
    }

    public void setClassId(int classId) throws InvalidDataException {
        if (classId <= 0) {
            throw new InvalidDataException("Class ID must be positive.");
        }
        this.classId = classId;
    }

    public int getStudentId() {
        return studentId;
    }

    public void setStudentId(int studentId) throws InvalidDataException {
        if (studentId <= 0) {
            throw new InvalidDataException("Student ID must be positive.");
        }
        this.studentId = studentId;
    }

    public LocalDate getEnrollmentDate() {
        return enrollmentDate;
    }

    public void setEnrollmentDate(LocalDate enrollmentDate) {
        this.enrollmentDate = enrollmentDate != null ? enrollmentDate : LocalDate.now();
    }

    public double getAttendancePercentage() {
        return attendancePercentage;
    }

    public void setAttendancePercentage(double attendancePercentage) throws InvalidDataException {
        if (attendancePercentage < 0.00 || attendancePercentage > 100.00) {
            throw new InvalidDataException("Attendance percentage must be between 0.00 and 100.00.");
        }
        this.attendancePercentage = Math.round(attendancePercentage * 100.0) / 100.0;
    }

    public String getGrade() {
        return grade;
    }

    public void setGrade(String grade) throws InvalidDataException {
        if (grade == null || grade.trim().isEmpty()) {
            this.grade = null;
            return;
        }
        String normalized = grade.trim().toUpperCase();
        if (!VALID_GRADES.contains(normalized)) {
            throw new InvalidDataException("Invalid grade: " + grade + ". Allowed: A+, A, B+, B, C+, C, D, F, I");
        }
        this.grade = normalized;
    }

    public EnrollmentStatus getStatus() {
        return status;
    }

    public void setStatus(EnrollmentStatus status) {
        this.status = status != null ? status : EnrollmentStatus.ENROLLED;
    }

    public void setStatus(String statusStr) throws InvalidDataException {
        this.status = EnrollmentStatus.fromString(statusStr);
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ClassEnrollment that = (ClassEnrollment) o;
        return classId == that.classId && studentId == that.studentId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(classId, studentId);
    }

    @Override
    public String toString() {
        return String.format("ClassEnrollment[ID=%d, ClassID=%d, StudentID=%d, Attendance=%.2f%%, Grade='%s', Status=%s]",
                enrollmentId, classId, studentId, attendancePercentage, grade != null ? grade : "N/A", status);
    }
}
