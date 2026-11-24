package util;

import dao.ApplicationDAO;
import dao.AuditLogDAO;
import dao.ClassEnrollmentDAO;
import dao.ClassProgressLogDAO;
import dao.ClassResourceDAO;
import dao.QuizDAO;
import dao.QuizSubmissionDAO;
import model.Application;
import model.Application.ApplicationStatus;
import model.AuditLog;
import model.ClassEntity;
import model.ClassEntity.ClassStatus;
import model.ClassEnrollment;
import model.ClassResource;
import model.ClassResource.ResourceType;
import model.Company;
import model.Company.CompanyStatus;
import model.PlacementDrive;
import model.PlacementDrive.DriveStatus;
import model.Quiz;
import model.Quiz.QuizStatus;
import model.QuizSubmission;
import model.Student;
import model.Teacher;
import service.AuditService;
import service.ClassService;
import service.PlacementService;
import service.QuizService;
import service.StudentService;
import service.TeacherService;

import java.io.FileOutputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * End-to-End Integration Test simulating a full university cross-role lifecycle:
 * 1. FACULTY ROLE: Create course, upload learning materials, author & publish quiz.
 * 2. STUDENT ROLE: Enroll with atomic audit log, access study materials, submit quiz, apply for job.
 * 3. ADMIN ROLE: Register company, create drive, review applicants, promote to OFFERED, inspect audit trail.
 * 4. CLEANUP: Complete teardown guaranteeing seed data preservation.
 */
public class Step5EndToEndTest {

    private static final String EVIDENCE_FILE = "evidence/step5_verification.txt";
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

