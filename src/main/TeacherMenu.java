package main;

import exception.DuplicateRecordException;
import exception.EligibilityException;
import exception.InvalidDataException;
import exception.RecordNotFoundException;
import model.ClassEntity;
import model.ClassEntity.ClassStatus;
import model.ClassProgressLog;
import model.ClassResource;
import model.ClassResource.ResourceType;
import model.Quiz;
import model.Quiz.QuizStatus;
import model.QuizSubmission;
import model.QuizSubmission.SubmissionStatus;
import model.Teacher;
import service.ClassService;
import service.ClassService.AtRiskStudentReport;
import service.QuizService;
import service.TeacherService;
import util.InputValidator;
import util.MenuUtils;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public class TeacherMenu {

    private final TeacherService teacherService;
    private final ClassService classService;
    private final QuizService quizService;
    private final InputValidator validator;

    public TeacherMenu(TeacherService teacherService, ClassService classService,
                       QuizService quizService, InputValidator validator) {
        this.teacherService = teacherService;
        this.classService = classService;
        this.quizService = quizService;
        this.validator = validator;
    }

    public void run() {
        MenuUtils.printHeader("FACULTY & INSTRUCTOR PORTAL - LOGIN");
        System.out.println("Seed Quick-Login Hint: 'alan.turing@edutrack.edu' or 'grace.hopper@edutrack.edu'");

        String email = validator.readEmail("Enter your registered faculty email: ");
        Teacher teacher;
        try {
            teacher = teacherService.getTeacherByEmail(email);
        } catch (RecordNotFoundException e) {
            System.out.println(MenuUtils.errorBanner("Faculty email not found in records. Returning to main menu."));
            return;
        } catch (Exception e) {
            System.out.println(MenuUtils.errorBanner("Database error during login: " + e.getMessage()));
            return;
        }

        System.out.println(MenuUtils.successBanner("Welcome, " + teacher.getDesignation() + " " + teacher.getFullName() + "!"));

        boolean exitMenu = false;
        while (!exitMenu) {
            MenuUtils.printHeader("FACULTY DASHBOARD - " + teacher.getFullName());
            System.out.println("1. View My Assigned Courses & Classes");
            System.out.println("2. Create New Course Section");
            System.out.println("3. Log Daily Teaching Progress");
            System.out.println("4. Share Course Material & Learning Resources");
            System.out.println("5. Author & Publish New Quiz");
            System.out.println("6. Review Quiz Submissions & Grade Feedback");
            System.out.println("7. View Early-Warning At-Risk Students (<60% Attendance/Failing)");
            System.out.println("8. Logout / Return to Main Menu");

            int choice = validator.readInt("\nSelect an action [1-8]: ", 1, 8);
            switch (choice) {
                case 1: viewAssignedClasses(teacher.getTeacherId()); break;
                case 2: createCourseSection(teacher.getTeacherId()); break;
                case 3: logTeachingProgress(teacher.getTeacherId()); break;
                case 4: shareCourseResource(teacher.getTeacherId()); break;
                case 5: authorQuiz(teacher.getTeacherId()); break;
                case 6: reviewQuizSubmissions(teacher.getTeacherId()); break;
                case 7: checkAtRiskStudents(teacher.getTeacherId()); break;
                case 8: exitMenu = true; break;
            }
        }
    }

    private void viewAssignedClasses(int teacherId) {
        MenuUtils.printSubHeader("MY ASSIGNED COURSES");
        try {
            List<ClassEntity> classes = teacherService.getAssignedClasses(teacherId);
            if (classes.isEmpty()) {
                System.out.println(MenuUtils.infoBanner("No courses currently assigned to your profile."));
                return;
            }
            System.out.printf("%-8s | %-12s | %-32s | %-5s | %-8s | %-10s%n",
                    "ClassID", "Code", "Course Name", "Sem", "Capacity", "Status");
            System.out.println("-".repeat(88));
            for (ClassEntity c : classes) {
                System.out.printf("%-8d | %-12s | %-32s | %-5d | %-8d | %-10s%n",
                        c.getClassId(), c.getClassCode(), c.getCourseName(), c.getSemester(), c.getMaxCapacity(), c.getStatus());
            }
        } catch (Exception e) {
            System.out.println(MenuUtils.errorBanner("Failed to retrieve assigned classes: " + e.getMessage()));
        }
    }

    private void createCourseSection(int teacherId) {
        MenuUtils.printSubHeader("CREATE NEW COURSE SECTION");
        try {
            String code = validator.readNonEmptyString("Course Code (e.g., CS401): ", 30).toUpperCase();
            String name = validator.readNonEmptyString("Course Name: ", 100);
            int sem = validator.readInt("Semester [1-8]: ", 1, 8);
            String year = validator.readNonEmptyString("Academic Year (e.g., 2026-2027): ", 20);
            int credits = validator.readInt("Credits [1-10]: ", 1, 10);
            int capacity = validator.readInt("Max Enrollment Capacity: ", 10, 200);

            ClassEntity ce = new ClassEntity(code, name, teacherId, sem, year, credits, capacity, ClassStatus.SCHEDULED);
            ClassEntity created = classService.createClass(ce);
            System.out.println(MenuUtils.successBanner("Course section created successfully! Assigned Class ID: " + created.getClassId()));
        } catch (DuplicateRecordException e) {
            System.out.println(MenuUtils.errorBanner(e.getMessage()));
        } catch (Exception e) {
            System.out.println(MenuUtils.errorBanner("Creation failed: " + e.getMessage()));
        }
    }

    private void logTeachingProgress(int teacherId) {
        MenuUtils.printSubHeader("LOG TEACHING PROGRESS");
        try {
            viewAssignedClasses(teacherId);
            int classId = validator.readInt("\nEnter Class ID to log: ");
            classService.getClassById(classId); // verify existence

            LocalDate date = validator.readDate("Date of lecture");
            String topic = validator.readNonEmptyString("Topic Covered: ", 200);
            double hours = validator.readDouble("Lecture/Lab Hours Spent (0.5 to 12.0): ", 0.5, 12.0);
            String remarks = validator.readOptionalString("Remarks / Assignment Given", "Covered syllabus requirements", 255);

            ClassProgressLog log = classService.logProgress(classId, date, topic, hours, remarks);
            System.out.println(MenuUtils.successBanner("Teaching progress saved successfully (Log ID #" + log.getLogId() + ")"));
        } catch (Exception e) {
            System.out.println(MenuUtils.errorBanner("Failed to record progress: " + e.getMessage()));
        }
    }

    private void shareCourseResource(int teacherId) {
        MenuUtils.printSubHeader("SHARE COURSE MATERIAL & RESOURCE");
        try {
            viewAssignedClasses(teacherId);
            int classId = validator.readInt("\nEnter Class ID: ");
            classService.getClassById(classId);

            String title = validator.readNonEmptyString("Resource Title: ", 150);
            System.out.println("Select Resource Type: 1. SYLLABUS, 2. NOTES, 3. ASSIGNMENT, 4. VIDEO, 5. SLIDES, 6. OTHER");
            int typeChoice = validator.readInt("Choice [1-6]: ", 1, 6);
            ResourceType type = ResourceType.values()[typeChoice - 1];
            String url = validator.readNonEmptyString("Document URL / Cloud Link: ", 255);

            ClassResource res = classService.addResource(classId, title, type, url);
            System.out.println(MenuUtils.successBanner("Course material uploaded successfully (Resource ID #" + res.getResourceId() + ")"));
        } catch (Exception e) {
            System.out.println(MenuUtils.errorBanner("Failed to upload resource: " + e.getMessage()));
        }
    }

    private void authorQuiz(int teacherId) {
        MenuUtils.printSubHeader("AUTHOR & PUBLISH NEW QUIZ");
        try {
            viewAssignedClasses(teacherId);
            int classId = validator.readInt("\nEnter Target Class ID: ");
            classService.getClassById(classId);

            String title = validator.readNonEmptyString("Quiz Title: ", 150);
            int total = validator.readInt("Total Marks: ", 5, 100);
            int pass = validator.readInt("Passing Cutoff Marks: ", 1, total);
            LocalDateTime due = validator.readDateTime("Submission Due Date & Time");

            Quiz quiz = new Quiz(classId, title, total, pass, due, QuizStatus.PUBLISHED);
            Quiz created = quizService.createQuiz(quiz);
            System.out.println(MenuUtils.successBanner("Quiz published to students! Assigned Quiz ID #" + created.getQuizId()));
        } catch (Exception e) {
            System.out.println(MenuUtils.errorBanner("Failed to author quiz: " + e.getMessage()));
        }
    }

    private void reviewQuizSubmissions(int teacherId) {
        MenuUtils.printSubHeader("REVIEW & EVALUATE QUIZ SUBMISSIONS");
        try {
            int quizId = validator.readInt("Enter Quiz ID to view submissions: ");
            Quiz quiz = quizService.getQuizById(quizId);

            List<QuizSubmission> submissions = quizService.getSubmissionsByQuiz(quizId);
            if (submissions.isEmpty()) {
                System.out.println(MenuUtils.infoBanner("No submissions recorded yet for Quiz #" + quizId + "."));
                return;
            }

            System.out.printf("%-8s | %-10s | %-8s | %-12s | %-24s%n",
                    "SubID", "StudentID", "Score", "Status", "Feedback/Remarks");
            System.out.println("-".repeat(72));
            for (QuizSubmission s : submissions) {
                System.out.printf("%-8d | %-10d | %-8.2f | %-12s | %-24s%n",
                        s.getSubmissionId(), s.getStudentId(), s.getScoreObtained(), s.getStatus(),
                        s.getRemarks() != null ? s.getRemarks() : "None");
            }

            if (validator.readConfirmation("\nDo you wish to grade or add feedback to a submission?")) {
                int subId = validator.readInt("Enter Submission ID to evaluate: ");
                double newScore = validator.readDouble("Enter Adjusted/Evaluated Score (Max " + quiz.getTotalMarks() + "): ", 0.0, quiz.getTotalMarks());
                String feedback = validator.readNonEmptyString("Teacher Feedback Notes: ", 255);
                quizService.evaluateSubmission(subId, newScore, SubmissionStatus.EVALUATED, feedback);
                System.out.println(MenuUtils.successBanner("Evaluation recorded successfully!"));
            }
        } catch (Exception e) {
            System.out.println(MenuUtils.errorBanner("Error reviewing submissions: " + e.getMessage()));
        }
    }

    private void checkAtRiskStudents(int teacherId) {
        MenuUtils.printSubHeader("EARLY WARNING ENGINE - AT-RISK STUDENTS");
        try {
            viewAssignedClasses(teacherId);
            int classId = validator.readInt("\nEnter Class ID to inspect: ");
            List<AtRiskStudentReport> reports = classService.getAtRiskStudents(classId);

            if (reports.isEmpty()) {
                System.out.println(MenuUtils.successBanner("Good news! All enrolled students have >=60% attendance and passing grades."));
                return;
            }

            System.out.println(MenuUtils.warningBanner(reports.size() + " student(s) flagged for academic intervention!"));
            System.out.printf("%-10s | %-22s | %-10s | %-6s | %-32s%n",
                    "Roll No", "Student Name", "Attendance", "Grade", "Intervention Reason");
            System.out.println("-".repeat(88));
            for (AtRiskStudentReport r : reports) {
                System.out.printf("%-10s | %-22s | %-9.2f%% | %-6s | %-32s%n",
                        r.getRollNumber(), r.getStudentName(), r.getAttendancePercentage(),
                        r.getGrade() != null ? r.getGrade() : "N/A", r.getRiskReason());
            }
        } catch (Exception e) {
            System.out.println(MenuUtils.errorBanner("Failed to run at-risk analysis: " + e.getMessage()));
        }
    }
}
