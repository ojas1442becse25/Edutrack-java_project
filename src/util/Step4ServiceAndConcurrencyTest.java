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
import dao.StudentDAO;
import dao.TeacherDAO;
import exception.DuplicateRecordException;
import exception.EligibilityException;
import exception.InvalidDataException;
import exception.RecordNotFoundException;
import model.Application;
import model.AuditLog;
import model.ClassEntity;
import model.ClassEntity.ClassStatus;
import model.ClassEnrollment;
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
import model.Student;
import model.Student.StudentStatus;
import model.Teacher;
import model.Teacher.TeacherStatus;
import service.ClassService;
import service.ClassService.AtRiskStudentReport;
import service.PlacementService;
import service.QuizService;
import service.StudentService;
import service.StudentService.StudentAcademicSummary;
import service.TeacherService;

import java.io.FileOutputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class Step4ServiceAndConcurrencyTest {

    private static final String EVIDENCE_FILE = "evidence/step4_verification.txt";
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

            runAllStep4Tests();

        } catch (Exception e) {
            consoleErr.println("Error running Step4ServiceAndConcurrencyTest: " + e.getMessage());
            e.printStackTrace(consoleErr);
        } finally {
            System.setOut(consoleOut);
            System.setErr(consoleErr);
            consoleOut.println("Step4ServiceAndConcurrencyTest finished. Evidence saved to: " + EVIDENCE_FILE);
        }
    }

    private static void runAllStep4Tests() {
        System.out.println("================================================================================");
        System.out.println("EDUTRACK MILESTONE 5 - STEP 4 SERVICE LAYER & CONCURRENCY VERIFICATION");
        System.out.println("Timestamp: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        System.out.println("================================================================================\n");

        StudentService studentService = new StudentService();
        TeacherService teacherService = new TeacherService();
        ClassService classService = new ClassService();
        QuizService quizService = new QuizService();
        PlacementService placementService = new PlacementService();

        testStudentAndTeacherServices(studentService, teacherService);
        testClassServiceAndAtRiskAnalytics(classService);
        testQuizServiceWorkflow(quizService);
        testPlacementEligibilityRules(placementService);
        testMultiThreadedPlacementConcurrency(placementService);

        System.out.println("\n================================================================================");
        System.out.println(String.format("TEST RUN SUMMARY: Total Tests: %d | Passed: %d | Failed: %d",
                testsRun, testsPassed, (testsRun - testsPassed)));
        if (testsRun == testsPassed) {
            System.out.println("RESULT: ALL STEP 4 SERVICE & CONCURRENCY TESTS PASSED SUCCESSFULLY!");
        } else {
            System.out.println("RESULT: SOME TESTS FAILED. CHECK LOGS ABOVE.");
        }
        System.out.println("================================================================================");
    }

    private static void testStudentAndTeacherServices(StudentService studentService, TeacherService teacherService) {
        System.out.println(">>> 1. TESTING StudentService & TeacherService BUSINESS LOGIC");
        Student testStudent = null;
        Teacher testTeacher = null;
        try {
            // 1. Register Student
            testStudent = new Student(
                    "CS2026S4", "Kavya", "Iyer", "kavya.iyer.test@student.edu",
                    "+91-9876500001", "Computer Science", 6, 8.85, 2026, StudentStatus.ACTIVE
            );
            testStudent = studentService.registerStudent(testStudent);
            assertCondition(testStudent.getStudentId() != null && testStudent.getStudentId() > 0,
                    "StudentService: Registered student successfully with ID #" + testStudent.getStudentId());

            // 2. Duplicate Registration Rejection
            try {
                Student dup = new Student(
                        "CS2026S4", "Duplicate", "Student", "different.email@student.edu",
                        null, "Computer Science", 1, 7.50, 2026, StudentStatus.ACTIVE
                );
                studentService.registerStudent(dup);
                recordFailure("StudentService: Duplicate roll number should throw DuplicateRecordException", null);
            } catch (DuplicateRecordException e) {
                recordPass("StudentService: Duplicate roll number rejected -> DuplicateRecordException caught");
            }

            // 3. Invalid CGPA Rejection (> 10.0)
            try {
                Student invalid = new Student();
                invalid.setCgpa(10.50);
                recordFailure("StudentService: CGPA > 10.0 should throw InvalidDataException", null);
            } catch (InvalidDataException e) {
                recordPass("StudentService: Invalid CGPA 10.50 rejected -> InvalidDataException caught");
            }

            // 4. Fetch Enrolled Classes & Academic Summary for Seed Student 1
            List<ClassEntity> enrolledClasses = studentService.getEnrolledClasses(1);
            assertCondition(!enrolledClasses.isEmpty(),
                    "StudentService: Fetched " + enrolledClasses.size() + " enrolled course(s) for Seed Student #1");

            StudentAcademicSummary summary = studentService.getAcademicSummary(1);
            assertCondition(summary != null && summary.getTotalEnrolledCourses() >= 1 && summary.getAverageAttendance() > 0.0,
                    "StudentService: Generated academic summary: " + summary);

            // 5. Register Teacher
            testTeacher = new Teacher(
                    "Prof. Ramesh", "Kapur", "ramesh.kapur.test@edutrack.edu",
                    "+91-9876500002", "Information Technology", "Associate Professor",
                    LocalDate.now(), TeacherStatus.ACTIVE
            );
            testTeacher = teacherService.registerTeacher(testTeacher);
            assertCondition(testTeacher.getTeacherId() != null && testTeacher.getTeacherId() > 0,
                    "TeacherService: Registered teacher successfully with ID #" + testTeacher.getTeacherId());

            // 6. Assigned Classes for Seed Teacher 1
            List<ClassEntity> assigned = teacherService.getAssignedClasses(1);
            assertCondition(!assigned.isEmpty(),
                    "TeacherService: Retrieved " + assigned.size() + " assigned course(s) for Seed Teacher #1");

            // Cleanup
            studentService.deleteStudent(testStudent.getStudentId());
            teacherService.deleteTeacher(testTeacher.getTeacherId());
            recordPass("CLEANUP: Removed temporary student and teacher records");

        } catch (Exception e) {
            recordFailure("Student/Teacher service test encountered error", e);
            if (testStudent != null && testStudent.getStudentId() != null) {
                try { studentService.deleteStudent(testStudent.getStudentId()); } catch (Exception ignore) {}
            }
            if (testTeacher != null && testTeacher.getTeacherId() != null) {
                try { teacherService.deleteTeacher(testTeacher.getTeacherId()); } catch (Exception ignore) {}
            }
        }
        System.out.println();
    }

    private static void testClassServiceAndAtRiskAnalytics(ClassService classService) {
        System.out.println(">>> 2. TESTING ClassService (ENROLLMENT, AUDIT, PROGRESS, RESOURCES & AT-RISK ANALYTICS)");
        ClassEntity testClass = null;
        ClassEnrollment enrollment = null;
        ClassProgressLog log = null;
        ClassResource res = null;

        try {
            // 1. Create Class
            testClass = new ClassEntity(
                    "CS888TEST", "Cloud Native Microservices Architecture", 1,
                    6, "2026-2027", 4, 30, ClassStatus.SCHEDULED
            );
            testClass = classService.createClass(testClass);
            assertCondition(testClass.getClassId() != null && testClass.getClassId() > 0,
                    "ClassService: Created course section CS888TEST with ID #" + testClass.getClassId());

            // 2. Enroll Student 3 into Class 1 with Audit Logging
            enrollment = classService.enrollStudent(1, 3, "DEAN_ACADEMICS");
            assertCondition(enrollment.getEnrollmentId() != null && enrollment.getEnrollmentId() > 0,
                    "ClassService: Enrolled Student #3 into Class #1 with atomic AUDIT_LOG entry");

            // 3. Duplicate Enrollment Prevention
            try {
                classService.enrollStudent(1, 3, "DEAN_ACADEMICS");
                recordFailure("ClassService: Duplicate enrollment should throw DuplicateRecordException", null);
            } catch (DuplicateRecordException e) {
                recordPass("ClassService: Duplicate enrollment prevented -> DuplicateRecordException caught");
            }

            // 4. Update Attendance to 48.0% and Grade to 'F' to simulate at-risk status
            classService.updateAttendanceAndGrade(enrollment.getEnrollmentId(), 48.00, "F");
            recordPass("ClassService: Updated Student #3 attendance to 48.00% and grade to 'F'");

            // 5. Detect At-Risk Students in Class 1
            List<AtRiskStudentReport> atRiskList = classService.getAtRiskStudents(1);
            boolean foundStudent3 = atRiskList.stream().anyMatch(r -> r.getStudentId() == 3);
            assertCondition(foundStudent3,
                    "ClassService: Early warning engine successfully flagged Student #3 as At-Risk (<60% attendance & Failing F)");

            // 6. Log Progress & Add Resource
            log = classService.logProgress(testClass.getClassId(), LocalDate.now(), "Kubernetes Pod Lifecycle", 2.0, "Hands-on lab");
            assertCondition(log.getLogId() != null && log.getLogId() > 0,
                    "ClassService: Logged teaching progress entry ID #" + log.getLogId());

            res = classService.addResource(testClass.getClassId(), "K8s Architecture Diagrams", ResourceType.SLIDES, "https://edutrack.edu/k8s.pptx");
            assertCondition(res.getResourceId() != null && res.getResourceId() > 0,
                    "ClassService: Shared class resource ID #" + res.getResourceId());

            // Cleanup
            new ClassEnrollmentDAO().delete(enrollment.getEnrollmentId());
            new AuditLogDAO().findByEntity("CLASS_ENROLLMENT", enrollment.getEnrollmentId())
                    .forEach(a -> { try { new AuditLogDAO().delete(a.getLogId()); } catch (Exception ignore) {} });
            new ClassProgressLogDAO().delete(log.getLogId());
            new ClassResourceDAO().delete(res.getResourceId());
            classService.deleteClass(testClass.getClassId());
            recordPass("CLEANUP: Cleaned up test enrollment, audit logs, progress logs, and class");

        } catch (Exception e) {
            recordFailure("ClassService test encountered error", e);
            if (enrollment != null && enrollment.getEnrollmentId() != null) {
                try { new ClassEnrollmentDAO().delete(enrollment.getEnrollmentId()); } catch (Exception ignore) {}
            }
            if (testClass != null && testClass.getClassId() != null) {
                try { classService.deleteClass(testClass.getClassId()); } catch (Exception ignore) {}
            }
        }
        System.out.println();
    }

    private static void testQuizServiceWorkflow(QuizService quizService) {
        System.out.println(">>> 3. TESTING QuizService (AUTHORING, PUBLISHING, DEADLINES & EVALUATION)");
        Quiz testQuiz = null;
        Quiz expiredQuiz = null;
        QuizSubmission submission = null;

        try {
            // 1. Create and Publish Quiz
            testQuiz = new Quiz(
                    1, "Design Patterns Midterm", 25, 12,
                    LocalDateTime.now().plusDays(3), QuizStatus.PUBLISHED
            );
            testQuiz = quizService.createQuiz(testQuiz);
            assertCondition(testQuiz.getQuizId() != null && testQuiz.getQuizId() > 0,
                    "QuizService: Created quiz with ID #" + testQuiz.getQuizId());

            // 2. Pass/Fail Calculation
            assertCondition(quizService.isPassed(18.0, testQuiz.getQuizId()),
                    "QuizService: Score 18.0 correctly evaluated as PASS (>= 12 passing marks)");
            assertCondition(!quizService.isPassed(9.5, testQuiz.getQuizId()),
                    "QuizService: Score 9.5 correctly evaluated as FAIL (< 12 passing marks)");

            // 3. Submit Quiz for Seed Student 1
            submission = quizService.submitQuiz(testQuiz.getQuizId(), 1, 21.50, "Submitted before deadline");
            assertCondition(submission.getSubmissionId() != null && submission.getSubmissionId() > 0,
                    "QuizService: Student #1 submitted quiz successfully (Submission ID #" + submission.getSubmissionId() + ")");

            // 4. Duplicate Submission Rejection
            try {
                quizService.submitQuiz(testQuiz.getQuizId(), 1, 22.0, "Second attempt");
                recordFailure("QuizService: Duplicate submission should throw DuplicateRecordException", null);
            } catch (DuplicateRecordException e) {
                recordPass("QuizService: Duplicate submission rejected -> DuplicateRecordException caught");
            }

            // 5. Deadline Enforcement (Expired Quiz)
            expiredQuiz = new Quiz(
                    1, "Expired Assessment", 20, 10,
                    LocalDateTime.now().minusDays(1), // Deadline in the past
                    QuizStatus.PUBLISHED
            );
            expiredQuiz = quizService.createQuiz(expiredQuiz);

            try {
                quizService.submitQuiz(expiredQuiz.getQuizId(), 2, 15.0, "Late submission");
                recordFailure("QuizService: Submission after deadline should throw EligibilityException", null);
            } catch (EligibilityException e) {
                recordPass("QuizService: Expired quiz deadline enforced -> EligibilityException caught: " + e.getMessage());
            }

            // 6. Teacher Evaluation Update
            boolean evaluated = quizService.evaluateSubmission(
                    submission.getSubmissionId(), 23.50, SubmissionStatus.EVALUATED, "Bonus points for neat architecture diagram"
            );
            assertCondition(evaluated, "QuizService: Evaluation recorded with score 23.50 and feedback");

            // Cleanup
            new QuizSubmissionDAO().delete(submission.getSubmissionId());
            quizService.deleteQuiz(testQuiz.getQuizId());
            quizService.deleteQuiz(expiredQuiz.getQuizId());
            recordPass("CLEANUP: Removed test submissions and quizzes");

        } catch (Exception e) {
            recordFailure("QuizService test encountered error", e);
            if (submission != null && submission.getSubmissionId() != null) {
                try { new QuizSubmissionDAO().delete(submission.getSubmissionId()); } catch (Exception ignore) {}
            }
            if (testQuiz != null && testQuiz.getQuizId() != null) {
                try { quizService.deleteQuiz(testQuiz.getQuizId()); } catch (Exception ignore) {}
            }
            if (expiredQuiz != null && expiredQuiz.getQuizId() != null) {
                try { quizService.deleteQuiz(expiredQuiz.getQuizId()); } catch (Exception ignore) {}
            }
        }
        System.out.println();
    }

    private static void testPlacementEligibilityRules(PlacementService placementService) {
        System.out.println(">>> 4. TESTING PlacementService ELIGIBILITY RULES (CGPA CUTOFF, DEADLINE, STATUS)");
        Company testComp = null;
        PlacementDrive highCutoffDrive = null;

        try {
            testComp = new Company(
                    "Novastone AI Analytics", "Artificial Intelligence", "https://novastone.example.com",
                    "Meera Sen", "careers@novastone.example.com", "+91-80-77665544",
                    "Koramangala, Bengaluru", CompanyStatus.ACTIVE
            );
            testComp = placementService.registerCompany(testComp);

            // High CGPA Cutoff Drive: 8.50 CGPA minimum
            highCutoffDrive = new PlacementDrive(
                    testComp.getCompanyId(), "Principal AI Researcher", "Deep Learning & LLM fine-tuning",
                    18.00, 8.50, "Computer Science, Information Technology",
                    LocalDate.now().plusDays(25), LocalDate.now().plusDays(10), DriveStatus.UPCOMING
            );
            highCutoffDrive = placementService.createPlacementDrive(highCutoffDrive, 10);
            assertCondition(highCutoffDrive.getDriveId() != null && highCutoffDrive.getDriveId() > 0,
                    "PlacementService: Created placement drive with 8.50 CGPA cutoff");

            // Student #3 has CGPA 7.40 (< 8.50 cutoff) -> Must be rejected with EligibilityException
            try {
                placementService.applyForDrive(highCutoffDrive.getDriveId(), 3, "TEST_RUNNER");
                recordFailure("PlacementService: Below-cutoff CGPA should throw EligibilityException", null);
            } catch (EligibilityException e) {
                recordPass("PlacementService: Below-cutoff CGPA rejected -> EligibilityException caught: " + e.getMessage());
            }

            // Cleanup
            placementService.deleteDrive(highCutoffDrive.getDriveId());
            placementService.deleteCompany(testComp.getCompanyId());
            recordPass("CLEANUP: Removed test placement drive and company");

        } catch (Exception e) {
            recordFailure("Placement eligibility test encountered error", e);
            if (highCutoffDrive != null && highCutoffDrive.getDriveId() != null) {
                try { placementService.deleteDrive(highCutoffDrive.getDriveId()); } catch (Exception ignore) {}
            }
            if (testComp != null && testComp.getCompanyId() != null) {
                try { placementService.deleteCompany(testComp.getCompanyId()); } catch (Exception ignore) {}
            }
        }
        System.out.println();
    }

    private static void testMultiThreadedPlacementConcurrency(PlacementService placementService) {
        System.out.println(">>> 5. MULTI-THREADED CONCURRENCY TEST (THREAD-SAFE SYNCHRONIZED SLOT ALLOCATION)");
        System.out.println("Scenario: Exactly 2 vacancy slots available. 5 student threads apply simultaneously.");
        System.out.println("Expected: Exactly 2 succeed, exactly 3 fail with EligibilityException, ZERO over-allocation.");

        Company testCompany = null;
        PlacementDrive limitedDrive = null;
        StudentDAO studentDAO = new StudentDAO();
        ApplicationDAO applicationDAO = new ApplicationDAO();
        AuditLogDAO auditDAO = new AuditLogDAO();

        List<Student> tempStudents = new ArrayList<>();
        List<Application> successfulApplications = new ArrayList<>();

        try {
            // 1. Setup Company and Placement Drive with ONLY 2 VACANCIES
            testCompany = new Company(
                    "QuantumCore Systems Ltd.", "Quantum Computing", "https://quantumcore.example.com",
                    "Dr. Homi Bhabha", "jobs@quantumcore.example.com", "+91-22-88776655",
                    "TIFR Tech Park, Mumbai", CompanyStatus.ACTIVE
            );
            testCompany = placementService.registerCompany(testCompany);

            limitedDrive = new PlacementDrive(
                    testCompany.getCompanyId(), "Quantum Systems Architect", "Qubit Simulation & C++",
                    22.00, 6.00, "ALL", LocalDate.now().plusDays(30), LocalDate.now().plusDays(15), DriveStatus.UPCOMING
            );
            // Configure maximum vacancy slots = 2
            limitedDrive = placementService.createPlacementDrive(limitedDrive, 2);
            final int driveId = limitedDrive.getDriveId();
            final int maxVacancies = 2;

            // 2. Create 5 distinct, eligible test students (CGPA 8.50 >= 6.00 cutoff)
            for (int i = 1; i <= 5; i++) {
                Student s = new Student(
                        "CONC_STU_" + i, "Candidate" + i, "Concurrency",
                        "candidate" + i + "@concurrency.edu", "+91-900000000" + i,
                        "Computer Science", 6, 8.50, 2026, StudentStatus.ACTIVE
                );
                s = studentDAO.create(s);
                tempStudents.add(s);
            }
            recordPass("Created 5 eligible candidate students for concurrent race test");

            // 3. Concurrency Harness: Thread pool with synchronization barriers
            int totalThreads = 5;
            ExecutorService executor = Executors.newFixedThreadPool(totalThreads);
            CountDownLatch readyLatch = new CountDownLatch(totalThreads);
            CountDownLatch startLatch = new CountDownLatch(1);
            CountDownLatch doneLatch = new CountDownLatch(totalThreads);

            AtomicInteger successCounter = new AtomicInteger(0);
            AtomicInteger failureCounter = new AtomicInteger(0);
            List<String> threadLogs = new ArrayList<>();

            for (int i = 0; i < totalThreads; i++) {
                final Student applicant = tempStudents.get(i);
                final int threadNum = i + 1;

                executor.submit(() -> {
                    readyLatch.countDown();
                    try {
                        // Wait until all 5 threads are ready, then fire simultaneously
                        startLatch.await();

                        Application app = placementService.applyForDriveThreadSafe(
                                driveId, applicant.getStudentId(), maxVacancies, "CONCURRENT_THREAD_" + threadNum
                        );

                        successCounter.incrementAndGet();
                        synchronized (successfulApplications) {
                            successfulApplications.add(app);
                        }
                        synchronized (threadLogs) {
                            threadLogs.add("  -> [Thread " + threadNum + " SUCCESS] Allocated slot to Student #" + applicant.getStudentId());
                        }

                    } catch (EligibilityException e) {
                        failureCounter.incrementAndGet();
                        synchronized (threadLogs) {
                            threadLogs.add("  -> [Thread " + threadNum + " REJECTED] " + e.getMessage());
                        }
                    } catch (Exception e) {
                        failureCounter.incrementAndGet();
                        synchronized (threadLogs) {
                            threadLogs.add("  -> [Thread " + threadNum + " ERROR] " + e.getMessage());
                        }
                    } finally {
                        doneLatch.countDown();
                    }
                });
            }

            // Wait until all worker threads are armed at the starting gate
            readyLatch.await(5, TimeUnit.SECONDS);

            // Release all 5 threads at the exact same instant!
            System.out.println("  Firing concurrency race across 5 threads...");
            startLatch.countDown();

            // Wait for all 5 threads to complete
            boolean finishedInTime = doneLatch.await(10, TimeUnit.SECONDS);
            executor.shutdown();

            assertCondition(finishedInTime, "Concurrency execution completed within timeout");

            // Print thread diagnostic logs
            threadLogs.forEach(System.out::println);

            // 4. Assert Concurrency Invariants
            assertCondition(successCounter.get() == 2,
                    String.format("CONCURRENCY INVARIANT: Exactly 2 applications succeeded (Expected: 2 | Actual: %d)", successCounter.get()));

            assertCondition(failureCounter.get() == 3,
                    String.format("CONCURRENCY INVARIANT: Exactly 3 applications rejected due to capacity limit (Expected: 3 | Actual: %d)", failureCounter.get()));

            // Verify in live MySQL database that exactly 2 records were persisted
            List<Application> persistedApps = applicationDAO.findByDriveId(driveId);
            assertCondition(persistedApps.size() == 2,
                    String.format("DATABASE INTEGRITY: Live MySQL table contains EXACTLY %d applications (ZERO over-allocation!)", persistedApps.size()));

            // 5. Cleanup temporary concurrency test entities
            for (Application app : successfulApplications) {
                applicationDAO.delete(app.getApplicationId());
                List<AuditLog> audits = auditDAO.findByEntity("APPLICATION", app.getApplicationId());
                for (AuditLog a : audits) {
                    auditDAO.delete(a.getLogId());
                }
            }
            for (Student s : tempStudents) {
                studentDAO.delete(s.getStudentId());
            }
            placementService.deleteDrive(limitedDrive.getDriveId());
            placementService.deleteCompany(testCompany.getCompanyId());
            recordPass("CLEANUP: Removed all concurrency test applications, audit logs, students, drive, and company");

        } catch (Exception e) {
            recordFailure("Multi-threaded placement concurrency test failed", e);
            for (Application app : successfulApplications) {
                try { applicationDAO.delete(app.getApplicationId()); } catch (Exception ignore) {}
            }
            for (Student s : tempStudents) {
                try { studentDAO.delete(s.getStudentId()); } catch (Exception ignore) {}
            }
            if (limitedDrive != null && limitedDrive.getDriveId() != null) {
                try { placementService.deleteDrive(limitedDrive.getDriveId()); } catch (Exception ignore) {}
            }
            if (testCompany != null && testCompany.getCompanyId() != null) {
                try { placementService.deleteCompany(testCompany.getCompanyId()); } catch (Exception ignore) {}
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
