package main;

import exception.DuplicateRecordException;
import exception.EligibilityException;
import exception.InvalidDataException;
import exception.RecordNotFoundException;
import model.Application;
import model.ClassEntity;
import model.ClassResource;
import model.PlacementDrive;
import model.Quiz;
import model.QuizSubmission;
import model.Student;
import service.ClassService;
import service.PlacementService;
import service.QuizService;
import service.StudentService;
import service.StudentService.StudentAcademicSummary;
import util.InputValidator;
import util.MenuUtils;

import java.sql.SQLException;
import java.util.List;

/**
 * Presentation Layer: Student Console Self-Service Portal.
 * Handles course review, academic progress, quiz attempts, and placement drive applications.
 */
public class StudentMenu {

    private final StudentService studentService;
    private final ClassService classService;
    private final QuizService quizService;
    private final PlacementService placementService;
    private final InputValidator validator;

    public StudentMenu(StudentService studentService, ClassService classService,
                       QuizService quizService, PlacementService placementService,
                       InputValidator validator) {
        this.studentService = studentService;
        this.classService = classService;
        this.quizService = quizService;
        this.placementService = placementService;
        this.validator = validator;
    }

    public void run() {
        MenuUtils.printHeader("STUDENT SELF-SERVICE PORTAL - LOGIN");
        System.out.println("Seed Quick-Login Hint: 'CS2023001' (Rahul Sharma) or 'CS2023002' (Priya Nair)");

        String roll = validator.readNonEmptyString("Enter your Student Roll Number: ", 30).toUpperCase();
        Student student;
        try {
            student = studentService.getStudentByRollNumber(roll);
        } catch (RecordNotFoundException e) {
            System.out.println(MenuUtils.errorBanner("Student roll number not found in university database."));
            return;
        } catch (Exception e) {
            System.out.println(MenuUtils.errorBanner("Database error during login: " + e.getMessage()));
            return;
        }

        System.out.println(MenuUtils.successBanner("Welcome, " + student.getFullName() + "! (Semester " + student.getSemester() + " | CGPA: " + student.getCgpa() + ")"));

        boolean exitMenu = false;
        while (!exitMenu) {
            MenuUtils.printHeader("STUDENT DASHBOARD - " + student.getFullName());
            System.out.println("1. View My Enrolled Courses & Academic Performance Summary");
            System.out.println("2. Browse & Access Course Study Materials");
            System.out.println("3. View & Attempt Active Course Quizzes");
            System.out.println("4. Explore Upcoming Campus Placement Drives");
            System.out.println("5. Apply for Placement Drive (Thread-Safe Slot Allocation)");
            System.out.println("6. Track My Placement Application Statuses");
            System.out.println("7. Logout / Return to Main Menu");

            int choice = validator.readInt("\nSelect an action [1-7]: ", 1, 7);
            switch (choice) {
                case 1: viewAcademicPerformance(student.getStudentId()); break;
                case 2: browseCourseResources(student.getStudentId()); break;
                case 3: attemptActiveQuizzes(student.getStudentId()); break;
                case 4: viewPlacementDrives(); break;
                case 5: applyToPlacementDrive(student.getStudentId()); break;
                case 6: trackApplicationStatuses(student.getStudentId()); break;
                case 7: exitMenu = true; break;
            }
        }
    }

    private void viewAcademicPerformance(int studentId) {
        MenuUtils.printSubHeader("MY ACADEMIC PERFORMANCE SUMMARY");
        try {
            StudentAcademicSummary summary = studentService.getAcademicSummary(studentId);
            System.out.println("  * Full Name           : " + summary.getStudentName());
            System.out.println("  * Roll Number         : " + summary.getRollNumber());
            System.out.println("  * Academic Department : " + summary.getDepartment());
            System.out.println("  * Current Semester    : " + summary.getSemester());
            System.out.println("  * Current CGPA        : " + String.format("%.2f / 10.00", summary.getCgpa()));
            System.out.println("  * Total Courses Taken : " + summary.getTotalEnrolledCourses() + " (Active: " + summary.getActiveCourses() + ")");
            System.out.println("  * Overall Attendance  : " + String.format("%.2f%%", summary.getAverageAttendance()));
            System.out.println("  * Quizzes Attempted   : " + summary.getQuizzesAttempted() + " (Avg Score: " + String.format("%.2f", summary.getAverageQuizScore()) + ")");

            MenuUtils.printSubHeader("ENROLLED COURSES");
            List<ClassEntity> classes = studentService.getEnrolledClasses(studentId);
            if (classes.isEmpty()) {
                System.out.println(MenuUtils.infoBanner("No course enrollments found."));
                return;
            }
            System.out.printf("%-8s | %-12s | %-32s | %-7s | %-10s%n",
                    "ClassID", "Code", "Course Title", "Credits", "Status");
            System.out.println("-".repeat(76));
            for (ClassEntity c : classes) {
                System.out.printf("%-8d | %-12s | %-32s | %-7d | %-10s%n",
                        c.getClassId(), c.getClassCode(), c.getCourseName(), c.getCredits(), c.getStatus());
            }
        } catch (Exception e) {
            System.out.println(MenuUtils.errorBanner("Error fetching academic metrics: " + e.getMessage()));
        }
    }

