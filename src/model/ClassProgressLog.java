package model;

import exception.InvalidDataException;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class ClassProgressLog {

    private Integer logId;
    private int classId;
    private LocalDate logDate;
    private String topicCovered;
    private double hoursSpent;
    private String remarks;
    private LocalDateTime createdAt;

    public ClassProgressLog() {
        this.logDate = LocalDate.now();
        this.hoursSpent = 1.0;
    }

    public ClassProgressLog(int classId, LocalDate logDate, String topicCovered,
                            double hoursSpent, String remarks) throws InvalidDataException {
        setClassId(classId);
        setLogDate(logDate);
        setTopicCovered(topicCovered);
        setHoursSpent(hoursSpent);
        setRemarks(remarks);
    }

    public ClassProgressLog(Integer logId, int classId, LocalDate logDate, String topicCovered,
                            double hoursSpent, String remarks, LocalDateTime createdAt) throws InvalidDataException {
        this(classId, logDate, topicCovered, hoursSpent, remarks);
        this.logId = logId;
        this.createdAt = createdAt;
    }

    public Integer getLogId() {
        return logId;
    }

    public void setLogId(Integer logId) {
        this.logId = logId;
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

    public LocalDate getLogDate() {
        return logDate;
    }

    public void setLogDate(LocalDate logDate) throws InvalidDataException {
        if (logDate == null) {
            throw new InvalidDataException("Log date cannot be null.");
        }
        this.logDate = logDate;
    }

    public String getTopicCovered() {
        return topicCovered;
    }

    public void setTopicCovered(String topicCovered) throws InvalidDataException {
        if (topicCovered == null || topicCovered.trim().isEmpty()) {
            throw new InvalidDataException("Topic covered cannot be empty.");
        }
        if (topicCovered.trim().length() > 200) {
            throw new InvalidDataException("Topic covered cannot exceed 200 characters.");
        }
        this.topicCovered = topicCovered.trim();
    }

    public double getHoursSpent() {
        return hoursSpent;
    }

    public void setHoursSpent(double hoursSpent) throws InvalidDataException {
        if (hoursSpent <= 0.00 || hoursSpent > 12.00) {
            throw new InvalidDataException("Hours spent must be greater than 0 and at most 12 hours.");
        }
        this.hoursSpent = Math.round(hoursSpent * 100.0) / 100.0;
    }

    public String getRemarks() {
        return remarks;
    }

    public void setRemarks(String remarks) {
        this.remarks = remarks != null ? remarks.trim() : null;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return String.format("ClassProgressLog[ID=%d, ClassID=%d, Date=%s, Topic='%s', Hours=%.2f]",
                logId, classId, logDate, topicCovered, hoursSpent);
    }
}
