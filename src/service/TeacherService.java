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

        public boolean updateTeacher(Teacher teacher)
            throws InvalidDataException, DuplicateRecordException, RecordNotFoundException, SQLException {

        if (teacher == null || teacher.getTeacherId() == null) {
            throw new InvalidDataException("Teacher entity and ID are required for update.");
        }

        getTeacherById(teacher.getTeacherId());
        return teacherDAO.update(teacher);
    }

        public Teacher getTeacherById(int teacherId)
            throws RecordNotFoundException, SQLException, InvalidDataException {

        Optional<Teacher> teacher = teacherDAO.findById(teacherId);
        if (!teacher.isPresent()) {
            throw new RecordNotFoundException("Teacher with ID #" + teacherId + " does not exist.");
        }
        return teacher.get();
    }

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

        public List<Teacher> getAllTeachers() throws SQLException, InvalidDataException {
        return teacherDAO.findAll();
    }

        public List<Teacher> getTeachersByDepartment(String department)
            throws InvalidDataException, SQLException {

        if (department == null || department.trim().isEmpty()) {
            throw new InvalidDataException("Department cannot be empty.");
        }
        return teacherDAO.findByDepartment(department.trim());
    }

        public List<ClassEntity> getAssignedClasses(int teacherId)
            throws RecordNotFoundException, SQLException, InvalidDataException {

        getTeacherById(teacherId);
        return classDAO.findClassesByTeacherId(teacherId);
    }

        public boolean deleteTeacher(int teacherId) throws RecordNotFoundException, SQLException {
        return teacherDAO.delete(teacherId);
    }
}
