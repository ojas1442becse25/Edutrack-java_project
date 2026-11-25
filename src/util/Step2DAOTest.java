package util;

import dao.ClassDAO;
import dao.StudentDAO;
import dao.TeacherDAO;
import exception.DuplicateRecordException;
import exception.InvalidDataException;
import exception.RecordNotFoundException;
import model.ClassEntity;
import model.ClassEntity.ClassStatus;
import model.Student;
import model.Student.StudentStatus;
import model.Teacher;
import model.Teacher.TeacherStatus;

import java.io.FileOutputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

public class Step2DAOTest {

    private static final String EVIDENCE_FILE = "evidence/step2_verification.txt";
    private static int testsRun = 0;
    private static int testsPassed = 0;

    public static void main(String[] args) {
        // Tee standard out and err to file
        PrintStream consoleOut = System.out;
        PrintStream consoleErr = System.err;

        try {
            Files.createDirectories(Paths.get("evidence"));
            FileOutputStream fos = new FileOutputStream(EVIDENCE_FILE);
            TeeOutputStream tos = new TeeOutputStream(System.out, fos);
            PrintStream dualOut = new PrintStream(tos, true, "UTF-8");
            System.setOut(dualOut);
            System.setErr(dualOut);

            runAllTests();

        } catch (Exception e) {
            consoleErr.println("Error running Step2DAOTest: " + e.getMessage());
            e.printStackTrace(consoleErr);
        } finally {
            System.setOut(consoleOut);
            System.setErr(consoleErr);
            consoleOut.println("Step2DAOTest execution finished. Evidence written to: " + EVIDENCE_FILE);
        }
    }

