package model;

import exception.InvalidDataException;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Domain model representing the STUDENT table.
 */
public class Student {

    public enum StudentStatus {
        ACTIVE, GRADUATED, DROPPED, SUSPENDED;

        public static StudentStatus fromString(String val) throws InvalidDataException {
            if (val == null || val.trim().isEmpty()) {
                return ACTIVE;
            }
            try {
                return StudentStatus.valueOf(val.trim().toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new InvalidDataException("Invalid Student status: " + val + ". Allowed: ACTIVE, GRADUATED, DROPPED, SUSPENDED");
            }
        }
    }

    private Integer studentId;
    private String rollNumber;
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private String department;
    private int semester;
    private double cgpa;
    private int enrollmentYear;
    private StudentStatus status;
    private LocalDateTime createdAt;

    public Student() {
        this.semester = 1;
        this.cgpa = 0.00;
        this.enrollmentYear = 2024;
        this.status = StudentStatus.ACTIVE;
    }

    public Student(String rollNumber, String firstName, String lastName, String email,
                   String phone, String department, int semester, double cgpa,
                   int enrollmentYear, StudentStatus status) throws InvalidDataException {
        setRollNumber(rollNumber);
        setFirstName(firstName);
        setLastName(lastName);
        setEmail(email);
        setPhone(phone);
        setDepartment(department);
        setSemester(semester);
        setCgpa(cgpa);
        setEnrollmentYear(enrollmentYear);
        setStatus(status);
    }

    public Student(Integer studentId, String rollNumber, String firstName, String lastName,
                   String email, String phone, String department, int semester, double cgpa,
                   int enrollmentYear, StudentStatus status, LocalDateTime createdAt) throws InvalidDataException {
        this(rollNumber, firstName, lastName, email, phone, department, semester, cgpa, enrollmentYear, status);
        this.studentId = studentId;
        this.createdAt = createdAt;
    }

    public Integer getStudentId() {
        return studentId;
    }

    public void setStudentId(Integer studentId) {
        this.studentId = studentId;
    }

    public String getRollNumber() {
        return rollNumber;
    }

    public void setRollNumber(String rollNumber) throws InvalidDataException {
        if (rollNumber == null || rollNumber.trim().isEmpty()) {
            throw new InvalidDataException("Student roll number cannot be empty.");
        }
        if (rollNumber.trim().length() > 30) {
            throw new InvalidDataException("Student roll number cannot exceed 30 characters.");
        }
        this.rollNumber = rollNumber.trim().toUpperCase();
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) throws InvalidDataException {
        if (firstName == null || firstName.trim().isEmpty()) {
            throw new InvalidDataException("Student first name cannot be empty.");
        }
        if (firstName.trim().length() > 50) {
            throw new InvalidDataException("Student first name cannot exceed 50 characters.");
        }
        this.firstName = firstName.trim();
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) throws InvalidDataException {
        if (lastName == null || lastName.trim().isEmpty()) {
            throw new InvalidDataException("Student last name cannot be empty.");
        }
        if (lastName.trim().length() > 50) {
            throw new InvalidDataException("Student last name cannot exceed 50 characters.");
        }
        this.lastName = lastName.trim();
    }

    public String getFullName() {
        return (firstName != null ? firstName : "") + " " + (lastName != null ? lastName : "");
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) throws InvalidDataException {
        if (email == null || email.trim().isEmpty()) {
            throw new InvalidDataException("Student email cannot be empty.");
        }
        if (!email.contains("@") || !email.contains(".")) {
            throw new InvalidDataException("Invalid student email format: " + email);
        }
        if (email.trim().length() > 100) {
            throw new InvalidDataException("Student email cannot exceed 100 characters.");
        }
        this.email = email.trim().toLowerCase();
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) throws InvalidDataException {
        if (phone != null && phone.trim().length() > 20) {
            throw new InvalidDataException("Student phone cannot exceed 20 characters.");
        }
        this.phone = (phone != null && !phone.trim().isEmpty()) ? phone.trim() : null;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) throws InvalidDataException {
        if (department == null || department.trim().isEmpty()) {
            throw new InvalidDataException("Student department cannot be empty.");
        }
        if (department.trim().length() > 50) {
            throw new InvalidDataException("Student department cannot exceed 50 characters.");
        }
        this.department = department.trim();
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

    public double getCgpa() {
        return cgpa;
    }

    public void setCgpa(double cgpa) throws InvalidDataException {
        if (cgpa < 0.00 || cgpa > 10.00) {
            throw new InvalidDataException("Invalid CGPA: " + cgpa + ". CGPA must be between 0.00 and 10.00.");
        }
        this.cgpa = Math.round(cgpa * 100.0) / 100.0;
    }

    public int getEnrollmentYear() {
        return enrollmentYear;
    }

    public void setEnrollmentYear(int enrollmentYear) throws InvalidDataException {
        if (enrollmentYear < 2000) {
            throw new InvalidDataException("Invalid enrollment year: " + enrollmentYear + ". Must be 2000 or later.");
        }
        this.enrollmentYear = enrollmentYear;
    }

    public StudentStatus getStatus() {
        return status;
    }

    public void setStatus(StudentStatus status) {
        this.status = status != null ? status : StudentStatus.ACTIVE;
    }

    public void setStatus(String statusStr) throws InvalidDataException {
        this.status = StudentStatus.fromString(statusStr);
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
        Student student = (Student) o;
        return Objects.equals(studentId, student.studentId) || Objects.equals(rollNumber, student.rollNumber);
    }

    @Override
    public int hashCode() {
        return Objects.hash(studentId, rollNumber);
    }

    @Override
    public String toString() {
        return String.format("Student[ID=%d, Roll='%s', Name='%s', Dept='%s', Sem=%d, CGPA=%.2f, Status=%s]",
                studentId, rollNumber, getFullName(), department, semester, cgpa, status);
    }
}
