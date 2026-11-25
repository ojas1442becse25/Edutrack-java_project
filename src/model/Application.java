package model;

import exception.InvalidDataException;
import java.time.LocalDateTime;

public class Application {

    public enum ApplicationStatus {
        APPLIED, SHORTLISTED, TECHNICAL_ROUND, HR_ROUND, OFFERED, REJECTED, WITHDRAWN;

        public static ApplicationStatus fromString(String val) throws InvalidDataException {
            if (val == null || val.trim().isEmpty()) {
                return APPLIED;
            }
            try {
                return ApplicationStatus.valueOf(val.trim().toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new InvalidDataException("Invalid Application status: " + val +
                        ". Allowed: APPLIED, SHORTLISTED, TECHNICAL_ROUND, HR_ROUND, OFFERED, REJECTED, WITHDRAWN");
            }
        }
    }

    private Integer applicationId;
    private int driveId;
    private int studentId;
    private LocalDateTime applicationDate;
    private ApplicationStatus status;
    private String interviewFeedback;

    public Application() {
        this.applicationDate = LocalDateTime.now();
        this.status = ApplicationStatus.APPLIED;
    }

    public Application(int driveId, int studentId, ApplicationStatus status, String interviewFeedback) throws InvalidDataException {
        setDriveId(driveId);
        setStudentId(studentId);
        setStatus(status);
        setInterviewFeedback(interviewFeedback);
        this.applicationDate = LocalDateTime.now();
    }

    public Application(Integer applicationId, int driveId, int studentId,
                       LocalDateTime applicationDate, ApplicationStatus status,
                       String interviewFeedback) throws InvalidDataException {
        this(driveId, studentId, status, interviewFeedback);
        this.applicationId = applicationId;
        this.applicationDate = applicationDate;
    }

    public Integer getApplicationId() {
        return applicationId;
    }

    public void setApplicationId(Integer applicationId) {
        this.applicationId = applicationId;
    }

    public int getDriveId() {
        return driveId;
    }

    public void setDriveId(int driveId) throws InvalidDataException {
        if (driveId <= 0) {
            throw new InvalidDataException("Drive ID must be positive.");
        }
        this.driveId = driveId;
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

    public LocalDateTime getApplicationDate() {
        return applicationDate;
    }

    public void setApplicationDate(LocalDateTime applicationDate) {
        this.applicationDate = applicationDate != null ? applicationDate : LocalDateTime.now();
    }

    public ApplicationStatus getStatus() {
        return status;
    }

    public void setStatus(ApplicationStatus status) {
        this.status = status != null ? status : ApplicationStatus.APPLIED;
    }

    public void setStatus(String statusStr) throws InvalidDataException {
        this.status = ApplicationStatus.fromString(statusStr);
    }

    public String getInterviewFeedback() {
        return interviewFeedback;
    }

    public void setInterviewFeedback(String interviewFeedback) {
        this.interviewFeedback = interviewFeedback != null ? interviewFeedback.trim() : null;
    }

    @Override
    public String toString() {
        return String.format("Application[ID=%d, DriveID=%d, StudentID=%d, Date=%s, Status=%s]",
                applicationId, driveId, studentId, applicationDate, status);
    }
}
