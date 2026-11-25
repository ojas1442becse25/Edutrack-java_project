package dao;

import database.DBConnection;
import exception.DuplicateRecordException;
import exception.InvalidDataException;
import exception.RecordNotFoundException;
import model.QuizSubmission;
import model.QuizSubmission.SubmissionStatus;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class QuizSubmissionDAO {

    private static final String SQL_INSERT =
            "INSERT INTO QUIZ_SUBMISSION (quiz_id, student_id, score_obtained, submission_date, status, remarks) " +
            "VALUES (?, ?, ?, ?, ?, ?)";

    private static final String SQL_FIND_BY_ID =
            "SELECT submission_id, quiz_id, student_id, score_obtained, submission_date, status, remarks " +
            "FROM QUIZ_SUBMISSION WHERE submission_id = ?";

    private static final String SQL_FIND_BY_QUIZ =
            "SELECT submission_id, quiz_id, student_id, score_obtained, submission_date, status, remarks " +
            "FROM QUIZ_SUBMISSION WHERE quiz_id = ? ORDER BY score_obtained DESC";

    private static final String SQL_FIND_BY_STUDENT =
            "SELECT submission_id, quiz_id, student_id, score_obtained, submission_date, status, remarks " +
            "FROM QUIZ_SUBMISSION WHERE student_id = ? ORDER BY submission_date DESC";

    private static final String SQL_FIND_BY_QUIZ_AND_STUDENT =
            "SELECT submission_id, quiz_id, student_id, score_obtained, submission_date, status, remarks " +
            "FROM QUIZ_SUBMISSION WHERE quiz_id = ? AND student_id = ?";

    private static final String SQL_EVALUATE =
            "UPDATE QUIZ_SUBMISSION SET score_obtained = ?, status = ?, remarks = ? WHERE submission_id = ?";

    private static final String SQL_DELETE =
            "DELETE FROM QUIZ_SUBMISSION WHERE submission_id = ?";

        public QuizSubmission submit(QuizSubmission submission) throws SQLException, DuplicateRecordException {
        if (submission == null) {
            throw new IllegalArgumentException("QuizSubmission cannot be null.");
        }

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_INSERT, Statement.RETURN_GENERATED_KEYS)) {

            ps.setInt(1, submission.getQuizId());
            ps.setInt(2, submission.getStudentId());
            ps.setDouble(3, submission.getScoreObtained());
            ps.setTimestamp(4, Timestamp.valueOf(submission.getSubmissionDate() != null ? submission.getSubmissionDate() : java.time.LocalDateTime.now()));
            ps.setString(5, submission.getStatus() != null ? submission.getStatus().name() : SubmissionStatus.EVALUATED.name());
            ps.setString(6, submission.getRemarks());

            int affectedRows = ps.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Creating quiz submission failed, no rows affected.");
            }

            try (ResultSet generatedKeys = ps.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    submission.setSubmissionId(generatedKeys.getInt(1));
                }
            }
            return submission;
        } catch (SQLException e) {
            // check duplicate key
            if ("23000".equals(e.getSQLState()) || e.getErrorCode() == 1062) {
                throw new DuplicateRecordException("Student has already submitted this quiz: " + e.getMessage(), e);
            }
            throw e;
        }
    }

        public Optional<QuizSubmission> findById(int submissionId) throws SQLException, InvalidDataException {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_FIND_BY_ID)) {

            ps.setInt(1, submissionId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToSubmission(rs));
                }
            }
        }
        return Optional.empty();
    }

        public List<QuizSubmission> findByQuizId(int quizId) throws SQLException, InvalidDataException {
        List<QuizSubmission> list = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_FIND_BY_QUIZ)) {

            ps.setInt(1, quizId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToSubmission(rs));
                }
            }
        }
        return list;
    }

        public List<QuizSubmission> findByStudentId(int studentId) throws SQLException, InvalidDataException {
        List<QuizSubmission> list = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_FIND_BY_STUDENT)) {

            ps.setInt(1, studentId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToSubmission(rs));
                }
            }
        }
        return list;
    }

        public Optional<QuizSubmission> findByQuizAndStudent(int quizId, int studentId) throws SQLException, InvalidDataException {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_FIND_BY_QUIZ_AND_STUDENT)) {

            ps.setInt(1, quizId);
            ps.setInt(2, studentId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToSubmission(rs));
                }
            }
        }
        return Optional.empty();
    }

        public boolean evaluate(int submissionId, double score, SubmissionStatus status, String remarks)
            throws SQLException, RecordNotFoundException {

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_EVALUATE)) {

            ps.setDouble(1, score);
            ps.setString(2, status != null ? status.name() : SubmissionStatus.EVALUATED.name());
            ps.setString(3, remarks);
            ps.setInt(4, submissionId);

            int affectedRows = ps.executeUpdate();
            if (affectedRows == 0) {
                throw new RecordNotFoundException("Quiz submission with ID " + submissionId + " not found for evaluation.");
            }
            return true;
        }
    }

        public boolean delete(int submissionId) throws SQLException, RecordNotFoundException {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_DELETE)) {

            ps.setInt(1, submissionId);
            int affectedRows = ps.executeUpdate();
            if (affectedRows == 0) {
                throw new RecordNotFoundException("Quiz submission with ID " + submissionId + " not found for deletion.");
            }
            return true;
        }
    }

    private QuizSubmission mapResultSetToSubmission(ResultSet rs) throws SQLException, InvalidDataException {
        Timestamp subTs = rs.getTimestamp("submission_date");
        return new QuizSubmission(
                rs.getInt("submission_id"),
                rs.getInt("quiz_id"),
                rs.getInt("student_id"),
                rs.getDouble("score_obtained"),
                subTs != null ? subTs.toLocalDateTime() : null,
                SubmissionStatus.fromString(rs.getString("status")),
                rs.getString("remarks")
        );
    }
}
