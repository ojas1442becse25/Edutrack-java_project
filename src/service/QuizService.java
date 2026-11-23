package service;

import dao.ClassDAO;
import dao.ClassEnrollmentDAO;
import dao.QuizDAO;
import dao.QuizSubmissionDAO;
import dao.StudentDAO;
import exception.DuplicateRecordException;
import exception.EligibilityException;
import exception.InvalidDataException;
import exception.RecordNotFoundException;
import model.ClassEnrollment;
import model.Quiz;
import model.Quiz.QuizStatus;
import model.QuizSubmission;
import model.QuizSubmission.SubmissionStatus;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Service Layer class governing quiz authoring, publishing workflows,
 * deadline enforcement, submission processing, and grading calculations.
 */
public class QuizService {

    private final QuizDAO quizDAO;
    private final QuizSubmissionDAO submissionDAO;
    private final ClassDAO classDAO;
    private final StudentDAO studentDAO;
    private final ClassEnrollmentDAO enrollmentDAO;

    public QuizService() {
        this.quizDAO = new QuizDAO();
        this.submissionDAO = new QuizSubmissionDAO();
        this.classDAO = new ClassDAO();
        this.studentDAO = new StudentDAO();
        this.enrollmentDAO = new ClassEnrollmentDAO();
    }

    public QuizService(QuizDAO quizDAO, QuizSubmissionDAO submissionDAO,
                       ClassDAO classDAO, StudentDAO studentDAO, ClassEnrollmentDAO enrollmentDAO) {
        this.quizDAO = quizDAO;
        this.submissionDAO = submissionDAO;
        this.classDAO = classDAO;
        this.studentDAO = studentDAO;
        this.enrollmentDAO = enrollmentDAO;
    }

    /**
     * Creates a new quiz after validating associated course existence and marks criteria.
     */
    public Quiz createQuiz(Quiz quiz)
            throws InvalidDataException, RecordNotFoundException, SQLException {

        if (quiz == null) {
            throw new InvalidDataException("Quiz entity cannot be null.");
        }

        // Verify class section exists
        if (!classDAO.findById(quiz.getClassId()).isPresent()) {
            throw new RecordNotFoundException("Cannot create quiz: Associated Class ID #" + quiz.getClassId() + " does not exist.");
        }

        return quizDAO.create(quiz);
    }

    /**
     * Publishes a quiz, making it visible and open for student submissions.
     */
    public boolean publishQuiz(int quizId) throws RecordNotFoundException, SQLException, InvalidDataException {
        getQuizById(quizId);
        return quizDAO.updateStatus(quizId, QuizStatus.PUBLISHED);
    }

    /**
     * Closes a quiz to prevent any further student submissions.
     */
    public boolean closeQuiz(int quizId) throws RecordNotFoundException, SQLException, InvalidDataException {
        getQuizById(quizId);
        return quizDAO.updateStatus(quizId, QuizStatus.CLOSED);
    }

    /**
     * Finds a quiz by ID.
     */
    public Quiz getQuizById(int quizId)
            throws RecordNotFoundException, SQLException, InvalidDataException {

        Optional<Quiz> quiz = quizDAO.findById(quizId);
        if (!quiz.isPresent()) {
            throw new RecordNotFoundException("Quiz with ID #" + quizId + " does not exist.");
        }
        return quiz.get();
    }

    /**
     * Submits a student's quiz attempt with strict business validation:
     * 1. Quiz must exist and be in PUBLISHED status.
     * 2. Submission timestamp must be before quiz dueDate (throwing EligibilityException).
     * 3. Student must be actively enrolled in the course section.
     * 4. Student must not have already submitted this quiz (prevent duplicate attempts).
     * 5. Score obtained must be in range [0, totalMarks].
     */
    public QuizSubmission submitQuiz(int quizId, int studentId, double scoreObtained, String remarks)
            throws RecordNotFoundException, EligibilityException, DuplicateRecordException, InvalidDataException, SQLException {

        Quiz quiz = getQuizById(quizId);

        // Rule 1: Status must be PUBLISHED
        if (quiz.getStatus() != QuizStatus.PUBLISHED) {
            throw new EligibilityException("Quiz #" + quizId + " is not open for submission (Status: " + quiz.getStatus() + ").");
        }

        // Rule 2: Deadline check
        if (LocalDateTime.now().isAfter(quiz.getDueDate())) {
            throw new EligibilityException("Submission rejected: Deadline for Quiz #" + quizId + " passed on " + quiz.getDueDate() + ".");
        }

        // Rule 3: Enrollment verification
        Optional<ClassEnrollment> enrollment = enrollmentDAO.findByClassAndStudent(quiz.getClassId(), studentId);
        if (!enrollment.isPresent() || enrollment.get().getStatus() != ClassEnrollment.EnrollmentStatus.ENROLLED) {
            throw new EligibilityException("Student #" + studentId + " is not enrolled in Class #" + quiz.getClassId() + ".");
        }

        // Rule 4: Prevent duplicate submission
        Optional<QuizSubmission> existing = submissionDAO.findByQuizAndStudent(quizId, studentId);
        if (existing.isPresent()) {
            throw new DuplicateRecordException("Student #" + studentId + " has already submitted Quiz #" + quizId + ".");
        }

        // Rule 5: Score validation
        if (scoreObtained < 0.00 || scoreObtained > quiz.getTotalMarks()) {
            throw new InvalidDataException("Score (" + scoreObtained + ") must be between 0.00 and Total Marks (" + quiz.getTotalMarks() + ").");
        }

        QuizSubmission submission = new QuizSubmission(
                quizId, studentId, scoreObtained, SubmissionStatus.SUBMITTED, remarks
        );

        return submissionDAO.submit(submission);
    }

    /**
     * Evaluates whether a score qualifies as a PASS based on the quiz's passing criteria.
     */
    public boolean isPassed(double scoreObtained, int quizId)
            throws RecordNotFoundException, SQLException, InvalidDataException {

        Quiz quiz = getQuizById(quizId);
        return scoreObtained >= quiz.getPassingMarks();
    }

    /**
     * Records teacher evaluation feedback and updates score.
     */
    public boolean evaluateSubmission(int submissionId, double newScore, SubmissionStatus status, String evaluationFeedback)
            throws RecordNotFoundException, InvalidDataException, SQLException {

        Optional<QuizSubmission> sub = submissionDAO.findById(submissionId);
        if (!sub.isPresent()) {
            throw new RecordNotFoundException("Quiz submission #" + submissionId + " does not exist.");
        }

        Quiz quiz = getQuizById(sub.get().getQuizId());
        if (newScore < 0.00 || newScore > quiz.getTotalMarks()) {
            throw new InvalidDataException("Evaluated score (" + newScore + ") cannot exceed quiz total marks (" + quiz.getTotalMarks() + ").");
        }

        return submissionDAO.evaluate(submissionId, newScore, status, evaluationFeedback);
    }

    /**
     * Retrieves all student submissions for a quiz.
     */
    public List<QuizSubmission> getSubmissionsByQuiz(int quizId)
            throws RecordNotFoundException, SQLException, InvalidDataException {

        getQuizById(quizId);
        return submissionDAO.findByQuizId(quizId);
    }

    /**
     * Retrieves all quiz submissions for a student.
     */
    public List<QuizSubmission> getSubmissionsByStudent(int studentId)
            throws SQLException, InvalidDataException {

        return submissionDAO.findByStudentId(studentId);
    }

    /**
     * Deletes a quiz.
     */
    public boolean deleteQuiz(int quizId) throws RecordNotFoundException, SQLException {
        return quizDAO.delete(quizId);
    }
}