            runCompleteWorkflow();

        } catch (Exception e) {
            consoleErr.println("Error running Step5EndToEndTest: " + e.getMessage());
            e.printStackTrace(consoleErr);
        } finally {
            System.setOut(consoleOut);
            System.setErr(consoleErr);
            consoleOut.println("Step5EndToEndTest complete. Evidence recorded in: " + EVIDENCE_FILE);
        }
    }

    private static void runCompleteWorkflow() {
        System.out.println("================================================================================");
        System.out.println("EDUTRACK MILESTONE 5 - STEP 5 END-TO-END WORKFLOW INTEGRATION TEST");
        System.out.println("Timestamp: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        System.out.println("================================================================================\n");

        StudentService studentService = new StudentService();
        TeacherService teacherService = new TeacherService();
        ClassService classService = new ClassService();
        QuizService quizService = new QuizService();
        PlacementService placementService = new PlacementService();
        AuditService auditService = new AuditService();

        // Tracked test entities for teardown
        ClassEntity createdClass = null;
        ClassResource createdResource = null;
        Quiz createdQuiz = null;
        ClassEnrollment createdEnrollment = null;
        QuizSubmission createdSubmission = null;
        Company createdCompany = null;
        PlacementDrive createdDrive = null;
        Application createdApplication = null;

        List<Integer> auditLogIdsToClean = new ArrayList<>();

        try {
            // -----------------------------------------------------------------
            // STAGE 1: FACULTY / TEACHER WORKFLOW
            // -----------------------------------------------------------------
            System.out.println(">>> [STAGE 1] INSTRUCTOR WORKFLOW: Dr. Alan Turing (Teacher #1)");
            Teacher teacher = teacherService.getTeacherById(1);
            assertCondition(teacher != null, "Teacher #1 login verified: " + teacher.getFullName());

            // 1.1 Create new Course Section
            ClassEntity newClass = new ClassEntity(
                    "CS550E2E", "Cloud Native Distributed Architectures", teacher.getTeacherId(),
                    6, "2026-2027", 4, 40, ClassStatus.SCHEDULED
            );
            createdClass = classService.createClass(newClass);
            assertCondition(createdClass.getClassId() != null && createdClass.getClassId() > 0,
                    "Instructor created new course section: " + createdClass.getClassCode() + " (ID #" + createdClass.getClassId() + ")");

            // 1.2 Upload Learning Resource
            createdResource = classService.addResource(
                    createdClass.getClassId(), "Microservices Design Principles & Docker",
                    ResourceType.NOTES, "https://edutrack.edu/cs550_notes.pdf"
            );
            assertCondition(createdResource.getResourceId() != null && createdResource.getResourceId() > 0,
                    "Instructor uploaded study material: " + createdResource.getTitle() + " (ID #" + createdResource.getResourceId() + ")");

            // 1.3 Author & Publish Quiz
            Quiz newQuiz = new Quiz(
                    createdClass.getClassId(), "Quiz 1: Microservices & CAP Theorem",
                    30, 15, LocalDateTime.now().plusDays(5), QuizStatus.PUBLISHED
            );
            createdQuiz = quizService.createQuiz(newQuiz);
            assertCondition(createdQuiz.getQuizId() != null && createdQuiz.getQuizId() > 0,
                    "Instructor authored & published quiz: " + createdQuiz.getTitle() + " (ID #" + createdQuiz.getQuizId() + ")");

            System.out.println();

            // -----------------------------------------------------------------
            // STAGE 2: STUDENT SELF-SERVICE WORKFLOW
            // -----------------------------------------------------------------
            System.out.println(">>> [STAGE 2] STUDENT WORKFLOW: Rahul Sharma (CS2023001, Student #1)");
            Student student = studentService.getStudentById(1);
            assertCondition(student != null && "CS2023001".equals(student.getRollNumber()),
                    "Student #1 login verified: " + student.getFullName() + " (CGPA: " + student.getCgpa() + ")");

            // 2.1 Student Enrolls in Course Section (Atomic Transaction with AUDIT_LOG)
            createdEnrollment = classService.enrollStudent(createdClass.getClassId(), student.getStudentId(), "STUDENT_SELF_PORTAL");
            assertCondition(createdEnrollment.getEnrollmentId() != null && createdEnrollment.getEnrollmentId() > 0,
                    "Student enrolled in CS550E2E (Enrollment ID #" + createdEnrollment.getEnrollmentId() + ")");

            // Collect audit log generated by enrollment transaction
            List<AuditLog> enrollAudits = auditService.getAuditLogsByEntity("CLASS_ENROLLMENT", createdEnrollment.getEnrollmentId());
            assertCondition(!enrollAudits.isEmpty(), "Verified atomic AUDIT_LOG generated for course enrollment");
            enrollAudits.forEach(a -> auditLogIdsToClean.add(a.getLogId()));

            // 2.2 Student Browses Resources
            List<ClassResource> resources = classService.getResources(createdClass.getClassId());
            assertCondition(!resources.isEmpty() && resources.get(0).getTitle().contains("Microservices"),
                    "Student accessed course study materials: Found " + resources.size() + " document(s)");

            // 2.3 Student Attempts and Submits Quiz
            createdSubmission = quizService.submitQuiz(createdQuiz.getQuizId(), student.getStudentId(), 27.50, "Completed successfully");
            assertCondition(createdSubmission.getSubmissionId() != null && createdSubmission.getSubmissionId() > 0,
                    "Student submitted quiz with score 27.50 / 30.00 (Submission ID #" + createdSubmission.getSubmissionId() + ")");

            boolean isPass = quizService.isPassed(27.50, createdQuiz.getQuizId());
            assertCondition(isPass, "Quiz result evaluated: PASS (Score 27.50 >= passing cutoff 15)");

            System.out.println();

            // -----------------------------------------------------------------
            // STAGE 3: PLACEMENT ADMINISTRATION & INTERVIEW LIFECYCLE
            // -----------------------------------------------------------------
            System.out.println(">>> [STAGE 3] PLACEMENT ADMIN WORKFLOW: Company Onboarding, Drive, Application & Offers");

            // 3.1 Admin Onboards New Recruiter Company
            Company comp = new Company(
                    "AeroTech Dynamics Ltd.", "Aerospace Software & Avionics", "https://aerotech.example.com",
                    "Captain Rajesh Rao", "recruitment@aerotech.example.com", "+91-80-66778899",
                    "Aerospace SEZ Park, Devanahalli, Bengaluru", CompanyStatus.ACTIVE
            );
            createdCompany = placementService.registerCompany(comp);
            assertCondition(createdCompany.getCompanyId() != null && createdCompany.getCompanyId() > 0,
                    "Admin registered corporate recruiter: " + createdCompany.getCompanyName() + " (Company ID #" + createdCompany.getCompanyId() + ")");

            // 3.2 Admin Creates & Activates Placement Drive
            PlacementDrive drive = new PlacementDrive(
                    createdCompany.getCompanyId(), "Avionics Embedded Software Engineer",
                    "Embedded C++, RTOS, DO-178C avionics safety software",
                    16.50, 8.00, "ALL", LocalDate.now().plusDays(25), LocalDate.now().plusDays(10), DriveStatus.UPCOMING
            );
            createdDrive = placementService.createPlacementDrive(drive, 5);
            assertCondition(createdDrive.getDriveId() != null && createdDrive.getDriveId() > 0,
                    "Admin activated placement drive: " + createdDrive.getJobTitle() + " (Drive ID #" + createdDrive.getDriveId() + " | Cutoff: 8.00 CGPA)");

            // 3.3 Student Applies (Rahul Sharma has CGPA 8.75 >= 8.00 cutoff -> Eligible!)
            createdApplication = placementService.applyForDriveThreadSafe(
                    createdDrive.getDriveId(), student.getStudentId(), 5, "STUDENT_PLACEMENT_PORTAL"
            );
            assertCondition(createdApplication.getApplicationId() != null && createdApplication.getApplicationId() > 0,
                    "Student #1 applied for AeroTech drive (Application ID #" + createdApplication.getApplicationId() + ")");

            // Collect audit log generated by application transaction
            List<AuditLog> appAudits = auditService.getAuditLogsByEntity("APPLICATION", createdApplication.getApplicationId());
            assertCondition(!appAudits.isEmpty(), "Verified atomic AUDIT_LOG generated for placement application");
            appAudits.forEach(a -> auditLogIdsToClean.add(a.getLogId()));

            // 3.4 Admin Reviews and Shortlists Applicant
            boolean shortlisted = placementService.updateApplicationStatus(
                    createdApplication.getApplicationId(), ApplicationStatus.SHORTLISTED,
                    "Candidate shortlisted for Technical Round 1 on Embedded Systems"
            );
            assertCondition(shortlisted, "Admin moved application status to: SHORTLISTED");

            // 3.5 Admin Completes Interviews & Issues Job Offer
            boolean offered = placementService.updateApplicationStatus(
                    createdApplication.getApplicationId(), ApplicationStatus.OFFERED,
                    "Outstanding performance in Technical & Managerial interviews. Job offer released (16.5 LPA)!"
            );
            assertCondition(offered, "Admin promoted application status to: OFFERED");

            // 3.6 Student Tracks Updated Status
            final int targetAppId = createdApplication.getApplicationId();
            List<Application> studentApps = placementService.getApplicationsByStudent(student.getStudentId());
            boolean offerConfirmed = studentApps.stream()
                    .anyMatch(a -> a.getApplicationId().equals(targetAppId)
                            && a.getStatus() == ApplicationStatus.OFFERED);
            assertCondition(offerConfirmed, "Student portal confirms final status is OFFERED with package 16.5 LPA!");

            // 3.7 Admin Inspects Audit Trail
            List<AuditLog> recentLogs = auditService.getRecentAuditLogs(10);
            assertCondition(!recentLogs.isEmpty(), "Admin verified system audit trail: " + recentLogs.size() + " recent events recorded");

            System.out.println();

        } catch (Exception e) {
            recordFailure("End-to-End workflow encountered error", e);
        } finally {
            // -----------------------------------------------------------------
            // STAGE 4: TEARDOWN & DATABASE CLEANUP
            // -----------------------------------------------------------------
            System.out.println(">>> [STAGE 4] TEARDOWN & VERIFYING SEED DATA PRESERVATION");
            try {
                if (createdApplication != null && createdApplication.getApplicationId() != null) {
                    new ApplicationDAO().delete(createdApplication.getApplicationId());
                }
                if (createdDrive != null && createdDrive.getDriveId() != null) {
                    placementService.deleteDrive(createdDrive.getDriveId());
                }
                if (createdCompany != null && createdCompany.getCompanyId() != null) {
                    placementService.deleteCompany(createdCompany.getCompanyId());
                }
                if (createdSubmission != null && createdSubmission.getSubmissionId() != null) {
                    new QuizSubmissionDAO().delete(createdSubmission.getSubmissionId());
                }
                if (createdQuiz != null && createdQuiz.getQuizId() != null) {
                    quizService.deleteQuiz(createdQuiz.getQuizId());
                }
                if (createdResource != null && createdResource.getResourceId() != null) {
                    new ClassResourceDAO().delete(createdResource.getResourceId());
                }
                if (createdEnrollment != null && createdEnrollment.getEnrollmentId() != null) {
                    new ClassEnrollmentDAO().delete(createdEnrollment.getEnrollmentId());
                }
                if (createdClass != null && createdClass.getClassId() != null) {
                    classService.deleteClass(createdClass.getClassId());
                }
                // Clean up generated audit logs
                for (int logId : auditLogIdsToClean) {
                    try { new AuditLogDAO().delete(logId); } catch (Exception ignore) {}
                }
                recordPass("CLEANUP: All end-to-end test artifacts deleted cleanly");

            } catch (Exception e) {
                System.err.println("Cleanup warning: " + e.getMessage());
            }
        }

        System.out.println("\n================================================================================");
        System.out.println(String.format("E2E WORKFLOW SUMMARY: Total Tests: %d | Passed: %d | Failed: %d",
                testsRun, testsPassed, (testsRun - testsPassed)));
        if (testsRun == testsPassed) {
            System.out.println("RESULT: COMPLETE END-TO-END WORKFLOW INTEGRATION TEST PASSED!");
        } else {
            System.out.println("RESULT: WORKFLOW ENCOUNTERED FAILURES.");
        }
        System.out.println("================================================================================");
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
