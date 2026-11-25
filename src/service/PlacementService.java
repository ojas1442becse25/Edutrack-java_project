package service;

import dao.ApplicationDAO;
import dao.CompanyDAO;
import dao.PlacementDriveDAO;
import dao.StudentDAO;
import exception.DuplicateRecordException;
import exception.EligibilityException;
import exception.InvalidDataException;
import exception.RecordNotFoundException;
import model.Application;
import model.Application.ApplicationStatus;
import model.Company;
import model.PlacementDrive;
import model.PlacementDrive.DriveStatus;
import model.Student;
import model.Student.StudentStatus;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class PlacementService {

    private final CompanyDAO companyDAO;
    private final PlacementDriveDAO driveDAO;
    private final ApplicationDAO applicationDAO;
    private final StudentDAO studentDAO;

    private final ConcurrentHashMap<Integer, Integer> driveCapacityRegistry = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Integer, Object> driveLocks = new ConcurrentHashMap<>();

    public PlacementService() {
        this.companyDAO = new CompanyDAO();
        this.driveDAO = new PlacementDriveDAO();
        this.applicationDAO = new ApplicationDAO();
        this.studentDAO = new StudentDAO();
    }

    public PlacementService(CompanyDAO companyDAO, PlacementDriveDAO driveDAO,
                            ApplicationDAO applicationDAO, StudentDAO studentDAO) {
        this.companyDAO = companyDAO;
        this.driveDAO = driveDAO;
        this.applicationDAO = applicationDAO;
        this.studentDAO = studentDAO;
    }

    public Company registerCompany(Company company)
            throws InvalidDataException, DuplicateRecordException, SQLException {

        if (company == null) {
            throw new InvalidDataException("Company data cannot be null.");
        }

        Optional<Company> existingName = companyDAO.findByName(company.getCompanyName());
        if (existingName.isPresent()) {
            throw new DuplicateRecordException("Company '" + company.getCompanyName() + "' is already registered.");
        }

        Optional<Company> existingEmail = companyDAO.findByEmail(company.getContactEmail());
        if (existingEmail.isPresent()) {
            throw new DuplicateRecordException("A company with contact email '" + company.getContactEmail() + "' already exists.");
        }

        return companyDAO.create(company);
    }

    public Company getCompanyById(int companyId)
            throws RecordNotFoundException, SQLException, InvalidDataException {

        Optional<Company> comp = companyDAO.findById(companyId);
        if (!comp.isPresent()) {
            throw new RecordNotFoundException("Company with ID #" + companyId + " does not exist.");
        }
        return comp.get();
    }

    public List<Company> getAllCompanies() throws SQLException, InvalidDataException {
        return companyDAO.findAll();
    }

    public PlacementDrive createPlacementDrive(PlacementDrive drive, int maxVacancies)
            throws InvalidDataException, RecordNotFoundException, DuplicateRecordException, SQLException {

        if (drive == null) {
            throw new InvalidDataException("Placement drive data cannot be null.");
        }

        // Verify company exists
        getCompanyById(drive.getCompanyId());

        // Validate dates
        if (drive.getLastDateToApply().isAfter(drive.getDriveDate())) {
            throw new InvalidDataException("Last date to apply cannot be after the placement drive date.");
        }

        PlacementDrive created = driveDAO.create(drive);
        setDriveVacancies(created.getDriveId(), maxVacancies > 0 ? maxVacancies : 50);
        return created;
    }

    public void setDriveVacancies(int driveId, int vacancies) {
        driveCapacityRegistry.put(driveId, vacancies > 0 ? vacancies : 1);
    }

    public int getDriveVacancies(int driveId) {
        return driveCapacityRegistry.getOrDefault(driveId, 50);
    }

    public List<PlacementDrive> getAllDrives() throws SQLException, InvalidDataException {
        return driveDAO.findAll();
    }

    public List<PlacementDrive> getActiveDrives() throws SQLException, InvalidDataException {
        return driveDAO.findActiveDrives();
    }

    public PlacementDrive getDriveById(int driveId)
            throws RecordNotFoundException, SQLException, InvalidDataException {

        Optional<PlacementDrive> drive = driveDAO.findById(driveId);
        if (!drive.isPresent()) {
            throw new RecordNotFoundException("Placement drive with ID #" + driveId + " does not exist.");
        }
        return drive.get();
    }

    public boolean updateDriveStatus(int driveId, DriveStatus status)
            throws RecordNotFoundException, SQLException {

        return driveDAO.updateStatus(driveId, status);
    }

    public Application applyForDrive(int driveId, int studentId, String performedBy)
            throws RecordNotFoundException, EligibilityException, DuplicateRecordException, InvalidDataException, SQLException {

        int maxSlots = getDriveVacancies(driveId);
        return applyForDriveThreadSafe(driveId, studentId, maxSlots, performedBy);
    }

    public Application applyForDriveThreadSafe(int driveId, int studentId, int maxVacancies, String performedBy)
            throws RecordNotFoundException, EligibilityException, DuplicateRecordException, InvalidDataException, SQLException {

        PlacementDrive drive = getDriveById(driveId);

        if (drive.getStatus() == DriveStatus.COMPLETED || drive.getStatus() == DriveStatus.CANCELLED) {
            throw new EligibilityException("Placement drive #" + driveId + " is no longer accepting applications (Status: " + drive.getStatus() + ").");
        }

        if (LocalDate.now().isAfter(drive.getLastDateToApply())) {
            throw new EligibilityException("Application deadline passed on " + drive.getLastDateToApply() + ".");
        }

        Optional<Student> studentOpt = studentDAO.findById(studentId);
        if (!studentOpt.isPresent()) {
            throw new RecordNotFoundException("Student with ID #" + studentId + " does not exist.");
        }
        Student student = studentOpt.get();

        if (student.getStatus() != StudentStatus.ACTIVE) {
            throw new EligibilityException("Student #" + studentId + " is not eligible for placements (Status: " + student.getStatus() + ").");
        }

        // Department eligibility check
        if (!"ALL".equalsIgnoreCase(drive.getEligibleDepartment())) {
            String allowed = drive.getEligibleDepartment().toUpperCase();
            String studentDept = student.getDepartment().toUpperCase();
            if (!allowed.contains(studentDept)) {
                throw new EligibilityException("Department mismatch: Student (" + student.getDepartment() +
                        ") is not eligible for drive open to: " + drive.getEligibleDepartment() + ".");
            }
        }

        // CGPA cutoff criteria check
        if (student.getCgpa() < drive.getMinCgpaCriteria()) {
            throw new EligibilityException(String.format(
                    "Eligibility check failed: Student CGPA (%.2f) does not meet minimum drive criteria (%.2f).",
                    student.getCgpa(), drive.getMinCgpaCriteria()
            ));
        }

        // Duplicate application check
        Optional<Application> existing = applicationDAO.findByDriveAndStudent(driveId, studentId);
        if (existing.isPresent()) {
            throw new DuplicateRecordException("Student #" + studentId + " has already applied to Placement Drive #" + driveId + ".");
        }

        // lock on drive to prevent race condition
        Object lock = driveLocks.computeIfAbsent(driveId, k -> new Object());
        synchronized (lock) {
            List<Application> currentApps = applicationDAO.findByDriveId(driveId);
            if (currentApps.size() >= maxVacancies) {
                throw new EligibilityException(String.format(
                    "Drive capacity exhausted: All %d slots for Placement Drive #%d have been filled.",
                    maxVacancies, driveId
                ));
            }

            Application app = new Application(
                    driveId, studentId, ApplicationStatus.APPLIED, "Applied via EduTrack Placement Portal"
            );
            return applicationDAO.applyWithAudit(app, performedBy != null ? performedBy : "STUDENT_PORTAL");
        }
    }

    public List<Application> getApplicationsByDrive(int driveId)
            throws RecordNotFoundException, SQLException, InvalidDataException {

        getDriveById(driveId);
        return applicationDAO.findByDriveId(driveId);
    }

    public List<Application> getApplicationsByStudent(int studentId)
            throws SQLException, InvalidDataException {

        return applicationDAO.findByStudentId(studentId);
    }

    public boolean updateApplicationStatus(int applicationId, ApplicationStatus status, String feedback)
            throws RecordNotFoundException, SQLException {

        return applicationDAO.updateStatus(applicationId, status, feedback);
    }

    public boolean deleteApplication(int applicationId) throws RecordNotFoundException, SQLException {
        return applicationDAO.delete(applicationId);
    }

    public boolean deleteDrive(int driveId) throws RecordNotFoundException, SQLException {
        return driveDAO.delete(driveId);
    }

    public boolean deleteCompany(int companyId) throws RecordNotFoundException, SQLException {
        return companyDAO.delete(companyId);
    }
}
