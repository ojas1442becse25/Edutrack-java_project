package model;

import exception.InvalidDataException;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Domain model representing the COMPANY table.
 */
public class Company {

    public enum CompanyStatus {
        ACTIVE, INACTIVE, BLACKLISTED;

        public static CompanyStatus fromString(String val) throws InvalidDataException {
            if (val == null || val.trim().isEmpty()) {
                return ACTIVE;
            }
            try {
                return CompanyStatus.valueOf(val.trim().toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new InvalidDataException("Invalid Company status: " + val + ". Allowed: ACTIVE, INACTIVE, BLACKLISTED");
            }
        }
    }

    private Integer companyId;
    private String companyName;
    private String industry;
    private String website;
    private String contactPerson;
    private String contactEmail;
    private String contactPhone;
    private String address;
    private CompanyStatus status;
    private LocalDateTime createdAt;

    public Company() {
        this.status = CompanyStatus.ACTIVE;
    }

    public Company(String companyName, String industry, String website, String contactPerson,
                   String contactEmail, String contactPhone, String address, CompanyStatus status) throws InvalidDataException {
        setCompanyName(companyName);
        setIndustry(industry);
        setWebsite(website);
        setContactPerson(contactPerson);
        setContactEmail(contactEmail);
        setContactPhone(contactPhone);
        setAddress(address);
        setStatus(status);
    }

    public Company(Integer companyId, String companyName, String industry, String website,
                   String contactPerson, String contactEmail, String contactPhone, String address,
                   CompanyStatus status, LocalDateTime createdAt) throws InvalidDataException {
        this(companyName, industry, website, contactPerson, contactEmail, contactPhone, address, status);
        this.companyId = companyId;
        this.createdAt = createdAt;
    }

    public Integer getCompanyId() {
        return companyId;
    }

    public void setCompanyId(Integer companyId) {
        this.companyId = companyId;
    }

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) throws InvalidDataException {
        if (companyName == null || companyName.trim().isEmpty()) {
            throw new InvalidDataException("Company name cannot be empty.");
        }
        if (companyName.trim().length() > 100) {
            throw new InvalidDataException("Company name cannot exceed 100 characters.");
        }
        this.companyName = companyName.trim();
    }

    public String getIndustry() {
        return industry;
    }

    public void setIndustry(String industry) throws InvalidDataException {
        if (industry == null || industry.trim().isEmpty()) {
            throw new InvalidDataException("Industry cannot be empty.");
        }
        if (industry.trim().length() > 50) {
            throw new InvalidDataException("Industry cannot exceed 50 characters.");
        }
        this.industry = industry.trim();
    }

    public String getWebsite() {
        return website;
    }

    public void setWebsite(String website) throws InvalidDataException {
        if (website != null && website.trim().length() > 150) {
            throw new InvalidDataException("Website cannot exceed 150 characters.");
        }
        this.website = (website != null && !website.trim().isEmpty()) ? website.trim() : null;
    }

    public String getContactPerson() {
        return contactPerson;
    }

    public void setContactPerson(String contactPerson) throws InvalidDataException {
        if (contactPerson == null || contactPerson.trim().isEmpty()) {
            throw new InvalidDataException("Contact person cannot be empty.");
        }
        if (contactPerson.trim().length() > 100) {
            throw new InvalidDataException("Contact person cannot exceed 100 characters.");
        }
        this.contactPerson = contactPerson.trim();
    }

    public String getContactEmail() {
        return contactEmail;
    }

    public void setContactEmail(String contactEmail) throws InvalidDataException {
        if (contactEmail == null || contactEmail.trim().isEmpty()) {
            throw new InvalidDataException("Contact email cannot be empty.");
        }
        if (!contactEmail.contains("@") || !contactEmail.contains(".")) {
            throw new InvalidDataException("Invalid contact email format: " + contactEmail);
        }
        if (contactEmail.trim().length() > 100) {
            throw new InvalidDataException("Contact email cannot exceed 100 characters.");
        }
        this.contactEmail = contactEmail.trim().toLowerCase();
    }

    public String getContactPhone() {
        return contactPhone;
    }

    public void setContactPhone(String contactPhone) throws InvalidDataException {
        if (contactPhone != null && contactPhone.trim().length() > 20) {
            throw new InvalidDataException("Contact phone cannot exceed 20 characters.");
        }
        this.contactPhone = (contactPhone != null && !contactPhone.trim().isEmpty()) ? contactPhone.trim() : null;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) throws InvalidDataException {
        if (address != null && address.trim().length() > 255) {
            throw new InvalidDataException("Address cannot exceed 255 characters.");
        }
        this.address = (address != null && !address.trim().isEmpty()) ? address.trim() : null;
    }

    public CompanyStatus getStatus() {
        return status;
    }

    public void setStatus(CompanyStatus status) {
        this.status = status != null ? status : CompanyStatus.ACTIVE;
    }

    public void setStatus(String statusStr) throws InvalidDataException {
        this.status = CompanyStatus.fromString(statusStr);
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
        Company company = (Company) o;
        return Objects.equals(companyId, company.companyId) || Objects.equals(companyName, company.companyName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(companyId, companyName);
    }

    @Override
    public String toString() {
        return String.format("Company[ID=%d, Name='%s', Industry='%s', Contact='%s', Status=%s]",
                companyId, companyName, industry, contactPerson, status);
    }
}
