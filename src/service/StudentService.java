package service;

import dao.ClassDAO;
import dao.ClassEnrollmentDAO;
import dao.QuizSubmissionDAO;
import dao.StudentDAO;
import exception.DuplicateRecordException;
import exception.InvalidDataException;
import exception.RecordNotFoundException;
import model.ClassEntity;
import model.ClassEnrollment;
import model.QuizSubmission;
import model.Student;
import model.Student.StudentStatus;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * ============================================================================
 * VIVA EVALUATION REFERENCE - 3-LAYER ARCHITECTURE (SERVICE LAYER):
 * ============================================================================
 * 1. PURPOSE OF THE SERVICE LAYER:
 *    The Service Layer acts as the orchestrator and business rule guardian of the
 *    system. It decouples the Presentation Layer (Menus/UI) from data storage details
 *    (DAOs and Database).
 *
 * 2. ARCHITECTURAL BOUNDARY RULES:
 *    - Service classes NEVER contain raw SQL strings (SELECT/INSERT/UPDATE/DELETE).
 *    - Service classes NEVER import or manage java.sql.Connection or PreparedStatement.
 *    - All database reads and writes are delegated entirely to the DAO Layer.
 *    - Service methods accept domain models or primitives and enforce business logic,
 *      eligibility calculations, and cross-entity validation before triggering DAOs.
 * ============================================================================
 */
public class StudentService {

    private final StudentDAO studentDAO;
    private final ClassEnrollmentDAO enrollmentDAO;
    private final ClassDAO classDAO;
    private final QuizSubmissionDAO submissionDAO;

    public StudentService() {
        this.studentDAO = new StudentDAO();
        this.enrollmentDAO = new ClassEnrollmentDAO();
        this.classDAO = new ClassDAO();
        this.submissionDAO = new QuizSubmissionDAO();
    }

    public StudentService(StudentDAO studentDAO, ClassEnrollmentDAO enrollmentDAO,
                          ClassDAO classDAO, QuizSubmissionDAO submissionDAO) {
        this.studentDAO = studentDAO;
        this.enrollmentDAO = enrollmentDAO;
        this.classDAO = classDAO;
        this.submissionDAO = submissionDAO;
    }

    /**
     * Registers a new student after enforcing domain validation rules.
     */
    public Student registerStudent(Student student)
            throws InvalidDataException, DuplicateRecordException, SQLException {

        if (student == null) {
            throw new InvalidDataException("Student data cannot be null.");
        }

        // Business Rule: Check if Roll Number or Email is already registered
        Optional<Student> existingRoll = studentDAO.findByRollNumber(student.getRollNumber());
        if (existingRoll.isPresent()) {
            throw new DuplicateRecordException("A student with Roll Number '" + student.getRollNumber() + "' is already registered.");
        }

        Optional<Student> existingEmail = studentDAO.findByEmail(student.getEmail());
        if (existingEmail.isPresent()) {
            throw new DuplicateRecordException("A student with Email '" + student.getEmail() + "' is already registered.");
        }

        return studentDAO.create(student);
    }

    /**
     * Updates an existing student record after validating business constraints.
     */
    public boolean updateStudent(Student student)
            throws InvalidDataException, DuplicateRecordException, RecordNotFoundException, SQLException {

        if (student == null || student.getStudentId() == null) {
            throw new InvalidDataException("Student entity and ID are required for update.");
        }

        // Verify existence before update
        Optional<Student> existing = studentDAO.findById(student.getStudentId());
        if (!existing.isPresent()) {
            throw new RecordNotFoundException("Student with ID #" + student.getStudentId() + " not found.");
        }

        return studentDAO.update(student);
    }

    /**
     * Finds a student by primary key ID.
     */
    public Student getStudentById(int studentId)
            throws RecordNotFoundException, SQLException, InvalidDataException {

        Optional<Student> student = studentDAO.findById(studentId);
        if (!student.isPresent()) {
            throw new RecordNotFoundException("Student with ID #" + studentId + " does not exist.");
        }
        return student.get();
    }

    /**
     * Finds a student by unique roll number.
     */
    public Student getStudentByRollNumber(String rollNumber)
            throws RecordNotFoundException, SQLException, InvalidDataException {

        if (rollNumber == null || rollNumber.trim().isEmpty()) {
            throw new InvalidDataException("Roll number cannot be empty.");
        }
        Optional<Student> student = studentDAO.findByRollNumber(rollNumber.trim());
        if (!student.isPresent()) {
            throw new RecordNotFoundException("Student with Roll Number '" + rollNumber + "' does not exist.");
        }
        return student.get();
    }

    /**
     * Retrieves all registered students.
     */
    public List<Student> getAllStudents() throws SQLException, InvalidDataException {
        return studentDAO.findAll();
    }

