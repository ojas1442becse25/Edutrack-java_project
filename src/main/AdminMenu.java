package main;

import exception.DuplicateRecordException;
import exception.InvalidDataException;
import exception.RecordNotFoundException;
import model.Application;
import model.Application.ApplicationStatus;
import model.AuditLog;
import model.Company;
import model.Company.CompanyStatus;
import model.PlacementDrive;
import model.PlacementDrive.DriveStatus;
import model.Student;
import model.Student.StudentStatus;
import model.Teacher;
import model.Teacher.TeacherStatus;
import service.AuditService;
import service.PlacementService;
import service.StudentService;
import service.TeacherService;
import util.InputValidator;
import util.MenuUtils;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

public class AdminMenu {

    private final PlacementService placementService;
    private final StudentService studentService;
    private final TeacherService teacherService;
    private final AuditService auditService;
    private final InputValidator validator;

    public AdminMenu(PlacementService placementService, StudentService studentService,
                     TeacherService teacherService, AuditService auditService,
                     InputValidator validator) {
        this.placementService = placementService;
        this.studentService = studentService;
        this.teacherService = teacherService;
        this.auditService = auditService;
        this.validator = validator;
    }

    public void run() {
        boolean exitMenu = false;
        while (!exitMenu) {
            MenuUtils.printHeader("UNIVERSITY & PLACEMENT ADMINISTRATION");
            System.out.println("1. Register Corporate Recruiting Partner (Company)");
            System.out.println("2. Create & Activate New Placement Drive");
            System.out.println("3. View Placement Drives & Review Applications");
            System.out.println("4. Update Student Application Status (Shortlist / Offer / Reject)");
            System.out.println("5. Inspect System Audit Trail (AUDIT_LOG)");
            System.out.println("6. Register New Student Record");
            System.out.println("7. Register New Faculty / Teacher Record");
            System.out.println("8. Return to Main Menu");

            int choice = validator.readInt("\nSelect administrative action [1-8]: ", 1, 8);
            switch (choice) {
                case 1: registerCompany(); break;
                case 2: createPlacementDrive(); break;
                case 3: viewDrivesAndApplications(); break;
                case 4: updateApplicationStatus(); break;
                case 5: viewAuditLogs(); break;
                case 6: registerStudent(); break;
                case 7: registerTeacher(); break;
                case 8: exitMenu = true; break;
            }
        }
    }

    private void registerCompany() {
        MenuUtils.printSubHeader("REGISTER RECRUITING COMPANY");
        try {
            String name = validator.readNonEmptyString("Company Name: ", 100);
            String industry = validator.readNonEmptyString("Industry Sector (e.g., FinTech, SaaS): ", 50);
            String website = validator.readOptionalString("Corporate Website URL", "https://example.com", 150);
            String contactPerson = validator.readNonEmptyString("Primary Contact Person: ", 100);
            String email = validator.readEmail("Contact Official Email: ");
            String phone = validator.readOptionalString("Contact Phone Number", "+91-9876543210", 20);
            String address = validator.readOptionalString("Corporate Office Address", "IT Park, Tech City", 255);

            Company comp = new Company(name, industry, website, contactPerson, email, phone, address, CompanyStatus.ACTIVE);
            Company registered = placementService.registerCompany(comp);
            System.out.println(MenuUtils.successBanner("Company registered successfully! Company ID #" + registered.getCompanyId()));
        } catch (DuplicateRecordException e) {
            System.out.println(MenuUtils.errorBanner(e.getMessage()));
        } catch (Exception e) {
            System.out.println(MenuUtils.errorBanner("Registration failed: " + e.getMessage()));
        }
    }

