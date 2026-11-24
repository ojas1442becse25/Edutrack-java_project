package main;

import database.DBConnection;
import service.AuditService;
import service.ClassService;
import service.PlacementService;
import service.QuizService;
import service.StudentService;
import service.TeacherService;
import util.InputValidator;
import util.MenuUtils;

import java.util.Scanner;

/**
 * ============================================================================
 * EDUTRACK: ACADEMIC & CAMPUS PLACEMENT MANAGEMENT SYSTEM
 * Milestone 5: Core Java (Java 8+) + MySQL 8.0 Console Application
 * Architecture: 3-Layer (Presentation -> Service -> DAO -> MySQL)
 * ============================================================================
 * Application Entry Point.
 * Initializes services, verifies database connectivity, and routes users
 * to their respective role-based dashboards.
 */
public class EduTrackApp {

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        InputValidator validator = new InputValidator(scanner);

        MenuUtils.printAppLogo();

        // 1. Verify Database Connectivity on startup
        System.out.println("[BOOT] Initializing database connection pool...");
        boolean connected = DBConnection.testConnection();
        if (!connected) {
            System.err.println(MenuUtils.errorBanner("Unable to establish MySQL connection. Please check database service & credentials."));
            System.exit(1);
        }

        // 2. Initialize Service Layer dependencies (Strict 3-Layer separation)
        StudentService studentService = new StudentService();
        TeacherService teacherService = new TeacherService();
        ClassService classService = new ClassService();
        QuizService quizService = new QuizService();
        PlacementService placementService = new PlacementService();
        AuditService auditService = new AuditService();

        // 3. Initialize Presentation Menus
        TeacherMenu teacherMenu = new TeacherMenu(teacherService, classService, quizService, validator);
        StudentMenu studentMenu = new StudentMenu(studentService, classService, quizService, placementService, validator);
        AdminMenu adminMenu = new AdminMenu(placementService, studentService, teacherService, auditService, validator);

        // 4. Main Application Role-Selection Loop
        boolean running = true;
        while (running) {
            try {
                MenuUtils.printHeader("EDUTRACK MAIN ROLE NAVIGATION");
                System.out.println("1. Faculty & Instructor Portal");
                System.out.println("2. Student Self-Service Portal");
                System.out.println("3. Placement & Academic Administration Portal");
                System.out.println("4. Exit Application");

                int choice = validator.readInt("\nSelect role to login [1-4]: ", 1, 4);
                switch (choice) {
                    case 1:
                        teacherMenu.run();
                        break;
                    case 2:
                        studentMenu.run();
                        break;
                    case 3:
                        adminMenu.run();
                        break;
                    case 4:
                        running = false;
                        System.out.println("\n" + MenuUtils.successBanner("Thank you for using EduTrack. Session terminated safely. Goodbye!"));
                        break;
                }
            } catch (Exception e) {
                // Safeguard against unhandled exceptions bubbling up to JVM
                System.err.println(MenuUtils.errorBanner("An unexpected error occurred: " + e.getMessage()));
            }
        }

        scanner.close();
    }
}