    private void browseCourseResources(int studentId) {
        MenuUtils.printSubHeader("COURSE STUDY MATERIALS & RESOURCES");
        try {
            List<ClassEntity> classes = studentService.getEnrolledClasses(studentId);
            if (classes.isEmpty()) {
                System.out.println(MenuUtils.infoBanner("You are not enrolled in any courses."));
                return;
            }

            for (ClassEntity c : classes) {
                System.out.println("\n[Course: " + c.getClassCode() + " - " + c.getCourseName() + "]");
                List<ClassResource> resources = classService.getResources(c.getClassId());
                if (resources.isEmpty()) {
                    System.out.println("  (No resources uploaded yet)");
                    continue;
                }
                for (ClassResource r : resources) {
                    System.out.printf("  • [%-10s] %-32s -> Link: %s%n",
                            r.getResourceType(), r.getTitle(), r.getResourceUrl());
                }
            }
        } catch (Exception e) {
            System.out.println(MenuUtils.errorBanner("Failed to browse resources: " + e.getMessage()));
        }
    }

    private void attemptActiveQuizzes(int studentId) {
        MenuUtils.printSubHeader("ACTIVE COURSE QUIZZES");
        try {
            int quizId = validator.readInt("Enter Quiz ID to inspect and attempt: ");
            Quiz quiz = quizService.getQuizById(quizId);

            System.out.println("  * Quiz Title     : " + quiz.getTitle());
            System.out.println("  * Total Marks    : " + quiz.getTotalMarks());
            System.out.println("  * Passing Cutoff : " + quiz.getPassingMarks());
            System.out.println("  * Submission Due : " + quiz.getDueDate());
            System.out.println("  * Status         : " + quiz.getStatus());

            if (validator.readConfirmation("\nDo you wish to submit your answers now?")) {
                double score = validator.readDouble("Enter your score/marks obtained (0 to " + quiz.getTotalMarks() + "): ",
                        0.0, quiz.getTotalMarks());
                String remarks = validator.readOptionalString("Notes / Remarks", "Submitted via portal", 255);

                QuizSubmission sub = quizService.submitQuiz(quizId, studentId, score, remarks);
                boolean passed = quizService.isPassed(score, quizId);

                System.out.println(MenuUtils.successBanner(String.format(
                        "Quiz submitted! Result: %s (Scored: %.2f / %d | Passing cutoff: %d)",
                        passed ? "PASS (Qualified)" : "FAIL (Below Cutoff)", score, quiz.getTotalMarks(), quiz.getPassingMarks()
                )));
            }
        } catch (EligibilityException | DuplicateRecordException e) {
            System.out.println(MenuUtils.errorBanner(e.getMessage()));
        } catch (Exception e) {
            System.out.println(MenuUtils.errorBanner("Quiz submission failed: " + e.getMessage()));
        }
    }

    private void viewPlacementDrives() {
        MenuUtils.printSubHeader("UPCOMING CAMPUS PLACEMENT DRIVES");
        try {
            List<PlacementDrive> drives = placementService.getActiveDrives();
            if (drives.isEmpty()) {
                System.out.println(MenuUtils.infoBanner("No active placement drives currently scheduled."));
                return;
            }

            System.out.printf("%-8s | %-28s | %-12s | %-10s | %-14s | %-12s%n",
                    "DriveID", "Role Title", "Package", "Min CGPA", "Deadline", "Status");
            System.out.println("-".repeat(92));
            for (PlacementDrive d : drives) {
                System.out.printf("%-8d | %-28s | %-9.2f LPA | %-10.2f | %-14s | %-12s%n",
                        d.getDriveId(), d.getJobTitle(), d.getPackageLpa(),
                        d.getMinCgpaCriteria(), d.getLastDateToApply(), d.getStatus());
            }
        } catch (Exception e) {
            System.out.println(MenuUtils.errorBanner("Error loading drives: " + e.getMessage()));
        }
    }

    private void applyToPlacementDrive(int studentId) {
        MenuUtils.printSubHeader("APPLY FOR PLACEMENT DRIVE");
        viewPlacementDrives();

        try {
            int driveId = validator.readInt("\nEnter Target Drive ID: ");
            PlacementDrive drive = placementService.getDriveById(driveId);

            System.out.println("\nApplication Criteria:");
            System.out.println("  • Minimum CGPA Cutoff : " + drive.getMinCgpaCriteria());
            System.out.println("  • Eligible Department : " + drive.getEligibleDepartment());
            System.out.println("  • Last Date to Apply  : " + drive.getLastDateToApply());

            if (validator.readConfirmation("\nConfirm submission of job application?")) {
                // Thread-safe application invocation
                Application app = placementService.applyForDrive(driveId, studentId, "STUDENT_PORTAL");
                System.out.println(MenuUtils.successBanner("Congratulations! Your application has been submitted successfully (App ID #" + app.getApplicationId() + ")."));
            }
        } catch (EligibilityException | DuplicateRecordException e) {
            System.out.println(MenuUtils.errorBanner(e.getMessage()));
        } catch (Exception e) {
            System.out.println(MenuUtils.errorBanner("Application failed: " + e.getMessage()));
        }
    }

    private void trackApplicationStatuses(int studentId) {
        MenuUtils.printSubHeader("MY PLACEMENT APPLICATIONS TRACKER");
        try {
            List<Application> apps = placementService.getApplicationsByStudent(studentId);
            if (apps.isEmpty()) {
                System.out.println(MenuUtils.infoBanner("You have not submitted any placement applications yet."));
                return;
            }

            System.out.printf("%-8s | %-8s | %-20s | %-18s | %-28s%n",
                    "AppID", "DriveID", "Application Date", "Current Status", "Interview Feedback");
            System.out.println("-".repeat(92));
            for (Application a : apps) {
                System.out.printf("%-8d | %-8d | %-20s | %-18s | %-28s%n",
                        a.getApplicationId(), a.getDriveId(), a.getApplicationDate().toLocalDate(),
                        a.getStatus(), a.getInterviewFeedback() != null ? a.getInterviewFeedback() : "Awaiting Screening");
            }
        } catch (Exception e) {
            System.out.println(MenuUtils.errorBanner("Failed to retrieve applications: " + e.getMessage()));
        }
    }
}
