package model;

import exception.InvalidDataException;
import java.time.LocalDateTime;
import java.util.Objects;

public class ClassEntity {

    public enum ClassStatus {
        SCHEDULED, ONGOING, COMPLETED, CANCELLED;

        public static ClassStatus fromString(String val) throws InvalidDataException {
            if (val == null || val.trim().isEmpty()) {
                return SCHEDULED;
            }
            try {
                return ClassStatus.valueOf(val.trim().toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new InvalidDataException("Invalid Class status: " + val + ". Allowed: SCHEDULED, ONGOING, COMPLETED, CANCELLED");
            }
        }
    }

    private Integer classId;
    private String classCode;
    private String courseName;
    private int teacherId;
    private int semester;
    private String academicYear;
    private int credits;
    private int maxCapacity;
    private ClassStatus status;
    private LocalDateTime createdAt;

    public ClassEntity() {
        this.credits = 3;
        this.maxCapacity = 60;
        this.status = ClassStatus.SCHEDULED;
    }

    public ClassEntity(String classCode, String courseName, int teacherId, int semester,
                       String academicYear, int credits, int maxCapacity, ClassStatus status) throws InvalidDataException {
        setClassCode(classCode);
        setCourseName(courseName);
        setTeacherId(teacherId);
        setSemester(semester);
        setAcademicYear(academicYear);
        setCredits(credits);
        setMaxCapacity(maxCapacity);
        setStatus(status);
    }

    public ClassEntity(Integer classId, String classCode, String courseName, int teacherId,
                       int semester, String academicYear, int credits, int maxCapacity,
                       ClassStatus status, LocalDateTime createdAt) throws InvalidDataException {
        this(classCode, courseName, teacherId, semester, academicYear, credits, maxCapacity, status);
        this.classId = classId;
        this.createdAt = createdAt;
    }

    public Integer getClassId() {
        return classId;
    }

    public void setClassId(Integer classId) {
        this.classId = classId;
    }

    public String getClassCode() {
        return classCode;
    }

    public void setClassCode(String classCode) throws InvalidDataException {
        if (classCode == null || classCode.trim().isEmpty()) {
            throw new InvalidDataException("Class code cannot be empty.");
        }
        if (classCode.trim().length() > 30) {
            throw new InvalidDataException("Class code cannot exceed 30 characters.");
        }
        this.classCode = classCode.trim().toUpperCase();
    }

    public String getCourseName() {
        return courseName;
    }

    public void setCourseName(String courseName) throws InvalidDataException {
        if (courseName == null || courseName.trim().isEmpty()) {
            throw new InvalidDataException("Course name cannot be empty.");
        }
        if (courseName.trim().length() > 100) {
            throw new InvalidDataException("Course name cannot exceed 100 characters.");
        }
        this.courseName = courseName.trim();
    }

    public int getTeacherId() {
        return teacherId;
    }

    public void setTeacherId(int teacherId) throws InvalidDataException {
        if (teacherId <= 0) {
            throw new InvalidDataException("Teacher ID must be a positive integer.");
        }
        this.teacherId = teacherId;
    }

    public int getSemester() {
        return semester;
    }

    public void setSemester(int semester) throws InvalidDataException {
        if (semester < 1 || semester > 8) {
            throw new InvalidDataException("Invalid semester: " + semester + ". Semester must be between 1 and 8.");
        }
        this.semester = semester;
    }

    public String getAcademicYear() {
        return academicYear;
    }

    public void setAcademicYear(String academicYear) throws InvalidDataException {
        if (academicYear == null || academicYear.trim().isEmpty()) {
            throw new InvalidDataException("Academic year cannot be empty (e.g., '2025-2026').");
        }
        if (academicYear.trim().length() > 20) {
            throw new InvalidDataException("Academic year cannot exceed 20 characters.");
        }
        this.academicYear = academicYear.trim();
    }

    public int getCredits() {
        return credits;
    }

    public void setCredits(int credits) throws InvalidDataException {
        if (credits < 1 || credits > 10) {
            throw new InvalidDataException("Credits must be between 1 and 10.");
        }
        this.credits = credits;
    }

    public int getMaxCapacity() {
        return maxCapacity;
    }

    public void setMaxCapacity(int maxCapacity) throws InvalidDataException {
        if (maxCapacity <= 0) {
            throw new InvalidDataException("Max capacity must be greater than zero.");
        }
        this.maxCapacity = maxCapacity;
    }

    public ClassStatus getStatus() {
        return status;
    }

    public void setStatus(ClassStatus status) {
        this.status = status != null ? status : ClassStatus.SCHEDULED;
    }

    public void setStatus(String statusStr) throws InvalidDataException {
        this.status = ClassStatus.fromString(statusStr);
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
        ClassEntity that = (ClassEntity) o;
        return Objects.equals(classId, that.classId) || Objects.equals(classCode, that.classCode);
    }

    @Override
    public int hashCode() {
        return Objects.hash(classId, classCode);
    }

    @Override
    public String toString() {
        return String.format("ClassEntity[ID=%d, Code='%s', Course='%s', TeacherID=%d, Sem=%d, Year='%s', Status=%s]",
                classId, classCode, courseName, teacherId, semester, academicYear, status);
    }
}
