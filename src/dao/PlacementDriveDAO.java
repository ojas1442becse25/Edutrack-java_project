package dao;

import database.DBConnection;
import exception.DuplicateRecordException;
import exception.InvalidDataException;
import exception.RecordNotFoundException;
import model.PlacementDrive;
import model.PlacementDrive.DriveStatus;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class PlacementDriveDAO {

    private static final String SQL_INSERT =
            "INSERT INTO PLACEMENT_DRIVE (company_id, job_title, job_description, package_lpa, min_cgpa_criteria, " +
            "eligible_department, drive_date, last_date_to_apply, status) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

    private static final String SQL_FIND_BY_ID =
            "SELECT drive_id, company_id, job_title, job_description, package_lpa, min_cgpa_criteria, " +
            "eligible_department, drive_date, last_date_to_apply, status, created_at " +
            "FROM PLACEMENT_DRIVE WHERE drive_id = ?";

    private static final String SQL_FIND_ALL =
            "SELECT drive_id, company_id, job_title, job_description, package_lpa, min_cgpa_criteria, " +
            "eligible_department, drive_date, last_date_to_apply, status, created_at " +
            "FROM PLACEMENT_DRIVE ORDER BY drive_date ASC";

    private static final String SQL_FIND_ACTIVE =
            "SELECT drive_id, company_id, job_title, job_description, package_lpa, min_cgpa_criteria, " +
            "eligible_department, drive_date, last_date_to_apply, status, created_at " +
            "FROM PLACEMENT_DRIVE WHERE status IN ('UPCOMING', 'IN_PROGRESS') ORDER BY drive_date ASC";

    private static final String SQL_FIND_BY_COMPANY =
            "SELECT drive_id, company_id, job_title, job_description, package_lpa, min_cgpa_criteria, " +
            "eligible_department, drive_date, last_date_to_apply, status, created_at " +
            "FROM PLACEMENT_DRIVE WHERE company_id = ? ORDER BY drive_date DESC";

    private static final String SQL_UPDATE =
            "UPDATE PLACEMENT_DRIVE SET company_id = ?, job_title = ?, job_description = ?, package_lpa = ?, " +
            "min_cgpa_criteria = ?, eligible_department = ?, drive_date = ?, last_date_to_apply = ?, status = ? " +
            "WHERE drive_id = ?";

    private static final String SQL_UPDATE_STATUS =
            "UPDATE PLACEMENT_DRIVE SET status = ? WHERE drive_id = ?";

    private static final String SQL_DELETE =
            "DELETE FROM PLACEMENT_DRIVE WHERE drive_id = ?";

        public PlacementDrive create(PlacementDrive drive) throws SQLException, DuplicateRecordException {
        if (drive == null) {
            throw new IllegalArgumentException("PlacementDrive entity cannot be null.");
        }

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_INSERT, Statement.RETURN_GENERATED_KEYS)) {

            ps.setInt(1, drive.getCompanyId());
            ps.setString(2, drive.getJobTitle());
            ps.setString(3, drive.getJobDescription());
            ps.setDouble(4, drive.getPackageLpa());
            ps.setDouble(5, drive.getMinCgpaCriteria());
            ps.setString(6, drive.getEligibleDepartment());
            ps.setDate(7, Date.valueOf(drive.getDriveDate()));
            ps.setDate(8, Date.valueOf(drive.getLastDateToApply()));
            ps.setString(9, drive.getStatus() != null ? drive.getStatus().name() : DriveStatus.UPCOMING.name());

            int affectedRows = ps.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Creating placement drive failed, no rows affected.");
            }

            try (ResultSet generatedKeys = ps.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    drive.setDriveId(generatedKeys.getInt(1));
                } else {
                    throw new SQLException("Creating placement drive failed, no ID obtained.");
                }
            }
            return drive;
        } catch (SQLException e) {
            // check duplicate key
            if ("23000".equals(e.getSQLState()) || e.getErrorCode() == 1062) {
                throw new DuplicateRecordException("Duplicate placement drive constraint violation: " + e.getMessage(), e);
            }
            throw e;
        }
    }

        public Optional<PlacementDrive> findById(int driveId) throws SQLException, InvalidDataException {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_FIND_BY_ID)) {

            ps.setInt(1, driveId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToDrive(rs));
                }
            }
        }
        return Optional.empty();
    }

        public List<PlacementDrive> findAll() throws SQLException, InvalidDataException {
        List<PlacementDrive> list = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_FIND_ALL);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                list.add(mapResultSetToDrive(rs));
            }
        }
        return list;
    }

        public List<PlacementDrive> findActiveDrives() throws SQLException, InvalidDataException {
        List<PlacementDrive> list = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_FIND_ACTIVE);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                list.add(mapResultSetToDrive(rs));
            }
        }
        return list;
    }

        public List<PlacementDrive> findByCompanyId(int companyId) throws SQLException, InvalidDataException {
        List<PlacementDrive> list = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_FIND_BY_COMPANY)) {

            ps.setInt(1, companyId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToDrive(rs));
                }
            }
        }
        return list;
    }

        public boolean update(PlacementDrive drive) throws SQLException, RecordNotFoundException, DuplicateRecordException {
        if (drive == null || drive.getDriveId() == null) {
            throw new IllegalArgumentException("PlacementDrive and Drive ID cannot be null for update.");
        }

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_UPDATE)) {

            ps.setInt(1, drive.getCompanyId());
            ps.setString(2, drive.getJobTitle());
            ps.setString(3, drive.getJobDescription());
            ps.setDouble(4, drive.getPackageLpa());
            ps.setDouble(5, drive.getMinCgpaCriteria());
            ps.setString(6, drive.getEligibleDepartment());
            ps.setDate(7, Date.valueOf(drive.getDriveDate()));
            ps.setDate(8, Date.valueOf(drive.getLastDateToApply()));
            ps.setString(9, drive.getStatus() != null ? drive.getStatus().name() : DriveStatus.UPCOMING.name());
            ps.setInt(10, drive.getDriveId());

            int affectedRows = ps.executeUpdate();
            if (affectedRows == 0) {
                throw new RecordNotFoundException("Placement drive with ID " + drive.getDriveId() + " not found for update.");
            }
            return true;
        } catch (SQLException e) {
            // check duplicate key
            if ("23000".equals(e.getSQLState()) || e.getErrorCode() == 1062) {
                throw new DuplicateRecordException("Cannot update placement drive: Constraint failure. (" + e.getMessage() + ")", e);
            }
            throw e;
        }
    }

        public boolean updateStatus(int driveId, DriveStatus newStatus) throws SQLException, RecordNotFoundException {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_UPDATE_STATUS)) {

            ps.setString(1, newStatus != null ? newStatus.name() : DriveStatus.UPCOMING.name());
            ps.setInt(2, driveId);

            int affectedRows = ps.executeUpdate();
            if (affectedRows == 0) {
                throw new RecordNotFoundException("Placement drive with ID " + driveId + " not found for status update.");
            }
            return true;
        }
    }

        public boolean delete(int driveId) throws SQLException, RecordNotFoundException {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_DELETE)) {

            ps.setInt(1, driveId);
            int affectedRows = ps.executeUpdate();
            if (affectedRows == 0) {
                throw new RecordNotFoundException("Placement drive with ID " + driveId + " not found for deletion.");
            }
            return true;
        }
    }

    private PlacementDrive mapResultSetToDrive(ResultSet rs) throws SQLException, InvalidDataException {
        Date dDate = rs.getDate("drive_date");
        Date lastDate = rs.getDate("last_date_to_apply");
        Timestamp ts = rs.getTimestamp("created_at");

        return new PlacementDrive(
                rs.getInt("drive_id"),
                rs.getInt("company_id"),
                rs.getString("job_title"),
                rs.getString("job_description"),
                rs.getDouble("package_lpa"),
                rs.getDouble("min_cgpa_criteria"),
                rs.getString("eligible_department"),
                dDate != null ? dDate.toLocalDate() : null,
                lastDate != null ? lastDate.toLocalDate() : null,
                DriveStatus.fromString(rs.getString("status")),
                ts != null ? ts.toLocalDateTime() : null
        );
    }
}
