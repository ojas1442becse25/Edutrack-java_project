package model;

import exception.InvalidDataException;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Domain model representing the PLACEMENT_DRIVE table.
 */
public class PlacementDrive {

    public enum DriveStatus {
        UPCOMING, IN_PROGRESS, COMPLETED, CANCELLED;

        public static DriveStatus fromString(String val) throws InvalidDataException {
            if (val == null || val.trim().isEmpty()) {
                return UPCOMING;
            }
            try {
                return DriveStatus.valueOf(val.trim().toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new InvalidDataException("Invalid Drive status: " + val + ". Allowed: UPCOMING, IN_PROGRESS, COMPLETED, CANCELLED");
            }
        }
    }

    private Integer driveId;
    private int companyId;
    private String jobTitle;
    private String jobDescription;
    private double packageLpa;
    private double minCgpaCriteria;
    private String eligibleDepartment;
    private LocalDate driveDate;
    private LocalDate lastDateToApply;
    private DriveStatus status;
    private LocalDateTime createdAt;

    public PlacementDrive() {
        this.packageLpa = 5.0;
        this.minCgpaCriteria = 6.0;
        this.eligibleDepartment = "ALL";
        this.status = DriveStatus.UPCOMING;
    }

    public PlacementDrive(int companyId, String jobTitle, String jobDescription,
                          double packageLpa, double minCgpaCriteria, String eligibleDepartment,
                          LocalDate driveDate, LocalDate lastDateToApply, DriveStatus status) throws InvalidDataException {
        setCompanyId(companyId);
        setJobTitle(jobTitle);
        setJobDescription(jobDescription);
        setPackageLpa(packageLpa);
        setMinCgpaCriteria(minCgpaCriteria);
        setEligibleDepartment(eligibleDepartment);
        setDriveDate(driveDate);
        setLastDateToApply(lastDateToApply);
        setStatus(status);
    }

    public PlacementDrive(Integer driveId, int companyId, String jobTitle, String jobDescription,
                          double packageLpa, double minCgpaCriteria, String eligibleDepartment,
                          LocalDate driveDate, LocalDate lastDateToApply, DriveStatus status,
                          LocalDateTime createdAt) throws InvalidDataException {
        this(companyId, jobTitle, jobDescription, packageLpa, minCgpaCriteria, eligibleDepartment, driveDate, lastDateToApply, status);
        this.driveId = driveId;
        this.createdAt = createdAt;
    }

    public Integer getDriveId() {
        return driveId;
    }

    public void setDriveId(Integer driveId) {
        this.driveId = driveId;
    }

    public int getCompanyId() {
        return companyId;
    }

    public void setCompanyId(int companyId) throws InvalidDataException {
        if (companyId <= 0) {
            throw new InvalidDataException("Company ID must be positive.");
        }
        this.companyId = companyId;
    }

    public String getJobTitle() {
        return jobTitle;
    }

    public void setJobTitle(String jobTitle) throws InvalidDataException {
        if (jobTitle == null || jobTitle.trim().isEmpty()) {
            throw new InvalidDataException("Job title cannot be empty.");
        }
        if (jobTitle.trim().length() > 100) {
            throw new InvalidDataException("Job title cannot exceed 100 characters.");
        }
        this.jobTitle = jobTitle.trim();
    }

    public String getJobDescription() {
        return jobDescription;
    }

    public void setJobDescription(String jobDescription) {
        this.jobDescription = jobDescription != null ? jobDescription.trim() : null;
    }

    public double getPackageLpa() {
        return packageLpa;
    }

    public void setPackageLpa(double packageLpa) throws InvalidDataException {
        if (packageLpa <= 0.00) {
            throw new InvalidDataException("Package LPA must be greater than zero.");
        }
        this.packageLpa = Math.round(packageLpa * 100.0) / 100.0;
    }

    public double getMinCgpaCriteria() {
        return minCgpaCriteria;
    }

    public void setMinCgpaCriteria(double minCgpaCriteria) throws InvalidDataException {
        if (minCgpaCriteria < 0.00 || minCgpaCriteria > 10.00) {
            throw new InvalidDataException("Minimum CGPA criteria must be between 0.00 and 10.00.");
        }
        this.minCgpaCriteria = Math.round(minCgpaCriteria * 100.0) / 100.0;
    }

    public String getEligibleDepartment() {
        return eligibleDepartment;
    }

    public void setEligibleDepartment(String eligibleDepartment) {
        if (eligibleDepartment == null || eligibleDepartment.trim().isEmpty()) {
            this.eligibleDepartment = "ALL";
        } else {
            this.eligibleDepartment = eligibleDepartment.trim();
        }
    }

    public LocalDate getDriveDate() {
        return driveDate;
    }

    public void setDriveDate(LocalDate driveDate) throws InvalidDataException {
        if (driveDate == null) {
            throw new InvalidDataException("Drive date cannot be null.");
        }
        this.driveDate = driveDate;
    }

    public LocalDate getLastDateToApply() {
        return lastDateToApply;
    }

    public void setLastDateToApply(LocalDate lastDateToApply) throws InvalidDataException {
        if (lastDateToApply == null) {
            throw new InvalidDataException("Last date to apply cannot be null.");
        }
        this.lastDateToApply = lastDateToApply;
    }

    public DriveStatus getStatus() {
        return status;
    }

    public void setStatus(DriveStatus status) {
        this.status = status != null ? status : DriveStatus.UPCOMING;
    }

    public void setStatus(String statusStr) throws InvalidDataException {
        this.status = DriveStatus.fromString(statusStr);
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return String.format("PlacementDrive[ID=%d, CompanyID=%d, Role='%s', Package=%.2f LPA, MinCGPA=%.2f, Date=%s, Status=%s]",
                driveId, companyId, jobTitle, packageLpa, minCgpaCriteria, driveDate, status);
    }
}
