package service;

import dao.ClassDAO;
import dao.TeacherDAO;
import exception.DuplicateRecordException;
import exception.InvalidDataException;
import exception.RecordNotFoundException;
import model.ClassEntity;
import model.Teacher;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/**
 * Service Layer class encapsulating teacher onboarding, profile management,
 * and course allocation lookups.
 */
public class TeacherService {

    private final TeacherDAO teacherDAO;
    private final ClassDAO classDAO;

    public TeacherService() {
        this.teacherDAO = new TeacherDAO();
        this.classDAO = new ClassDAO();
    }

    public TeacherService(TeacherDAO teacherDAO, ClassDAO classDAO) {
        this.teacherDAO = teacherDAO;
        this.classDAO = classDAO;
    }

    /**
     * Registers a new teacher after validating business rules.
     */
    public Teacher registerTeacher(Teacher teacher)
            throws InvalidDataException, DuplicateRecordException, SQLException {

        if (teacher == null) {
            throw new InvalidDataException("Teacher data cannot be null.");
        }

        Optional<Teacher> existingEmail = teacherDAO.findByEmail(teacher.getEmail());
        if (existingEmail.isPresent()) {
            throw new DuplicateRecordException("Teacher with Email '" + teacher.getEmail() + "' is already registered.");
        }

        return teacherDAO.create(teacher);
    }

    /**
     * Updates an existing teacher profile.
     */
    public boolean updateTeacher(Teacher teacher)
            throws InvalidDataException, DuplicateRecordException, RecordNotFoundException, SQLException {

        if (teacher == null || teacher.getTeacherId() == null) {
            throw new InvalidDataException("Teacher entity and ID are required for update.");
        }

        getTeacherById(teacher.getTeacherId());
        return teacherDAO.update(teacher);
    }

    /**
     * Finds a teacher by primary key ID.
     */
    public Teacher getTeacherById(int teacherId)
            throws RecordNotFoundException, SQLException, InvalidDataException {

        Optional<Teacher> teacher = teacherDAO.findById(teacherId);
        if (!teacher.isPresent()) {
            throw new RecordNotFoundException("Teacher with ID #" + teacherId + " does not exist.");
        }
        return teacher.get();
    }

    /**
     * Finds a teacher by email address.
     */
    public Teacher getTeacherByEmail(String email)
            throws RecordNotFoundException, SQLException, InvalidDataException {

        if (email == null || email.trim().isEmpty()) {
            throw new InvalidDataException("Email cannot be empty.");
        }
        Optional<Teacher> teacher = teacherDAO.findByEmail(email.trim());
        if (!teacher.isPresent()) {
            throw new RecordNotFoundException("Teacher with Email '" + email + "' does not exist.");
        }
        return teacher.get();
    }

    /**
     * Retrieves all registered teachers.
     */
    public List<Teacher> getAllTeachers() throws SQLException, InvalidDataException {
        return teacherDAO.findAll();
    }

    /**
     * Retrieves teachers belonging to a specific academic department.
     */
    public List<Teacher> getTeachersByDepartment(String department)
            throws InvalidDataException, SQLException {

        if (department == null || department.trim().isEmpty()) {
            throw new InvalidDataException("Department cannot be empty.");
        }
        return teacherDAO.findByDepartment(department.trim());
    }

    /**
     * Retrieves all classes/course sections assigned to an instructor.
     */
    public List<ClassEntity> getAssignedClasses(int teacherId)
            throws RecordNotFoundException, SQLException, InvalidDataException {

        getTeacherById(teacherId);
        return classDAO.findClassesByTeacherId(teacherId);
    }

    /**
     * Deletes a teacher record.
     */
    public boolean deleteTeacher(int teacherId) throws RecordNotFoundException, SQLException {
        return teacherDAO.delete(teacherId);
    }
}
