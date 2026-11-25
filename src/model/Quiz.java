package model;

import exception.InvalidDataException;
import java.time.LocalDateTime;

public class Quiz {

    public enum QuizStatus {
        DRAFT, PUBLISHED, CLOSED;

        public static QuizStatus fromString(String val) throws InvalidDataException {
            if (val == null || val.trim().isEmpty()) {
                return PUBLISHED;
            }
            try {
                return QuizStatus.valueOf(val.trim().toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new InvalidDataException("Invalid Quiz status: " + val + ". Allowed: DRAFT, PUBLISHED, CLOSED");
            }
        }
    }

    private Integer quizId;
    private int classId;
    private String title;
    private int totalMarks;
    private int passingMarks;
    private LocalDateTime dueDate;
    private QuizStatus status;
    private LocalDateTime createdAt;

    public Quiz() {
        this.totalMarks = 20;
        this.passingMarks = 8;
        this.status = QuizStatus.PUBLISHED;
    }

    public Quiz(int classId, String title, int totalMarks, int passingMarks,
                LocalDateTime dueDate, QuizStatus status) throws InvalidDataException {
        setClassId(classId);
        setTitle(title);
        setMarks(totalMarks, passingMarks);
        setDueDate(dueDate);
        setStatus(status);
    }

    public Quiz(Integer quizId, int classId, String title, int totalMarks, int passingMarks,
                LocalDateTime dueDate, QuizStatus status, LocalDateTime createdAt) throws InvalidDataException {
        this(classId, title, totalMarks, passingMarks, dueDate, status);
        this.quizId = quizId;
        this.createdAt = createdAt;
    }

    public Integer getQuizId() {
        return quizId;
    }

    public void setQuizId(Integer quizId) {
        this.quizId = quizId;
    }

    public int getClassId() {
        return classId;
    }

    public void setClassId(int classId) throws InvalidDataException {
        if (classId <= 0) {
            throw new InvalidDataException("Class ID must be positive.");
        }
        this.classId = classId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) throws InvalidDataException {
        if (title == null || title.trim().isEmpty()) {
            throw new InvalidDataException("Quiz title cannot be empty.");
        }
        if (title.trim().length() > 150) {
            throw new InvalidDataException("Quiz title cannot exceed 150 characters.");
        }
        this.title = title.trim();
    }

    public int getTotalMarks() {
        return totalMarks;
    }

    public int getPassingMarks() {
        return passingMarks;
    }

    public void setMarks(int totalMarks, int passingMarks) throws InvalidDataException {
        if (totalMarks <= 0) {
            throw new InvalidDataException("Total marks must be greater than zero.");
        }
        if (passingMarks <= 0) {
            throw new InvalidDataException("Passing marks must be greater than zero.");
        }
        if (passingMarks > totalMarks) {
            throw new InvalidDataException("Passing marks (" + passingMarks + ") cannot exceed total marks (" + totalMarks + ").");
        }
        this.totalMarks = totalMarks;
        this.passingMarks = passingMarks;
    }

    public void setTotalMarks(int totalMarks) throws InvalidDataException {
        setMarks(totalMarks, this.passingMarks > 0 ? this.passingMarks : (totalMarks / 2));
    }

    public void setPassingMarks(int passingMarks) throws InvalidDataException {
        setMarks(this.totalMarks > 0 ? this.totalMarks : passingMarks, passingMarks);
    }

    public LocalDateTime getDueDate() {
        return dueDate;
    }

    public void setDueDate(LocalDateTime dueDate) throws InvalidDataException {
        if (dueDate == null) {
            throw new InvalidDataException("Quiz due date cannot be null.");
        }
        this.dueDate = dueDate;
    }

    public QuizStatus getStatus() {
        return status;
    }

    public void setStatus(QuizStatus status) {
        this.status = status != null ? status : QuizStatus.PUBLISHED;
    }

    public void setStatus(String statusStr) throws InvalidDataException {
        this.status = QuizStatus.fromString(statusStr);
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return String.format("Quiz[ID=%d, ClassID=%d, Title='%s', Marks=%d/%d, Due=%s, Status=%s]",
                quizId, classId, title, passingMarks, totalMarks, dueDate, status);
    }
}
