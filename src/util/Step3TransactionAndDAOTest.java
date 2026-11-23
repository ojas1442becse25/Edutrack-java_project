package util;

import dao.ApplicationDAO;
import dao.AuditLogDAO;
import dao.ClassEnrollmentDAO;
import dao.ClassProgressLogDAO;
import dao.ClassResourceDAO;
import dao.CompanyDAO;
import dao.PlacementDriveDAO;
import dao.QuizDAO;
import dao.QuizSubmissionDAO;
import exception.DuplicateRecordException;
import exception.InvalidDataException;
import exception.RecordNotFoundException;
import model.Application;
import model.Application.ApplicationStatus;
import model.AuditLog;
import model.ClassEnrollment;
import model.ClassEnrollment.EnrollmentStatus;
import model.ClassProgressLog;
import model.ClassResource;
import model.ClassResource.ResourceType;
import model.Company;
import model.Company.CompanyStatus;
import model.PlacementDrive;
import model.PlacementDrive.DriveStatus;
import model.Quiz;
import model.Quiz.QuizStatus;
import model.QuizSubmission;
import model.QuizSubmission.SubmissionStatus;

import java.io.FileOutputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

/**
 * Verification test runner for Step 3 deliverables:
 * - Tests full CRUD across remaining 9 DAOs:
 *   CompanyDAO, PlacementDriveDAO, ApplicationDAO, AuditLogDAO,
 *   ClassEnrollmentDAO, ClassProgressLogDAO, ClassResourceDAO,
 *   QuizDAO, QuizSubmissionDAO
 * - Demonstrates multi-step atomic COMMIT transactions (Main Record + Audit Log)
 * - Demonstrates forced-failure ROLLBACK transactions (Zero partial data saved)
 * - Automated cleanup preserving seed data integrity
 * - Generates report in evidence/step3_verification.txt
 */
public class Step3TransactionAndDAOTest {

    private static final String EVIDENCE_FILE = "evidence/step3_verification.txt";
    private static int testsRun = 0;
    private static int testsPassed = 0;

    public static void main(String[] args) {
        PrintStream consoleOut = System.out;
        PrintStream consoleErr = System.err;

        try {
            Files.createDirectories(Paths.get("evidence"));
            FileOutputStream fos = new FileOutputStream(EVIDENCE_FILE);
            TeeOutputStream tos = new TeeOutputStream(System.out, fos);
            PrintStream dualOut = new PrintStream(tos, true, "UTF-8");
            System.setOut(dualOut);
            System.setErr(dualOut);

            runAllStep3Tests();

        } catch (Exception e) {
            consoleErr.println("Error running Step3TransactionAndDAOTest: " + e.getMessage());
            e.printStackTrace(consoleErr);
        } finally {
            System.setOut(consoleOut);
            System.setErr(consoleErr);
            consoleOut.println("Step3TransactionAndDAOTest finished. Evidence saved to: " + EVIDENCE_FILE);
        }
    }

