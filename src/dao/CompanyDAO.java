package dao;

import database.DBConnection;
import exception.DuplicateRecordException;
import exception.InvalidDataException;
import exception.RecordNotFoundException;
import model.Company;
import model.Company.CompanyStatus;

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
 * Data Access Object for the COMPANY table.
 */
public class CompanyDAO {

    private static final String SQL_INSERT =
            "INSERT INTO COMPANY (company_name, industry, website, contact_person, contact_email, contact_phone, address, status) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

    private static final String SQL_FIND_BY_ID =
            "SELECT company_id, company_name, industry, website, contact_person, contact_email, contact_phone, address, status, created_at " +
            "FROM COMPANY WHERE company_id = ?";

    private static final String SQL_FIND_BY_NAME =
            "SELECT company_id, company_name, industry, website, contact_person, contact_email, contact_phone, address, status, created_at " +
            "FROM COMPANY WHERE company_name = ?";

    private static final String SQL_FIND_BY_EMAIL =
            "SELECT company_id, company_name, industry, website, contact_person, contact_email, contact_phone, address, status, created_at " +
            "FROM COMPANY WHERE contact_email = ?";

    private static final String SQL_FIND_ALL =
            "SELECT company_id, company_name, industry, website, contact_person, contact_email, contact_phone, address, status, created_at " +
            "FROM COMPANY ORDER BY company_id ASC";

    private static final String SQL_FIND_BY_STATUS =
            "SELECT company_id, company_name, industry, website, contact_person, contact_email, contact_phone, address, status, created_at " +
            "FROM COMPANY WHERE status = ? ORDER BY company_name ASC";

    private static final String SQL_UPDATE =
            "UPDATE COMPANY SET company_name = ?, industry = ?, website = ?, contact_person = ?, " +
            "contact_email = ?, contact_phone = ?, address = ?, status = ? WHERE company_id = ?";

    private static final String SQL_DELETE =
            "DELETE FROM COMPANY WHERE company_id = ?";

    /**
     * Inserts a new Company into the database.
     */
    public Company create(Company company) throws SQLException, DuplicateRecordException {
        if (company == null) {
            throw new IllegalArgumentException("Company entity cannot be null.");
        }

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_INSERT, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, company.getCompanyName());
            ps.setString(2, company.getIndustry());
            ps.setString(3, company.getWebsite());
            ps.setString(4, company.getContactPerson());
            ps.setString(5, company.getContactEmail());
            ps.setString(6, company.getContactPhone());
            ps.setString(7, company.getAddress());
            ps.setString(8, company.getStatus() != null ? company.getStatus().name() : CompanyStatus.ACTIVE.name());

            int affectedRows = ps.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Creating company failed, no rows affected.");
            }

            try (ResultSet generatedKeys = ps.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    company.setCompanyId(generatedKeys.getInt(1));
                } else {
                    throw new SQLException("Creating company failed, no ID obtained.");
                }
            }
            return company;
        } catch (SQLException e) {
            if ("23000".equals(e.getSQLState()) || e.getErrorCode() == 1062) {
                throw new DuplicateRecordException("Company with this name or email already exists. (" + e.getMessage() + ")", e);
            }
            throw e;
        }
    }

    /**
     * Finds a company by primary key.
     */
    public Optional<Company> findById(int companyId) throws SQLException, InvalidDataException {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_FIND_BY_ID)) {

            ps.setInt(1, companyId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToCompany(rs));
                }
            }
        }
        return Optional.empty();
    }

    /**
     * Finds a company by unique company name.
     */
    public Optional<Company> findByName(String companyName) throws SQLException, InvalidDataException {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_FIND_BY_NAME)) {

            ps.setString(1, companyName != null ? companyName.trim() : "");
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToCompany(rs));
                }
            }
        }
        return Optional.empty();
    }

    /**
     * Finds a company by contact email.
     */
    public Optional<Company> findByEmail(String contactEmail) throws SQLException, InvalidDataException {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_FIND_BY_EMAIL)) {

            ps.setString(1, contactEmail != null ? contactEmail.trim().toLowerCase() : "");
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToCompany(rs));
                }
            }
        }
        return Optional.empty();
    }

    /**
     * Retrieves all companies.
     */
    public List<Company> findAll() throws SQLException, InvalidDataException {
        List<Company> list = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_FIND_ALL);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                list.add(mapResultSetToCompany(rs));
            }
        }
        return list;
    }

    /**
     * Retrieves companies filtered by status.
     */
    public List<Company> findByStatus(CompanyStatus status) throws SQLException, InvalidDataException {
        List<Company> list = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_FIND_BY_STATUS)) {

            ps.setString(1, status != null ? status.name() : CompanyStatus.ACTIVE.name());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToCompany(rs));
                }
            }
        }
        return list;
    }

    /**
     * Updates an existing company record.
     */
    public boolean update(Company company) throws SQLException, RecordNotFoundException, DuplicateRecordException {
        if (company == null || company.getCompanyId() == null) {
            throw new IllegalArgumentException("Company and Company ID cannot be null for update.");
        }

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_UPDATE)) {

            ps.setString(1, company.getCompanyName());
            ps.setString(2, company.getIndustry());
            ps.setString(3, company.getWebsite());
            ps.setString(4, company.getContactPerson());
            ps.setString(5, company.getContactEmail());
            ps.setString(6, company.getContactPhone());
            ps.setString(7, company.getAddress());
            ps.setString(8, company.getStatus() != null ? company.getStatus().name() : CompanyStatus.ACTIVE.name());
            ps.setInt(9, company.getCompanyId());

            int affectedRows = ps.executeUpdate();
            if (affectedRows == 0) {
                throw new RecordNotFoundException("Company with ID " + company.getCompanyId() + " not found for update.");
            }
            return true;
        } catch (SQLException e) {
            if ("23000".equals(e.getSQLState()) || e.getErrorCode() == 1062) {
                throw new DuplicateRecordException("Cannot update company: Name or email already in use. (" + e.getMessage() + ")", e);
            }
            throw e;
        }
    }

    /**
     * Permanently deletes a company by ID.
     */
    public boolean delete(int companyId) throws SQLException, RecordNotFoundException {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_DELETE)) {

            ps.setInt(1, companyId);
            int affectedRows = ps.executeUpdate();
            if (affectedRows == 0) {
                throw new RecordNotFoundException("Company with ID " + companyId + " not found for deletion.");
            }
            return true;
        }
    }

    private Company mapResultSetToCompany(ResultSet rs) throws SQLException, InvalidDataException {
        Timestamp ts = rs.getTimestamp("created_at");
        return new Company(
                rs.getInt("company_id"),
                rs.getString("company_name"),
                rs.getString("industry"),
                rs.getString("website"),
                rs.getString("contact_person"),
                rs.getString("contact_email"),
                rs.getString("contact_phone"),
                rs.getString("address"),
                CompanyStatus.fromString(rs.getString("status")),
                ts != null ? ts.toLocalDateTime() : null
        );
    }
}
