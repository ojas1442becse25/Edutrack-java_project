package dao;

import database.DBConnection;
import exception.InvalidDataException;
import exception.RecordNotFoundException;
import model.Quiz;
import model.Quiz.QuizStatus;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Data Access Object for the QUIZ table.
 */
public class QuizDAO {

    private static final String SQL_INSERT =
            "INSERT INTO QUIZ (class_id, title, total_marks, passing_marks, due_date, status) " +
            "VALUES (?, ?, ?, ?, ?, ?)";

    private static final String SQL_FIND_BY_ID =
            "SELECT quiz_id, class_id, title, total_marks, passing_marks, due_date, status, created_at " +
            "FROM QUIZ WHERE quiz_id = ?";

    private static final String SQL_FIND_BY_CLASS =
            "SELECT quiz_id, class_id, title, total_marks, passing_marks, due_date, status, created_at " +
            "FROM QUIZ WHERE class_id = ? ORDER BY due_date ASC";

    private static final String SQL_FIND_ACTIVE_BY_CLASS =
            "SELECT quiz_id, class_id, title, total_marks, passing_marks, due_date, status, created_at " +
            "FROM QUIZ WHERE class_id = ? AND status = 'PUBLISHED' ORDER BY due_date ASC";

    private static final String SQL_UPDATE_STATUS =
            "UPDATE QUIZ SET status = ? WHERE quiz_id = ?";

    private static final String SQL_UPDATE =
            "UPDATE QUIZ SET title = ?, total_marks = ?, passing_marks = ?, due_date = ?, status = ? WHERE quiz_id = ?";

    private static final String SQL_DELETE =
            "DELETE FROM QUIZ WHERE quiz_id = ?";

    /**
     * Creates a new quiz.
     */
    public Quiz create(Quiz quiz) throws SQLException {
        if (quiz == null) {
            throw new IllegalArgumentException("Quiz cannot be null.");
        }

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_INSERT, Statement.RETURN_GENERATED_KEYS)) {

            ps.setInt(1, quiz.getClassId());
            ps.setString(2, quiz.getTitle());
            ps.setInt(3, quiz.getTotalMarks());
            ps.setInt(4, quiz.getPassingMarks());
            ps.setTimestamp(5, Timestamp.valueOf(quiz.getDueDate()));
            ps.setString(6, quiz.getStatus() != null ? quiz.getStatus().name() : QuizStatus.PUBLISHED.name());

            int affectedRows = ps.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Creating quiz failed, no rows affected.");
            }

            try (ResultSet generatedKeys = ps.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    quiz.setQuizId(generatedKeys.getInt(1));
                }
            }
            return quiz;
        }
    }

    /**
     * Finds a quiz by ID.
     */
    public Optional<Quiz> findById(int quizId) throws SQLException, InvalidDataException {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_FIND_BY_ID)) {

            ps.setInt(1, quizId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToQuiz(rs));
                }
            }
        }
        return Optional.empty();
    }

    /**
     * Retrieves all quizzes for a specific class.
     */
    public List<Quiz> findByClassId(int classId) throws SQLException, InvalidDataException {
        List<Quiz> list = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_FIND_BY_CLASS)) {

            ps.setInt(1, classId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToQuiz(rs));
                }
            }
        }
        return list;
    }

    /**
     * Retrieves currently active (PUBLISHED) quizzes for a class.
     */
    public List<Quiz> findActiveQuizzesByClassId(int classId) throws SQLException, InvalidDataException {
        List<Quiz> list = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_FIND_ACTIVE_BY_CLASS)) {

            ps.setInt(1, classId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToQuiz(rs));
                }
            }
        }
        return list;
    }

    /**
     * Publishes or unpublishes a quiz by changing its status (DRAFT, PUBLISHED, CLOSED).
     */
    public boolean updateStatus(int quizId, QuizStatus status) throws SQLException, RecordNotFoundException {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_UPDATE_STATUS)) {

            ps.setString(1, status != null ? status.name() : QuizStatus.PUBLISHED.name());
            ps.setInt(2, quizId);

            int affectedRows = ps.executeUpdate();
            if (affectedRows == 0) {
                throw new RecordNotFoundException("Quiz with ID " + quizId + " not found for status update.");
            }
            return true;
        }
    }

    /**
     * Updates an existing quiz.
     */
    public boolean update(Quiz quiz) throws SQLException, RecordNotFoundException {
        if (quiz == null || quiz.getQuizId() == null) {
            throw new IllegalArgumentException("Quiz and Quiz ID cannot be null for update.");
        }

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_UPDATE)) {

            ps.setString(1, quiz.getTitle());
            ps.setInt(2, quiz.getTotalMarks());
            ps.setInt(3, quiz.getPassingMarks());
            ps.setTimestamp(4, Timestamp.valueOf(quiz.getDueDate()));
            ps.setString(5, quiz.getStatus() != null ? quiz.getStatus().name() : QuizStatus.PUBLISHED.name());
            ps.setInt(6, quiz.getQuizId());

            int affectedRows = ps.executeUpdate();
            if (affectedRows == 0) {
                throw new RecordNotFoundException("Quiz with ID " + quiz.getQuizId() + " not found for update.");
            }
            return true;
        }
    }

    /**
     * Deletes a quiz by ID.
     */
    public boolean delete(int quizId) throws SQLException, RecordNotFoundException {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_DELETE)) {

            ps.setInt(1, quizId);
            int affectedRows = ps.executeUpdate();
            if (affectedRows == 0) {
                throw new RecordNotFoundException("Quiz with ID " + quizId + " not found for deletion.");
            }
            return true;
        }
    }

    private Quiz mapResultSetToQuiz(ResultSet rs) throws SQLException, InvalidDataException {
        Timestamp dueTs = rs.getTimestamp("due_date");
        Timestamp createTs = rs.getTimestamp("created_at");

        return new Quiz(
                rs.getInt("quiz_id"),
                rs.getInt("class_id"),
                rs.getString("title"),
                rs.getInt("total_marks"),
                rs.getInt("passing_marks"),
                dueTs != null ? dueTs.toLocalDateTime() : null,
                QuizStatus.fromString(rs.getString("status")),
                createTs != null ? createTs.toLocalDateTime() : null
        );
    }
}
