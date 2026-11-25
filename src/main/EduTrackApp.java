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

public class EduTrackApp {

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        InputValidator validator = new InputValidator(scanner);

        MenuUtils.printAppLogo();

        // verify db connection
        System.out.println("Checking database connection...");
        boolean connected = DBConnection.testConnection();
        if (!connected) {
            System.err.println(MenuUtils.errorBanner("Unable to connect to MySQL database."));
            System.exit(1);
        }

        // init services
        StudentService studentService = new StudentService();
        TeacherService teacherService = new TeacherService();
        ClassService classService = new ClassService();
        QuizService quizService = new QuizService();
        PlacementService placementService = new PlacementService();
        AuditService auditService = new AuditService();

        // init menus
        TeacherMenu teacherMenu = new TeacherMenu(teacherService, classService, quizService, validator);
        StudentMenu studentMenu = new StudentMenu(studentService, classService, quizService, placementService, validator);
        AdminMenu adminMenu = new AdminMenu(placementService, studentService, teacherService, auditService, validator);

        // main loop
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
                        System.out.println("\n" + MenuUtils.successBanner("Thank you for using EduTrack. Goodbye!"));
                        break;
                }
            } catch (Exception e) {
                System.err.println(MenuUtils.errorBanner("An unexpected error occurred: " + e.getMessage()));
            }
        }

        scanner.close();
    }
}
