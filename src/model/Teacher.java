package model;

import exception.InvalidDataException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Domain model representing the TEACHER table.
 */
public class Teacher {

    public enum TeacherStatus {
        ACTIVE, INACTIVE, ON_LEAVE;

        public static TeacherStatus fromString(String val) throws InvalidDataException {
            if (val == null || val.trim().isEmpty()) {
                return ACTIVE;
            }
            try {
                return TeacherStatus.valueOf(val.trim().toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new InvalidDataException("Invalid Teacher status: " + val + ". Allowed: ACTIVE, INACTIVE, ON_LEAVE");
            }
        }
    }

    private Integer teacherId;
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private String department;
    private String designation;
    private LocalDate hireDate;
    private TeacherStatus status;
    private LocalDateTime createdAt;

    public Teacher() {
        this.status = TeacherStatus.ACTIVE;
        this.designation = "Assistant Professor";
        this.hireDate = LocalDate.now();
    }

    public Teacher(String firstName, String lastName, String email, String phone,
                   String department, String designation, LocalDate hireDate, TeacherStatus status) throws InvalidDataException {
        setFirstName(firstName);
        setLastName(lastName);
        setEmail(email);
        setPhone(phone);
        setDepartment(department);
        setDesignation(designation);
        setHireDate(hireDate);
        setStatus(status);
    }

    public Teacher(Integer teacherId, String firstName, String lastName, String email, String phone,
                   String department, String designation, LocalDate hireDate, TeacherStatus status,
                   LocalDateTime createdAt) throws InvalidDataException {
        this(firstName, lastName, email, phone, department, designation, hireDate, status);
        this.teacherId = teacherId;
        this.createdAt = createdAt;
    }

    public Integer getTeacherId() {
        return teacherId;
    }

    public void setTeacherId(Integer teacherId) {
        this.teacherId = teacherId;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) throws InvalidDataException {
        if (firstName == null || firstName.trim().isEmpty()) {
            throw new InvalidDataException("Teacher first name cannot be empty.");
        }
        if (firstName.trim().length() > 50) {
            throw new InvalidDataException("Teacher first name cannot exceed 50 characters.");
        }
        this.firstName = firstName.trim();
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) throws InvalidDataException {
        if (lastName == null || lastName.trim().isEmpty()) {
            throw new InvalidDataException("Teacher last name cannot be empty.");
        }
        if (lastName.trim().length() > 50) {
            throw new InvalidDataException("Teacher last name cannot exceed 50 characters.");
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
            throw new InvalidDataException("Teacher email cannot be empty.");
        }
        if (!email.contains("@") || !email.contains(".")) {
            throw new InvalidDataException("Invalid teacher email format: " + email);
        }
        if (email.trim().length() > 100) {
            throw new InvalidDataException("Teacher email cannot exceed 100 characters.");
        }
        this.email = email.trim().toLowerCase();
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) throws InvalidDataException {
        if (phone != null && phone.trim().length() > 20) {
            throw new InvalidDataException("Teacher phone cannot exceed 20 characters.");
        }
        this.phone = (phone != null && !phone.trim().isEmpty()) ? phone.trim() : null;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) throws InvalidDataException {
        if (department == null || department.trim().isEmpty()) {
            throw new InvalidDataException("Teacher department cannot be empty.");
        }
        if (department.trim().length() > 50) {
            throw new InvalidDataException("Teacher department cannot exceed 50 characters.");
        }
        this.department = department.trim();
    }

    public String getDesignation() {
        return designation;
    }

    public void setDesignation(String designation) throws InvalidDataException {
        if (designation == null || designation.trim().isEmpty()) {
            this.designation = "Assistant Professor";
        } else {
            if (designation.trim().length() > 50) {
                throw new InvalidDataException("Teacher designation cannot exceed 50 characters.");
            }
            this.designation = designation.trim();
        }
    }

    public LocalDate getHireDate() {
        return hireDate;
    }

    public void setHireDate(LocalDate hireDate) throws InvalidDataException {
        if (hireDate == null) {
            throw new InvalidDataException("Teacher hire date cannot be null.");
        }
        this.hireDate = hireDate;
    }

    public TeacherStatus getStatus() {
        return status;
    }

    public void setStatus(TeacherStatus status) {
        this.status = status != null ? status : TeacherStatus.ACTIVE;
    }

    public void setStatus(String statusStr) throws InvalidDataException {
        this.status = TeacherStatus.fromString(statusStr);
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
        Teacher teacher = (Teacher) o;
        return Objects.equals(teacherId, teacher.teacherId) || Objects.equals(email, teacher.email);
    }

    @Override
    public int hashCode() {
        return Objects.hash(teacherId, email);
    }

    @Override
    public String toString() {
        return String.format("Teacher[ID=%d, Name='%s', Email='%s', Dept='%s', Desig='%s', Status=%s]",
                teacherId, getFullName(), email, department, designation, status);
    }
}