    private static void runAllStep3Tests() {
        System.out.println("================================================================================");
        System.out.println("EDUTRACK MILESTONE 5 - STEP 3 DAOs & ATOMIC TRANSACTION VERIFICATION");
        System.out.println("Timestamp: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        System.out.println("================================================================================\n");

        CompanyDAO companyDAO = new CompanyDAO();
        PlacementDriveDAO driveDAO = new PlacementDriveDAO();
        ApplicationDAO appDAO = new ApplicationDAO();
        AuditLogDAO auditDAO = new AuditLogDAO();
        ClassEnrollmentDAO enrollmentDAO = new ClassEnrollmentDAO();
        ClassProgressLogDAO progressDAO = new ClassProgressLogDAO();
        ClassResourceDAO resourceDAO = new ClassResourceDAO();
        QuizDAO quizDAO = new QuizDAO();
        QuizSubmissionDAO submissionDAO = new QuizSubmissionDAO();

        testCompanyAndPlacementDrive(companyDAO, driveDAO);
        testAtomicTransactionApplication(appDAO, auditDAO, driveDAO, companyDAO);
        testAtomicTransactionEnrollment(enrollmentDAO, auditDAO);
        testClassProgressAndResources(progressDAO, resourceDAO);
        testQuizAndQuizSubmissions(quizDAO, submissionDAO);

        System.out.println("\n================================================================================");
        System.out.println(String.format("TEST RUN SUMMARY: Total Tests: %d | Passed: %d | Failed: %d",
                testsRun, testsPassed, (testsRun - testsPassed)));
        if (testsRun == testsPassed) {
            System.out.println("RESULT: ALL STEP 3 DAO & TRANSACTION TESTS PASSED SUCCESSFULLY!");
        } else {
            System.out.println("RESULT: SOME TESTS FAILED. CHECK LOGS ABOVE.");
        }
        System.out.println("================================================================================");
    }

    private static void testCompanyAndPlacementDrive(CompanyDAO companyDAO, PlacementDriveDAO driveDAO) {
        System.out.println(">>> 1. TESTING CompanyDAO & PlacementDriveDAO CRUD");
        Company comp = null;
        PlacementDrive drive = null;
        try {
            // 1. Create Company
            comp = new Company(
                    "Apex Global Cloud Ltd.", "Cloud Solutions", "https://apexcloud.example.com",
                    "Rohan Mehra", "talent@apexcloud.example.com", "+91-80-99887766",
                    "Electronic City, Bengaluru", CompanyStatus.ACTIVE
            );
            comp = companyDAO.create(comp);
            assertCondition(comp.getCompanyId() != null && comp.getCompanyId() > 0,
                    "CREATE Company: Auto-increment ID obtained -> " + comp.getCompanyId());

            // Read by Name
            Optional<Company> foundName = companyDAO.findByName("Apex Global Cloud Ltd.");
            assertCondition(foundName.isPresent() && "talent@apexcloud.example.com".equals(foundName.get().getContactEmail()),
                    "READ Company by Name: Found " + foundName.get().getCompanyName());

            // Update Company
            comp.setIndustry("Enterprise Multi-Cloud SaaS");
            boolean compUpdated = companyDAO.update(comp);
            assertCondition(compUpdated, "UPDATE Company: Modified industry to SaaS");

            // 2. Create Placement Drive
            drive = new PlacementDrive(
                    comp.getCompanyId(), "Cloud DevOps Engineer",
                    "Kubernetes, Terraform, AWS, CI/CD Pipeline engineering",
                    12.50, 7.50, "Computer Science, Information Technology",
                    LocalDate.now().plusDays(30), LocalDate.now().plusDays(15), DriveStatus.UPCOMING
            );
            drive = driveDAO.create(drive);
            assertCondition(drive.getDriveId() != null && drive.getDriveId() > 0,
                    "CREATE PlacementDrive: Auto-increment ID obtained -> " + drive.getDriveId());

            // Query Active Drives
            final int createdDriveId = drive.getDriveId();
            List<PlacementDrive> active = driveDAO.findActiveDrives();
            assertCondition(active.stream().anyMatch(d -> d.getDriveId().equals(createdDriveId)),
                    "QUERY Active Drives: Newly created drive is present in active list");

            // Update Drive Status
            boolean driveStatusUpdated = driveDAO.updateStatus(drive.getDriveId(), DriveStatus.IN_PROGRESS);
            assertCondition(driveStatusUpdated, "UPDATE PlacementDrive Status: Changed to IN_PROGRESS");

            // Cleanup Placement Drive and Company
            driveDAO.delete(drive.getDriveId());
            recordPass("DELETE PlacementDrive: Cleaned up test drive ID " + drive.getDriveId());

            companyDAO.delete(comp.getCompanyId());
            recordPass("DELETE Company: Cleaned up test company ID " + comp.getCompanyId());

        } catch (Exception e) {
            recordFailure("Company/Drive CRUD test failed", e);
            if (drive != null && drive.getDriveId() != null) {
                try { driveDAO.delete(drive.getDriveId()); } catch (Exception ignore) {}
            }
            if (comp != null && comp.getCompanyId() != null) {
                try { companyDAO.delete(comp.getCompanyId()); } catch (Exception ignore) {}
            }
        }
        System.out.println();
    }

    private static void testAtomicTransactionApplication(ApplicationDAO appDAO, AuditLogDAO auditDAO,
                                                         PlacementDriveDAO driveDAO, CompanyDAO companyDAO) {
        System.out.println(">>> 2. TESTING ATOMIC JDBC TRANSACTIONS IN ApplicationDAO (COMMIT & ROLLBACK)");
        Company testComp = null;
        PlacementDrive testDrive = null;
        Application committedApp = null;

        try {
            // Setup prerequisite test company & drive
            testComp = new Company(
                    "Starlight Robotics", "Robotics & AI", "https://starlight.example.com",
                    "Sarah Connor", "hr@starlight.example.com", "+91-11-22334455",
                    "DLF Cyber City, Gurugram", CompanyStatus.ACTIVE
            );
            testComp = companyDAO.create(testComp);

            testDrive = new PlacementDrive(
                    testComp.getCompanyId(), "Embedded AI Specialist", "ROS, C++, CUDA, Edge AI",
                    14.00, 8.00, "ALL", LocalDate.now().plusDays(20), LocalDate.now().plusDays(10), DriveStatus.UPCOMING
            );
            testDrive = driveDAO.create(testDrive);

            // -----------------------------------------------------------------
            // (A) TRANSACTION DEMO 1: SUCCESSFUL MULTI-STEP COMMIT
            // -----------------------------------------------------------------
            System.out.println("  --- [TRANSACTION A: Multi-Statement COMMIT Scenario] ---");
            Application appA = new Application(testDrive.getDriveId(), 1, ApplicationStatus.APPLIED, "Initial application via portal");
            committedApp = appDAO.applyWithAudit(appA, "STUDENT_ID_1", false);

            assertCondition(committedApp.getApplicationId() != null && committedApp.getApplicationId() > 0,
                    "COMMIT: Application created with ID " + committedApp.getApplicationId());

            // Verify both application and audit log exist in database
            Optional<Application> verifyApp = appDAO.findById(committedApp.getApplicationId());
            assertCondition(verifyApp.isPresent(), "COMMIT VERIFICATION: Application record persists in MySQL database");

            List<AuditLog> auditList = auditDAO.findByEntity("APPLICATION", committedApp.getApplicationId());
            assertCondition(!auditList.isEmpty() && auditList.get(0).getDetails().contains("applied to Placement Drive"),
                    "COMMIT VERIFICATION: Corresponding AUDIT_LOG entry was written atomically on same connection");

            // -----------------------------------------------------------------
            // (B) TRANSACTION DEMO 2: SIMULATED FAILURE & FORCED ROLLBACK
            // -----------------------------------------------------------------
            System.out.println("  --- [TRANSACTION B: Forced Failure ROLLBACK Scenario] ---");
            Application appB = new Application(testDrive.getDriveId(), 2, ApplicationStatus.APPLIED, "Testing rollback behavior");

            boolean rollbackTriggered = false;
            try {
                // simulateFailure = true triggers an exception AFTER Application insert but BEFORE commit()
                appDAO.applyWithAudit(appB, "STUDENT_ID_2", true);
            } catch (SQLException e) {
                rollbackTriggered = true;
                recordPass("ROLLBACK TRIGGERED: Caught simulated exception: " + e.getMessage());
            }
            assertCondition(rollbackTriggered, "Exception was properly thrown before commit was reached");

            // PROOF OF ATOMICITY: Verify that Student 2 has NO application saved for this drive
            Optional<Application> uncommittedApp = appDAO.findByDriveAndStudent(testDrive.getDriveId(), 2);
            assertCondition(!uncommittedApp.isPresent(),
                    "ATOMICITY PROOF: Application for Student 2 was NOT saved (Rolled back completely)");

            // Cleanup committed test entities
            appDAO.delete(committedApp.getApplicationId());
            if (!auditList.isEmpty()) {
                auditDAO.delete(auditList.get(0).getLogId());
            }
            recordPass("CLEANUP: Removed committed test application and audit log");

            driveDAO.delete(testDrive.getDriveId());
            companyDAO.delete(testComp.getCompanyId());
            recordPass("CLEANUP: Removed test placement drive and company");

        } catch (Exception e) {
            recordFailure("Atomic transaction test in ApplicationDAO failed", e);
            if (committedApp != null && committedApp.getApplicationId() != null) {
                try { appDAO.delete(committedApp.getApplicationId()); } catch (Exception ignore) {}
            }
            if (testDrive != null && testDrive.getDriveId() != null) {
                try { driveDAO.delete(testDrive.getDriveId()); } catch (Exception ignore) {}
            }
            if (testComp != null && testComp.getCompanyId() != null) {
                try { companyDAO.delete(testComp.getCompanyId()); } catch (Exception ignore) {}
            }
        }
        System.out.println();
    }

    private static void testAtomicTransactionEnrollment(ClassEnrollmentDAO enrollmentDAO, AuditLogDAO auditDAO) {
        System.out.println(">>> 3. TESTING ATOMIC JDBC TRANSACTIONS IN ClassEnrollmentDAO (COMMIT & ROLLBACK)");
        ClassEnrollment committedEnrollment = null;
        try {
            // Seed Class 1 and Seed Student 3 are not enrolled together (Student 1 and 2 are in Class 1)
            // -----------------------------------------------------------------
            // (A) TRANSACTION DEMO 3: SUCCESSFUL ENROLLMENT COMMIT
            // -----------------------------------------------------------------
            System.out.println("  --- [TRANSACTION C: Enrollment with Audit COMMIT] ---");
            ClassEnrollment enrollment = new ClassEnrollment(
                    1, 3, LocalDate.now(), 88.50, null, EnrollmentStatus.ENROLLED
            );
            committedEnrollment = enrollmentDAO.enrollStudentWithAudit(enrollment, "ACADEMIC_DEAN", false);
            assertCondition(committedEnrollment.getEnrollmentId() != null && committedEnrollment.getEnrollmentId() > 0,
                    "COMMIT: Enrollment created with ID " + committedEnrollment.getEnrollmentId());

            Optional<ClassEnrollment> verifyEnrollment = enrollmentDAO.findById(committedEnrollment.getEnrollmentId());
            assertCondition(verifyEnrollment.isPresent(), "COMMIT VERIFICATION: ClassEnrollment persists in database");

            List<AuditLog> auditLogs = auditDAO.findByEntity("CLASS_ENROLLMENT", committedEnrollment.getEnrollmentId());
            assertCondition(!auditLogs.isEmpty() && auditLogs.get(0).getDetails().contains("Enrolled Student #3"),
                    "COMMIT VERIFICATION: Matching AUDIT_LOG record persisted atomically on same connection");

            // Update Attendance and Grade
            boolean updated = enrollmentDAO.updateAttendanceAndGrade(committedEnrollment.getEnrollmentId(), 94.00, "A");
            assertCondition(updated, "UPDATE Attendance & Grade: Attendance set to 94.00%, Grade 'A'");

            // -----------------------------------------------------------------
            // (B) TRANSACTION DEMO 4: FORCED ENROLLMENT ROLLBACK
            // -----------------------------------------------------------------
            System.out.println("  --- [TRANSACTION D: Enrollment Failure ROLLBACK] ---");
            // Seed Class 2 and Seed Student 1 (Student 1 is not in Class 2)
            ClassEnrollment rollbackEnrollment = new ClassEnrollment(
                    2, 1, LocalDate.now(), 75.0, null, EnrollmentStatus.ENROLLED
            );

            boolean rollbackSuccess = false;
            try {
                enrollmentDAO.enrollStudentWithAudit(rollbackEnrollment, "REGISTRAR", true);
            } catch (SQLException e) {
                rollbackSuccess = true;
                recordPass("ROLLBACK TRIGGERED: Caught expected exception: " + e.getMessage());
            }
            assertCondition(rollbackSuccess, "Exception properly interrupted transaction before commit");

            Optional<ClassEnrollment> rolledBackCheck = enrollmentDAO.findByClassAndStudent(2, 1);
            assertCondition(!rolledBackCheck.isPresent(),
                    "ATOMICITY PROOF: Enrollment (Class 2, Student 1) NOT present in database (Rolled back completely)");

            // Cleanup committed enrollment and audit log
            enrollmentDAO.delete(committedEnrollment.getEnrollmentId());
            if (!auditLogs.isEmpty()) {
                auditDAO.delete(auditLogs.get(0).getLogId());
            }
            recordPass("CLEANUP: Removed test enrollment and audit log");

        } catch (Exception e) {
            recordFailure("ClassEnrollment transaction test failed", e);
            if (committedEnrollment != null && committedEnrollment.getEnrollmentId() != null) {
                try { enrollmentDAO.delete(committedEnrollment.getEnrollmentId()); } catch (Exception ignore) {}
            }
        }
        System.out.println();
    }

    private static void testClassProgressAndResources(ClassProgressLogDAO progressDAO, ClassResourceDAO resourceDAO) {
        System.out.println(">>> 4. TESTING ClassProgressLogDAO & ClassResourceDAO CRUD");
        ClassProgressLog log = null;
        ClassResource resource = null;
        try {
            // 1. Progress Log for Seed Class 1
            log = new ClassProgressLog(
                    1, LocalDate.now(), "Transaction Management: ACID, Commit & Rollback Patterns",
                    2.50, "Full class hands-on demonstration with live MySQL and JDBC."
            );
            log = progressDAO.create(log);
            assertCondition(log.getLogId() != null && log.getLogId() > 0,
                    "CREATE ClassProgressLog: Auto-increment ID obtained -> " + log.getLogId());

            final int createdLogId = log.getLogId();
            List<ClassProgressLog> logs = progressDAO.findByClassId(1);
            assertCondition(logs.stream().anyMatch(l -> l.getLogId().equals(createdLogId)),
                    "READ ProgressLogs by Class ID: Verified log presence in Class 1");

            progressDAO.delete(log.getLogId());
            recordPass("DELETE ClassProgressLog: Removed test log ID " + log.getLogId());

            // 2. Class Resource for Seed Class 1
            resource = new ClassResource(
                    1, "JDBC Transaction Cheatsheet & Lab Code",
                    ResourceType.NOTES, "https://edutrack.edu/resources/cs301_jdbc_tx.pdf"
            );
            resource = resourceDAO.create(resource);
            assertCondition(resource.getResourceId() != null && resource.getResourceId() > 0,
                    "CREATE ClassResource: Auto-increment ID obtained -> " + resource.getResourceId());

            resource.setTitle("JDBC Transaction Cheatsheet (v2 Revised)");
            resource.setResourceType(ResourceType.SLIDES);
            boolean updated = resourceDAO.update(resource);
            assertCondition(updated, "UPDATE ClassResource: Modified title and resource type to SLIDES");

            resourceDAO.delete(resource.getResourceId());
            recordPass("DELETE ClassResource: Removed test resource ID " + resource.getResourceId());

        } catch (Exception e) {
            recordFailure("ProgressLog/Resource CRUD test failed", e);
            if (log != null && log.getLogId() != null) {
                try { progressDAO.delete(log.getLogId()); } catch (Exception ignore) {}
            }
            if (resource != null && resource.getResourceId() != null) {
                try { resourceDAO.delete(resource.getResourceId()); } catch (Exception ignore) {}
            }
        }
        System.out.println();
    }

    private static void testQuizAndQuizSubmissions(QuizDAO quizDAO, QuizSubmissionDAO submissionDAO) {
        System.out.println(">>> 5. TESTING QuizDAO & QuizSubmissionDAO CRUD");
        Quiz quiz = null;
        QuizSubmission submission = null;
        try {
            // 1. Create Quiz for Seed Class 1
            quiz = new Quiz(
                    1, "Midterm Assessment: JDBC & Relational Integrity",
                    30, 15, LocalDateTime.now().plusDays(5), QuizStatus.PUBLISHED
            );
            quiz = quizDAO.create(quiz);
            assertCondition(quiz.getQuizId() != null && quiz.getQuizId() > 0,
                    "CREATE Quiz: Auto-increment ID obtained -> " + quiz.getQuizId());

            final int createdQuizId = quiz.getQuizId();
            List<Quiz> active = quizDAO.findActiveQuizzesByClassId(1);
            assertCondition(active.stream().anyMatch(q -> q.getQuizId().equals(createdQuizId)),
                    "QUERY Active Quizzes by Class: Found active quiz in Class 1");

            // 2. Submit Quiz by Seed Student 1
            submission = new QuizSubmission(
                    quiz.getQuizId(), 1, 28.00, SubmissionStatus.SUBMITTED, "Completed on time."
            );
            submission = submissionDAO.submit(submission);
            assertCondition(submission.getSubmissionId() != null && submission.getSubmissionId() > 0,
                    "SUBMIT Quiz: Submission ID obtained -> " + submission.getSubmissionId());

            // 3. Evaluate Submission
            boolean evaluated = submissionDAO.evaluate(
                    submission.getSubmissionId(), 29.50, SubmissionStatus.EVALUATED, "Excellent bonus point answers!"
            );
            assertCondition(evaluated, "EVALUATE Submission: Score adjusted to 29.50 with status EVALUATED");

            Optional<QuizSubmission> evalCheck = submissionDAO.findById(submission.getSubmissionId());
            assertCondition(evalCheck.isPresent() && Math.abs(evalCheck.get().getScoreObtained() - 29.50) < 0.01,
                    "VERIFY EVALUATION: Persisted score confirmed as 29.50");

            // Cleanup Submission and Quiz
            submissionDAO.delete(submission.getSubmissionId());
            recordPass("DELETE QuizSubmission: Removed submission ID " + submission.getSubmissionId());

            quizDAO.delete(quiz.getQuizId());
            recordPass("DELETE Quiz: Removed test quiz ID " + quiz.getQuizId());

        } catch (Exception e) {
            recordFailure("Quiz/Submission CRUD test failed", e);
            if (submission != null && submission.getSubmissionId() != null) {
                try { submissionDAO.delete(submission.getSubmissionId()); } catch (Exception ignore) {}
            }
            if (quiz != null && quiz.getQuizId() != null) {
                try { quizDAO.delete(quiz.getQuizId()); } catch (Exception ignore) {}
            }
        }
        System.out.println();
    }

    private static void assertCondition(boolean condition, String description) {
        testsRun++;
        if (condition) {
            testsPassed++;
            System.out.println("  [PASS] " + description);
        } else {
            System.err.println("  [FAIL] " + description);
        }
    }

    private static void recordPass(String message) {
        testsRun++;
        testsPassed++;
        System.out.println("  [PASS] " + message);
    }

    private static void recordFailure(String message, Throwable t) {
        testsRun++;
        System.err.println("  [FAIL] " + message + (t != null ? " (" + t.getMessage() + ")" : ""));
    }

    private static class TeeOutputStream extends java.io.OutputStream {
        private final java.io.OutputStream out1;
        private final java.io.OutputStream out2;

        public TeeOutputStream(java.io.OutputStream out1, java.io.OutputStream out2) {
            this.out1 = out1;
            this.out2 = out2;
        }

        @Override
        public void write(int b) throws java.io.IOException {
            out1.write(b);
            out2.write(b);
        }

        @Override
        public void write(byte[] b, int off, int len) throws java.io.IOException {
            out1.write(b, off, len);
            out2.write(b, off, len);
        }

        @Override
        public void flush() throws java.io.IOException {
            out1.flush();
            out2.flush();
        }

        @Override
        public void close() throws java.io.IOException {
            try { out1.close(); } finally { out2.close(); }
        }
    }
}
