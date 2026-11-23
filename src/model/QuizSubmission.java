package model;

import exception.InvalidDataException;
import java.time.LocalDateTime;

/**
 * Domain model representing the QUIZ_SUBMISSION table.
 */
public class QuizSubmission {

    public enum SubmissionStatus {
        SUBMITTED, EVALUATED, REJECTED;

        public static SubmissionStatus fromString(String val) throws InvalidDataException {
            if (val == null || val.trim().isEmpty()) {
                return EVALUATED;
            }
            try {
                return SubmissionStatus.valueOf(val.trim().toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new InvalidDataException("Invalid Submission status: " + val + ". Allowed: SUBMITTED, EVALUATED, REJECTED");
            }
        }
    }

    private Integer submissionId;
    private int quizId;
    private int studentId;
    private double scoreObtained;
    private LocalDateTime submissionDate;
    private SubmissionStatus status;
    private String remarks;

    public QuizSubmission() {
        this.scoreObtained = 0.00;
        this.submissionDate = LocalDateTime.now();
        this.status = SubmissionStatus.EVALUATED;
    }

    public QuizSubmission(int quizId, int studentId, double scoreObtained,
                          SubmissionStatus status, String remarks) throws InvalidDataException {
        setQuizId(quizId);
        setStudentId(studentId);
        setScoreObtained(scoreObtained);
        setStatus(status);
        setRemarks(remarks);
        this.submissionDate = LocalDateTime.now();
    }

    public QuizSubmission(Integer submissionId, int quizId, int studentId, double scoreObtained,
                          LocalDateTime submissionDate, SubmissionStatus status, String remarks) throws InvalidDataException {
        this(quizId, studentId, scoreObtained, status, remarks);
        this.submissionId = submissionId;
        this.submissionDate = submissionDate;
    }

    public Integer getSubmissionId() {
        return submissionId;
    }

    public void setSubmissionId(Integer submissionId) {
        this.submissionId = submissionId;
    }

    public int getQuizId() {
        return quizId;
    }

    public void setQuizId(int quizId) throws InvalidDataException {
        if (quizId <= 0) {
            throw new InvalidDataException("Quiz ID must be positive.");
        }
        this.quizId = quizId;
    }

    public int getStudentId() {
        return studentId;
    }

    public void setStudentId(int studentId) throws InvalidDataException {
        if (studentId <= 0) {
            throw new InvalidDataException("Student ID must be positive.");
        }
        this.studentId = studentId;
    }

    public double getScoreObtained() {
        return scoreObtained;
    }

    public void setScoreObtained(double scoreObtained) throws InvalidDataException {
        if (scoreObtained < 0.00) {
            throw new InvalidDataException("Score obtained cannot be negative: " + scoreObtained);
        }
        this.scoreObtained = Math.round(scoreObtained * 100.0) / 100.0;
    }

    public LocalDateTime getSubmissionDate() {
        return submissionDate;
    }

    public void setSubmissionDate(LocalDateTime submissionDate) {
        this.submissionDate = submissionDate != null ? submissionDate : LocalDateTime.now();
    }

    public SubmissionStatus getStatus() {
        return status;
    }

    public void setStatus(SubmissionStatus status) {
        this.status = status != null ? status : SubmissionStatus.EVALUATED;
    }

    public void setStatus(String statusStr) throws InvalidDataException {
        this.status = SubmissionStatus.fromString(statusStr);
    }

    public String getRemarks() {
        return remarks;
    }

    public void setRemarks(String remarks) {
        this.remarks = remarks != null ? remarks.trim() : null;
    }

    @Override
    public String toString() {
        return String.format("QuizSubmission[ID=%d, QuizID=%d, StudentID=%d, Score=%.2f, Status=%s]",
                submissionId, quizId, studentId, scoreObtained, status);
    }
}