    /**
     * Retrieves students belonging to a specified department.
     */
    public List<Student> getStudentsByDepartment(String department)
            throws InvalidDataException, SQLException {

        if (department == null || department.trim().isEmpty()) {
            throw new InvalidDataException("Department cannot be empty.");
        }
        return studentDAO.findByDepartment(department.trim());
    }

    /**
     * Retrieves all course sections (ClassEntity) currently enrolled by a student.
     */
    public List<ClassEntity> getEnrolledClasses(int studentId)
            throws RecordNotFoundException, SQLException, InvalidDataException {

        // Validate student existence
        getStudentById(studentId);

        List<ClassEnrollment> enrollments = enrollmentDAO.findEnrollmentsByStudentId(studentId);
        List<ClassEntity> classes = new ArrayList<>();

        for (ClassEnrollment e : enrollments) {
            Optional<ClassEntity> ce = classDAO.findById(e.getClassId());
            ce.ifPresent(classes::add);
        }
        return classes;
    }

    /**
     * Computes a comprehensive academic performance summary for a student.
     */
    public StudentAcademicSummary getAcademicSummary(int studentId)
            throws RecordNotFoundException, SQLException, InvalidDataException {

        Student student = getStudentById(studentId);
        List<ClassEnrollment> enrollments = enrollmentDAO.findEnrollmentsByStudentId(studentId);
        List<QuizSubmission> submissions = submissionDAO.findByStudentId(studentId);

        double totalAttendance = 0.0;
        int activeEnrollments = 0;
        for (ClassEnrollment e : enrollments) {
            totalAttendance += e.getAttendancePercentage();
            if (e.getStatus() == ClassEnrollment.EnrollmentStatus.ENROLLED) {
                activeEnrollments++;
            }
        }
        double avgAttendance = enrollments.isEmpty() ? 0.0 : Math.round((totalAttendance / enrollments.size()) * 100.0) / 100.0;

        double totalQuizScore = 0.0;
        for (QuizSubmission s : submissions) {
            totalQuizScore += s.getScoreObtained();
        }
        double avgQuizScore = submissions.isEmpty() ? 0.0 : Math.round((totalQuizScore / submissions.size()) * 100.0) / 100.0;

        return new StudentAcademicSummary(
                student.getStudentId(),
                student.getRollNumber(),
                student.getFullName(),
                student.getDepartment(),
                student.getSemester(),
                student.getCgpa(),
                enrollments.size(),
                activeEnrollments,
                avgAttendance,
                submissions.size(),
                avgQuizScore
        );
    }

    /**
     * Soft-deletes a student by changing their status to DROPPED.
     */
    public boolean softDeleteStudent(int studentId) throws RecordNotFoundException, SQLException {
        return studentDAO.softDelete(studentId);
    }

    /**
     * Permanently deletes a student record.
     */
    public boolean deleteStudent(int studentId) throws RecordNotFoundException, SQLException {
        return studentDAO.delete(studentId);
    }

    /**
     * DTO containing aggregated academic metrics for a student.
     */
    public static class StudentAcademicSummary {
        private final int studentId;
        private final String rollNumber;
        private final String studentName;
        private final String department;
        private final int semester;
        private final double cgpa;
        private final int totalEnrolledCourses;
        private final int activeCourses;
        private final double averageAttendance;
        private final int quizzesAttempted;
        private final double averageQuizScore;

        public StudentAcademicSummary(int studentId, String rollNumber, String studentName,
                                      String department, int semester, double cgpa,
                                      int totalEnrolledCourses, int activeCourses,
                                      double averageAttendance, int quizzesAttempted,
                                      double averageQuizScore) {
            this.studentId = studentId;
            this.rollNumber = rollNumber;
            this.studentName = studentName;
            this.department = department;
            this.semester = semester;
            this.cgpa = cgpa;
            this.totalEnrolledCourses = totalEnrolledCourses;
            this.activeCourses = activeCourses;
            this.averageAttendance = averageAttendance;
            this.quizzesAttempted = quizzesAttempted;
            this.averageQuizScore = averageQuizScore;
        }

        public int getStudentId() { return studentId; }
        public String getRollNumber() { return rollNumber; }
        public String getStudentName() { return studentName; }
        public String getDepartment() { return department; }
        public int getSemester() { return semester; }
        public double getCgpa() { return cgpa; }
        public int getTotalEnrolledCourses() { return totalEnrolledCourses; }
        public int getActiveCourses() { return activeCourses; }
        public double getAverageAttendance() { return averageAttendance; }
        public int getQuizzesAttempted() { return quizzesAttempted; }
        public double getAverageQuizScore() { return averageQuizScore; }

        @Override
        public String toString() {
            return String.format(
                    "AcademicSummary[Student: %s (%s) | Sem: %d | CGPA: %.2f | Courses: %d (Active: %d) | Avg Att: %.2f%% | Quizzes: %d (Avg: %.2f)]",
                    studentName, rollNumber, semester, cgpa, totalEnrolledCourses, activeCourses, averageAttendance, quizzesAttempted, averageQuizScore
            );
        }
    }
}