    private static void runAllTests() {
        System.out.println("================================================================================");
        System.out.println("EDUTRACK MILESTONE 5 - STEP 2 CORE DAO & DOMAIN MODEL VERIFICATION");
        System.out.println("Timestamp: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        System.out.println("================================================================================\n");

        TeacherDAO teacherDAO = new TeacherDAO();
        StudentDAO studentDAO = new StudentDAO();
        ClassDAO classDAO = new ClassDAO();

        testSeedDataVerification(teacherDAO, studentDAO, classDAO);
        testDomainValidationRules();
        testTeacherDAOCRUD(teacherDAO);
        testStudentDAOCRUD(studentDAO);
        testClassDAOCRUD(classDAO, teacherDAO);

        System.out.println("\n================================================================================");
        System.out.println(String.format("TEST RUN SUMMARY: Total Tests: %d | Passed: %d | Failed: %d",
                testsRun, testsPassed, (testsRun - testsPassed)));
        if (testsRun == testsPassed) {
            System.out.println("RESULT: ALL STEP 2 DAO & MODEL TESTS PASSED SUCCESSFULLY!");
        } else {
            System.out.println("RESULT: SOME TESTS FAILED. CHECK LOGS ABOVE.");
        }
        System.out.println("================================================================================");
    }

    private static void testSeedDataVerification(TeacherDAO teacherDAO, StudentDAO studentDAO, ClassDAO classDAO) {
        System.out.println(">>> 1. VERIFYING SEED DATA PRESENCE FROM STEP 1 SCHEMA");
        try {
            List<Teacher> teachers = teacherDAO.findAll();
            assertCondition(teachers.size() >= 3, "Teacher table contains at least 3 seed records (Found: " + teachers.size() + ")");

            List<Student> students = studentDAO.findAll();
            assertCondition(students.size() >= 3, "Student table contains at least 3 seed records (Found: " + students.size() + ")");

            List<ClassEntity> classes = classDAO.findAll();
            assertCondition(classes.size() >= 3, "Class table contains at least 3 seed records (Found: " + classes.size() + ")");

            // Verify specific seed lookups
            Optional<Student> rahul = studentDAO.findByRollNumber("CS2023001");
            assertCondition(rahul.isPresent() && "Rahul Sharma".equals(rahul.get().getFullName().trim()),
                    "Found seed student CS2023001 (Rahul Sharma)");

            Optional<Teacher> turing = teacherDAO.findByEmail("alan.turing@edutrack.edu");
            assertCondition(turing.isPresent() && "Dr. Alan Turing".equals(turing.get().getFullName().trim()),
                    "Found seed teacher alan.turing@edutrack.edu (Dr. Alan Turing)");

            Optional<ClassEntity> cs301 = classDAO.findByClassCode("CS301");
            assertCondition(cs301.isPresent() && cs301.get().getTeacherId() == 1,
                    "Found seed class CS301 assigned to Teacher ID 1");

        } catch (Exception e) {
            recordFailure("Seed data verification failed", e);
        }
        System.out.println();
    }

    private static void testDomainValidationRules() {
        System.out.println(">>> 2. TESTING DOMAIN VALIDATION RULES (InvalidDataException)");

        // 1. Invalid CGPA (> 10.00)
        try {
            Student s = new Student();
            s.setCgpa(11.50);
            recordFailure("Failed: CGPA > 10.00 should throw InvalidDataException", null);
        } catch (InvalidDataException e) {
            recordPass("Passed: CGPA 11.50 correctly rejected -> " + e.getMessage());
        }

        // 2. Negative semester
        try {
            Student s = new Student();
            s.setSemester(0);
            recordFailure("Failed: Semester 0 should throw InvalidDataException", null);
        } catch (InvalidDataException e) {
            recordPass("Passed: Semester 0 correctly rejected -> " + e.getMessage());
        }

        // 3. Invalid Class credits (> 10)
        try {
            ClassEntity c = new ClassEntity();
            c.setCredits(15);
            recordFailure("Failed: Credits 15 should throw InvalidDataException", null);
        } catch (InvalidDataException e) {
            recordPass("Passed: Credits 15 correctly rejected -> " + e.getMessage());
        }

        // 4. Blank teacher email
        try {
            Teacher t = new Teacher();
            t.setEmail("");
            recordFailure("Failed: Empty email should throw InvalidDataException", null);
        } catch (InvalidDataException e) {
            recordPass("Passed: Empty email correctly rejected -> " + e.getMessage());
        }
        System.out.println();
    }

    private static void testTeacherDAOCRUD(TeacherDAO teacherDAO) {
        System.out.println(">>> 3. TESTING TeacherDAO CRUD OPERATIONS");
        Teacher testTeacher = null;
        try {
            // CREATE
            testTeacher = new Teacher(
                    "TestProf", "Vanderbilt", "test.vanderbilt@edutrack.edu",
                    "+91-9999900001", "Computer Science", "Visiting Professor",
                    LocalDate.of(2025, 1, 1), TeacherStatus.ACTIVE
            );
            testTeacher = teacherDAO.create(testTeacher);
            assertCondition(testTeacher.getTeacherId() != null && testTeacher.getTeacherId() > 0,
                    "CREATE Teacher: Auto-increment ID obtained -> " + testTeacher.getTeacherId());

            // READ by ID & Email
            Optional<Teacher> foundById = teacherDAO.findById(testTeacher.getTeacherId());
            assertCondition(foundById.isPresent() && "test.vanderbilt@edutrack.edu".equals(foundById.get().getEmail()),
                    "READ Teacher by ID: Found " + foundById.get().getFullName());

            Optional<Teacher> foundByEmail = teacherDAO.findByEmail("test.vanderbilt@edutrack.edu");
            assertCondition(foundByEmail.isPresent(), "READ Teacher by Email: Successfully retrieved");

            // UPDATE
            testTeacher.setDesignation("Distinguished Chair Professor");
            testTeacher.setStatus(TeacherStatus.ON_LEAVE);
            boolean updated = teacherDAO.update(testTeacher);
            assertCondition(updated, "UPDATE Teacher: Modified designation and status");

            Optional<Teacher> updatedCheck = teacherDAO.findById(testTeacher.getTeacherId());
            assertCondition(updatedCheck.isPresent() && TeacherStatus.ON_LEAVE == updatedCheck.get().getStatus(),
                    "VERIFY UPDATE: Status is now ON_LEAVE");

            // DUPLICATE CHECK (SQLState 23000)
            try {
                Teacher dup = new Teacher(
                        "Another", "Prof", "test.vanderbilt@edutrack.edu", // Duplicate email
                        "+91-9999900002", "IT", "Lecturer",
                        LocalDate.now(), TeacherStatus.ACTIVE
                );
                teacherDAO.create(dup);
                recordFailure("Failed: Duplicate teacher email should throw DuplicateRecordException", null);
            } catch (DuplicateRecordException e) {
                recordPass("Passed: Duplicate email detected -> DuplicateRecordException caught");
            }

            // DELETE
            boolean deleted = teacherDAO.delete(testTeacher.getTeacherId());
            assertCondition(deleted, "DELETE Teacher: Removed test record with ID " + testTeacher.getTeacherId());

            // VERIFY DELETED
            Optional<Teacher> postDelete = teacherDAO.findById(testTeacher.getTeacherId());
            assertCondition(!postDelete.isPresent(), "VERIFY DELETE: Teacher ID " + testTeacher.getTeacherId() + " no longer exists");

            // RECORD NOT FOUND EXCEPTION ON UPDATE
            try {
                teacherDAO.update(testTeacher);
                recordFailure("Failed: Updating non-existent teacher should throw RecordNotFoundException", null);
            } catch (RecordNotFoundException e) {
                recordPass("Passed: RecordNotFoundException correctly caught on non-existent update");
            }

        } catch (Exception e) {
            recordFailure("TeacherDAO CRUD test encountered unexpected error", e);
            if (testTeacher != null && testTeacher.getTeacherId() != null) {
                try { teacherDAO.delete(testTeacher.getTeacherId()); } catch (Exception ignore) {}
            }
        }
        System.out.println();
    }

    private static void testStudentDAOCRUD(StudentDAO studentDAO) {
        System.out.println(">>> 4. TESTING StudentDAO CRUD OPERATIONS");
        Student testStudent = null;
        try {
            // CREATE
            testStudent = new Student(
                    "CS2026TEST", "Vikram", "Aditya", "vikram.aditya.test@student.edu",
                    "+91-9999900003", "Computer Science", 6, 9.45, 2026, StudentStatus.ACTIVE
            );
            testStudent = studentDAO.create(testStudent);
            assertCondition(testStudent.getStudentId() != null && testStudent.getStudentId() > 0,
                    "CREATE Student: Auto-increment ID obtained -> " + testStudent.getStudentId());

            // READ by ID & Roll
            Optional<Student> foundById = studentDAO.findById(testStudent.getStudentId());
            assertCondition(foundById.isPresent() && "CS2026TEST".equals(foundById.get().getRollNumber()),
                    "READ Student by ID: Found roll " + foundById.get().getRollNumber());

            Optional<Student> foundByRoll = studentDAO.findByRollNumber("CS2026TEST");
            assertCondition(foundByRoll.isPresent(), "READ Student by RollNumber: Successfully retrieved");

            // FIND BY MIN CGPA
            List<Student> highScorers = studentDAO.findByMinCgpa(9.00);
            assertCondition(!highScorers.isEmpty(), "QUERY Student by Min CGPA >= 9.00: Found " + highScorers.size() + " students");

            // UPDATE
            testStudent.setCgpa(9.85);
            testStudent.setSemester(7);
            boolean updated = studentDAO.update(testStudent);
            assertCondition(updated, "UPDATE Student: Updated CGPA to 9.85 and Semester to 7");

            Optional<Student> updatedCheck = studentDAO.findById(testStudent.getStudentId());
            assertCondition(updatedCheck.isPresent() && Math.abs(updatedCheck.get().getCgpa() - 9.85) < 0.001,
                    "VERIFY UPDATE: CGPA persisted as 9.85");

            // DUPLICATE CHECK (SQLState 23000)
            try {
                Student dup = new Student(
                        "CS2026TEST", "Duplicate", "Student", "different.email@student.edu",
                        null, "Computer Science", 1, 8.0, 2026, StudentStatus.ACTIVE
                );
                studentDAO.create(dup);
                recordFailure("Failed: Duplicate roll number should throw DuplicateRecordException", null);
            } catch (DuplicateRecordException e) {
                recordPass("Passed: Duplicate roll number detected -> DuplicateRecordException caught");
            }

            // SOFT DELETE
            boolean softDeleted = studentDAO.softDelete(testStudent.getStudentId());
            assertCondition(softDeleted, "SOFT-DELETE Student: Marked status as DROPPED");

            Optional<Student> softCheck = studentDAO.findById(testStudent.getStudentId());
            assertCondition(softCheck.isPresent() && StudentStatus.DROPPED == softCheck.get().getStatus(),
                    "VERIFY SOFT-DELETE: Status is DROPPED in database");

            // HARD DELETE
            boolean deleted = studentDAO.delete(testStudent.getStudentId());
            assertCondition(deleted, "HARD-DELETE Student: Removed record ID " + testStudent.getStudentId());

            // VERIFY DELETED
            Optional<Student> postDelete = studentDAO.findById(testStudent.getStudentId());
            assertCondition(!postDelete.isPresent(), "VERIFY DELETE: Student ID " + testStudent.getStudentId() + " no longer exists");

        } catch (Exception e) {
            recordFailure("StudentDAO CRUD test encountered unexpected error", e);
            if (testStudent != null && testStudent.getStudentId() != null) {
                try { studentDAO.delete(testStudent.getStudentId()); } catch (Exception ignore) {}
            }
        }
        System.out.println();
    }

    private static void testClassDAOCRUD(ClassDAO classDAO, TeacherDAO teacherDAO) {
        System.out.println(">>> 5. TESTING ClassDAO CRUD OPERATIONS");
        ClassEntity testClass = null;
        Teacher tempTeacher = null;
        try {
            // Need a valid teacher for foreign key constraint
            tempTeacher = new Teacher(
                    "Course", "Instructor", "course.instructor@edutrack.edu",
                    null, "Computer Science", "Assistant Professor",
                    LocalDate.now(), TeacherStatus.ACTIVE
            );
            tempTeacher = teacherDAO.create(tempTeacher);

            // CREATE
            testClass = new ClassEntity(
                    "CS999TEST", "Distributed Cloud Systems", tempTeacher.getTeacherId(),
                    7, "2026-2027", 4, 45, ClassStatus.SCHEDULED
            );
            testClass = classDAO.create(testClass);
            assertCondition(testClass.getClassId() != null && testClass.getClassId() > 0,
                    "CREATE Class: Auto-increment ID obtained -> " + testClass.getClassId());

            // READ by ID & Code
            Optional<ClassEntity> foundById = classDAO.findById(testClass.getClassId());
            assertCondition(foundById.isPresent() && "CS999TEST".equals(foundById.get().getClassCode()),
                    "READ Class by ID: Found class code " + foundById.get().getClassCode());

            Optional<ClassEntity> foundByCode = classDAO.findByClassCode("CS999TEST");
            assertCondition(foundByCode.isPresent(), "READ Class by Code: Successfully retrieved");

            // QUERY CLASSES BY TEACHER
            List<ClassEntity> teacherClasses = classDAO.findClassesByTeacherId(tempTeacher.getTeacherId());
            assertCondition(teacherClasses.size() == 1 && "CS999TEST".equals(teacherClasses.get(0).getClassCode()),
                    "QUERY Class by Teacher ID: Found 1 class assigned to instructor");

            // UPDATE
            testClass.setMaxCapacity(50);
            testClass.setStatus(ClassStatus.ONGOING);
            boolean updated = classDAO.update(testClass);
            assertCondition(updated, "UPDATE Class: Capacity set to 50, status ONGOING");

            Optional<ClassEntity> updatedCheck = classDAO.findById(testClass.getClassId());
            assertCondition(updatedCheck.isPresent() && ClassStatus.ONGOING == updatedCheck.get().getStatus(),
                    "VERIFY UPDATE: Class status is now ONGOING");

            // DUPLICATE CLASS CODE
            try {
                ClassEntity dup = new ClassEntity(
                        "CS999TEST", "Another Course", tempTeacher.getTeacherId(),
                        7, "2026-2027", 3, 30, ClassStatus.SCHEDULED
                );
                classDAO.create(dup);
                recordFailure("Failed: Duplicate class code should throw DuplicateRecordException", null);
            } catch (DuplicateRecordException e) {
                recordPass("Passed: Duplicate class code detected -> DuplicateRecordException caught");
            }

            // DELETE
            boolean deleted = classDAO.delete(testClass.getClassId());
            assertCondition(deleted, "DELETE Class: Removed record ID " + testClass.getClassId());

            Optional<ClassEntity> postDelete = classDAO.findById(testClass.getClassId());
            assertCondition(!postDelete.isPresent(), "VERIFY DELETE: Class ID " + testClass.getClassId() + " no longer exists");

            // Clean up temporary teacher
            teacherDAO.delete(tempTeacher.getTeacherId());
            recordPass("CLEANUP: Removed temporary instructor ID " + tempTeacher.getTeacherId());

        } catch (Exception e) {
            recordFailure("ClassDAO CRUD test encountered unexpected error", e);
            if (testClass != null && testClass.getClassId() != null) {
                try { classDAO.delete(testClass.getClassId()); } catch (Exception ignore) {}
            }
            if (tempTeacher != null && tempTeacher.getTeacherId() != null) {
                try { teacherDAO.delete(tempTeacher.getTeacherId()); } catch (Exception ignore) {}
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
