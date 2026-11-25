package service;

import dao.ClassDAO;
import dao.ClassEnrollmentDAO;
import dao.ClassProgressLogDAO;
import dao.ClassResourceDAO;
import dao.StudentDAO;
import dao.TeacherDAO;
import exception.DuplicateRecordException;
import exception.EligibilityException;
import exception.InvalidDataException;
import exception.RecordNotFoundException;
import model.ClassEntity;
import model.ClassEnrollment;
import model.ClassEnrollment.EnrollmentStatus;
import model.ClassProgressLog;
import model.ClassResource;
import model.ClassResource.ResourceType;
import model.Student;
import model.Student.StudentStatus;
import model.Teacher;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ClassService {

    private final ClassDAO classDAO;
    private final TeacherDAO teacherDAO;
    private final StudentDAO studentDAO;
    private final ClassEnrollmentDAO enrollmentDAO;
    private final ClassProgressLogDAO progressLogDAO;
    private final ClassResourceDAO resourceDAO;

    public ClassService() {
        this.classDAO = new ClassDAO();
        this.teacherDAO = new TeacherDAO();
        this.studentDAO = new StudentDAO();
        this.enrollmentDAO = new ClassEnrollmentDAO();
        this.progressLogDAO = new ClassProgressLogDAO();
        this.resourceDAO = new ClassResourceDAO();
    }

    public ClassService(ClassDAO classDAO, TeacherDAO teacherDAO, StudentDAO studentDAO,
                        ClassEnrollmentDAO enrollmentDAO, ClassProgressLogDAO progressLogDAO,
                        ClassResourceDAO resourceDAO) {
        this.classDAO = classDAO;
        this.teacherDAO = teacherDAO;
        this.studentDAO = studentDAO;
        this.enrollmentDAO = enrollmentDAO;
        this.progressLogDAO = progressLogDAO;
        this.resourceDAO = resourceDAO;
    }

        public ClassEntity createClass(ClassEntity classEntity)
            throws InvalidDataException, DuplicateRecordException, RecordNotFoundException, SQLException {

        if (classEntity == null) {
            throw new InvalidDataException("Class data cannot be null.");
        }

        // Verify assigned instructor exists
        Optional<Teacher> teacher = teacherDAO.findById(classEntity.getTeacherId());
        if (!teacher.isPresent()) {
            throw new RecordNotFoundException("Cannot create class: Assigned teacher ID #" + classEntity.getTeacherId() + " does not exist.");
        }

        // Verify unique class code
        Optional<ClassEntity> existingCode = classDAO.findByClassCode(classEntity.getClassCode());
        if (existingCode.isPresent()) {
            throw new DuplicateRecordException("Class code '" + classEntity.getClassCode() + "' is already in use.");
        }

        return classDAO.create(classEntity);
    }

        public ClassEntity getClassById(int classId)
            throws RecordNotFoundException, SQLException, InvalidDataException {

        Optional<ClassEntity> ce = classDAO.findById(classId);
        if (!ce.isPresent()) {
            throw new RecordNotFoundException("Class section with ID #" + classId + " does not exist.");
        }
        return ce.get();
    }

        public List<ClassEntity> getAllClasses() throws SQLException, InvalidDataException {
        return classDAO.findAll();
    }

        public ClassEnrollment enrollStudent(int classId, int studentId, String performedBy)
            throws RecordNotFoundException, EligibilityException, DuplicateRecordException, InvalidDataException, SQLException {

        ClassEntity classEntity = getClassById(classId);
        if (classEntity.getStatus() == ClassEntity.ClassStatus.CANCELLED || classEntity.getStatus() == ClassEntity.ClassStatus.COMPLETED) {
            throw new EligibilityException("Cannot enroll in class: Course section is " + classEntity.getStatus() + ".");
        }

        // Check capacity limit
        List<ClassEnrollment> currentEnrollments = enrollmentDAO.findEnrollmentsByClassId(classId);
        if (currentEnrollments.size() >= classEntity.getMaxCapacity()) {
            throw new EligibilityException("Cannot enroll in class: Course has reached maximum capacity (" + classEntity.getMaxCapacity() + ").");
        }

        // Verify student existence and status
        Optional<Student> studentOpt = studentDAO.findById(studentId);
        if (!studentOpt.isPresent()) {
            throw new RecordNotFoundException("Student with ID #" + studentId + " does not exist.");
        }
        Student student = studentOpt.get();
        if (student.getStatus() != StudentStatus.ACTIVE) {
            throw new EligibilityException("Student is not eligible for enrollment (Academic Status: " + student.getStatus() + ").");
        }

        // Check existing enrollment
        Optional<ClassEnrollment> existingEnrollment = enrollmentDAO.findByClassAndStudent(classId, studentId);
        if (existingEnrollment.isPresent()) {
            throw new DuplicateRecordException("Student #" + studentId + " is already enrolled in class #" + classId + ".");
        }

        ClassEnrollment enrollment = new ClassEnrollment(
                classId, studentId, LocalDate.now(), 0.00, null, EnrollmentStatus.ENROLLED
        );

        // Execute atomic transaction (Enrollment + AuditLog on same connection)
        return enrollmentDAO.enrollStudentWithAudit(enrollment, performedBy);
    }

        public boolean updateAttendanceAndGrade(int enrollmentId, double attendance, String grade)
            throws InvalidDataException, RecordNotFoundException, SQLException {

        if (attendance < 0.00 || attendance > 100.00) {
            throw new InvalidDataException("Attendance percentage must be between 0.00 and 100.00.");
        }

        Optional<ClassEnrollment> enrollment = enrollmentDAO.findById(enrollmentId);
        if (!enrollment.isPresent()) {
            throw new RecordNotFoundException("Enrollment record #" + enrollmentId + " does not exist.");
        }

        return enrollmentDAO.updateAttendanceAndGrade(enrollmentId, attendance, grade);
    }

        public ClassProgressLog logProgress(int classId, LocalDate logDate, String topic, double hours, String remarks)
            throws RecordNotFoundException, InvalidDataException, SQLException {

        getClassById(classId); // Verify class exists
        ClassProgressLog log = new ClassProgressLog(classId, logDate, topic, hours, remarks);
        return progressLogDAO.create(log);
    }

        public List<ClassProgressLog> getProgressLogs(int classId)
            throws RecordNotFoundException, SQLException, InvalidDataException {

        getClassById(classId);
        return progressLogDAO.findByClassId(classId);
    }

        public ClassResource addResource(int classId, String title, ResourceType type, String url)
            throws RecordNotFoundException, InvalidDataException, SQLException {

        getClassById(classId);
        ClassResource resource = new ClassResource(classId, title, type, url);
        return resourceDAO.create(resource);
    }

        public List<ClassResource> getResources(int classId)
            throws RecordNotFoundException, SQLException, InvalidDataException {

        getClassById(classId);
        return resourceDAO.findByClassId(classId);
    }

        public List<AtRiskStudentReport> getAtRiskStudents(int classId)
            throws RecordNotFoundException, SQLException, InvalidDataException {

        ClassEntity classEntity = getClassById(classId);
        List<ClassEnrollment> enrollments = enrollmentDAO.findEnrollmentsByClassId(classId);
        List<AtRiskStudentReport> atRiskList = new ArrayList<>();

        for (ClassEnrollment e : enrollments) {
            boolean lowAttendance = e.getAttendancePercentage() < 60.00;
            boolean failingGrade = "F".equalsIgnoreCase(e.getGrade());

            if (lowAttendance || failingGrade) {
                Optional<Student> s = studentDAO.findById(e.getStudentId());
                String studentName = s.isPresent() ? s.get().getFullName() : "Unknown";
                String rollNumber = s.isPresent() ? s.get().getRollNumber() : "N/A";

                String reason = (lowAttendance && failingGrade)
                        ? "Critical: Low Attendance (" + e.getAttendancePercentage() + "%) AND Failing Grade (F)"
                        : lowAttendance ? "Warning: Low Attendance (" + e.getAttendancePercentage() + "% < 60%)"
                        : "Warning: Failing Grade (F)";

                atRiskList.add(new AtRiskStudentReport(
                        e.getEnrollmentId(), classId, classEntity.getClassCode(),
                        e.getStudentId(), rollNumber, studentName,
                        e.getAttendancePercentage(), e.getGrade(), reason
                ));
            }
        }
        return atRiskList;
    }

        public boolean deleteClass(int classId) throws RecordNotFoundException, SQLException {
        return classDAO.delete(classId);
    }

        public static class AtRiskStudentReport {
        private final int enrollmentId;
        private final int classId;
        private final String classCode;
        private final int studentId;
        private final String rollNumber;
        private final String studentName;
        private final double attendancePercentage;
        private final String grade;
        private final String riskReason;

        public AtRiskStudentReport(int enrollmentId, int classId, String classCode,
                                   int studentId, String rollNumber, String studentName,
                                   double attendancePercentage, String grade, String riskReason) {
            this.enrollmentId = enrollmentId;
            this.classId = classId;
            this.classCode = classCode;
            this.studentId = studentId;
            this.rollNumber = rollNumber;
            this.studentName = studentName;
            this.attendancePercentage = attendancePercentage;
            this.grade = grade;
            this.riskReason = riskReason;
        }

        public int getEnrollmentId() { return enrollmentId; }
        public int getClassId() { return classId; }
        public String getClassCode() { return classCode; }
        public int getStudentId() { return studentId; }
        public String getRollNumber() { return rollNumber; }
        public String getStudentName() { return studentName; }
        public double getAttendancePercentage() { return attendancePercentage; }
        public String getGrade() { return grade; }
        public String getRiskReason() { return riskReason; }

        @Override
        public String toString() {
            return String.format(
                    "AtRiskReport[Student: %s (%s) | Class: %s | Attendance: %.2f%% | Grade: %s | Reason: %s]",
                    studentName, rollNumber, classCode, attendancePercentage, grade != null ? grade : "N/A", riskReason
            );
        }
    }
}