    private void createPlacementDrive() {
        MenuUtils.printSubHeader("CREATE & ACTIVATE PLACEMENT DRIVE");
        try {
            List<Company> companies = placementService.getAllCompanies();
            if (companies.isEmpty()) {
                System.out.println(MenuUtils.errorBanner("No companies registered yet. Register a company first."));
                return;
            }
            System.out.println("Available Companies:");
            for (Company c : companies) {
                System.out.printf("  [%d] %-30s | %s%n", c.getCompanyId(), c.getCompanyName(), c.getIndustry());
            }

            int companyId = validator.readInt("\nEnter Company ID: ");
            String role = validator.readNonEmptyString("Job Role Title: ", 100);
            String desc = validator.readOptionalString("Job Description", "Full stack backend development", 255);
            double lpa = validator.readDouble("Annual Package (LPA, e.g. 10.5): ", 1.0, 100.0);
            double minCgpa = validator.readDouble("Minimum CGPA Eligibility Cutoff [0-10]: ", 0.0, 10.0);
            String dept = validator.readOptionalString("Eligible Department(s)", "ALL", 100);
            LocalDate driveDate = validator.readDate("Placement Drive Date");
            LocalDate applyDeadline = validator.readDate("Last Date to Apply");
            int vacancies = validator.readInt("Total Open Vacancy Slots: ", 1, 500);

            PlacementDrive drive = new PlacementDrive(
                    companyId, role, desc, lpa, minCgpa, dept, driveDate, applyDeadline, DriveStatus.UPCOMING
            );

            PlacementDrive created = placementService.createPlacementDrive(drive, vacancies);
            System.out.println(MenuUtils.successBanner("Placement drive published! Assigned Drive ID #" + created.getDriveId() + " (Capacity: " + vacancies + " slots)"));
        } catch (Exception e) {
            System.out.println(MenuUtils.errorBanner("Failed to create drive: " + e.getMessage()));
        }
    }

    private void viewDrivesAndApplications() {
        MenuUtils.printSubHeader("VIEW DRIVES & CANDIDATE APPLICATIONS");
        try {
            List<PlacementDrive> drives = placementService.getAllDrives();
            if (drives.isEmpty()) {
                System.out.println(MenuUtils.infoBanner("No placement drives available."));
                return;
            }

            System.out.printf("%-8s | %-10s | %-28s | %-9s | %-9s | %-12s%n",
                    "DriveID", "CompanyID", "Job Role", "LPA", "Min CGPA", "Status");
            System.out.println("-".repeat(84));
            for (PlacementDrive d : drives) {
                System.out.printf("%-8d | %-10d | %-28s | %-9.2f | %-9.2f | %-12s%n",
                        d.getDriveId(), d.getCompanyId(), d.getJobTitle(), d.getPackageLpa(),
                        d.getMinCgpaCriteria(), d.getStatus());
            }

            int driveId = validator.readInt("\nEnter Drive ID to review student applications: ");
            List<Application> apps = placementService.getApplicationsByDrive(driveId);
            if (apps.isEmpty()) {
                System.out.println(MenuUtils.infoBanner("Zero applications submitted for Drive #" + driveId + "."));
                return;
            }

            System.out.printf("%-8s | %-10s | %-18s | %-28s%n",
                    "AppID", "StudentID", "Status", "Interview Notes");
            System.out.println("-".repeat(72));
            for (Application a : apps) {
                System.out.printf("%-8d | %-10d | %-18s | %-28s%n",
                        a.getApplicationId(), a.getStudentId(), a.getStatus(),
                        a.getInterviewFeedback() != null ? a.getInterviewFeedback() : "N/A");
            }
        } catch (Exception e) {
            System.out.println(MenuUtils.errorBanner("Error reading drives/applications: " + e.getMessage()));
        }
    }

    private void updateApplicationStatus() {
        MenuUtils.printSubHeader("UPDATE APPLICANT STATUS");
        try {
            int appId = validator.readInt("Enter Application ID: ");
            System.out.println("Select New Application Status:");
            System.out.println("1. SHORTLISTED\n2. TECHNICAL_ROUND\n3. HR_ROUND\n4. OFFERED\n5. REJECTED");
            int choice = validator.readInt("Select [1-5]: ", 1, 5);

            ApplicationStatus status = ApplicationStatus.values()[choice]; // Maps nicely
            String feedback = validator.readNonEmptyString("Enter Interview Stage Feedback / Remarks: ", 255);

            placementService.updateApplicationStatus(appId, status, feedback);
            System.out.println(MenuUtils.successBanner("Application #" + appId + " updated to " + status + "!"));
        } catch (Exception e) {
            System.out.println(MenuUtils.errorBanner("Update failed: " + e.getMessage()));
        }
    }

    private void viewAuditLogs() {
        MenuUtils.printSubHeader("SYSTEM AUDIT TRAIL LOGS (Last 25 Events)");
        try {
            List<AuditLog> logs = auditService.getRecentAuditLogs(25);
            if (logs.isEmpty()) {
                System.out.println(MenuUtils.infoBanner("Audit log table is empty."));
                return;
            }

            System.out.printf("%-6s | %-16s | %-8s | %-10s | %-18s | %-32s%n",
                    "LogID", "Entity", "EntityID", "Action", "Timestamp", "Details");
            System.out.println("-".repeat(100));
            for (AuditLog l : logs) {
                System.out.printf("%-6d | %-16s | %-8d | %-10s | %-18s | %-32s%n",
                        l.getLogId(), l.getEntityName(), l.getEntityId(), l.getActionType(),
                        l.getActionTimestamp().toLocalDate() + " " + l.getActionTimestamp().toLocalTime().toString().substring(0, 5),
                        l.getDetails());
            }
        } catch (Exception e) {
            System.out.println(MenuUtils.errorBanner("Failed to retrieve audit trail: " + e.getMessage()));
        }
    }

    private void registerStudent() {
        MenuUtils.printSubHeader("REGISTER NEW STUDENT RECORD");
        try {
            String roll = validator.readNonEmptyString("Student Roll Number: ", 30).toUpperCase();
            String fName = validator.readNonEmptyString("First Name: ", 50);
            String lName = validator.readNonEmptyString("Last Name: ", 50);
            String email = validator.readEmail("Student Email: ");
            String phone = validator.readOptionalString("Phone", "+91-9000000000", 20);
            String dept = validator.readNonEmptyString("Department (e.g. Computer Science): ", 50);
            int sem = validator.readInt("Semester [1-8]: ", 1, 8);
            double cgpa = validator.readDouble("CGPA [0.0 - 10.0]: ", 0.0, 10.0);
            int year = validator.readInt("Enrollment Year: ", 2020, 2030);

            Student student = new Student(roll, fName, lName, email, phone, dept, sem, cgpa, year, StudentStatus.ACTIVE);
            Student created = studentService.registerStudent(student);
            System.out.println(MenuUtils.successBanner("Student onboarded successfully! Student ID #" + created.getStudentId()));
        } catch (DuplicateRecordException e) {
            System.out.println(MenuUtils.errorBanner(e.getMessage()));
        } catch (Exception e) {
            System.out.println(MenuUtils.errorBanner("Registration failed: " + e.getMessage()));
        }
    }

    private void registerTeacher() {
        MenuUtils.printSubHeader("REGISTER NEW FACULTY MEMBER");
        try {
            String fName = validator.readNonEmptyString("First Name: ", 50);
            String lName = validator.readNonEmptyString("Last Name: ", 50);
            String email = validator.readEmail("Faculty Official Email: ");
            String phone = validator.readOptionalString("Phone", "+91-9111111111", 20);
            String dept = validator.readNonEmptyString("Department: ", 50);
            String desig = validator.readOptionalString("Designation", "Assistant Professor", 50);
            LocalDate hire = validator.readDate("Hire Date");

            Teacher teacher = new Teacher(fName, lName, email, phone, dept, desig, hire, TeacherStatus.ACTIVE);
            Teacher created = teacherService.registerTeacher(teacher);
            System.out.println(MenuUtils.successBanner("Faculty onboarded successfully! Faculty ID #" + created.getTeacherId()));
        } catch (DuplicateRecordException e) {
            System.out.println(MenuUtils.errorBanner(e.getMessage()));
        } catch (Exception e) {
            System.out.println(MenuUtils.errorBanner("Registration failed: " + e.getMessage()));
        }
    }
}
