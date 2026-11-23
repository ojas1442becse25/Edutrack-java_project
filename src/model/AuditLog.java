package model;

import exception.InvalidDataException;
import java.time.LocalDateTime;

/**
 * Domain model representing the AUDIT_LOG table.
 */
public class AuditLog {

    public enum ActionType {
        INSERT, UPDATE, DELETE, LOGIN, LOGOUT;

        public static ActionType fromString(String val) throws InvalidDataException {
            if (val == null || val.trim().isEmpty()) {
                throw new InvalidDataException("Action type cannot be empty.");
            }
            try {
                return ActionType.valueOf(val.trim().toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new InvalidDataException("Invalid Action type: " + val + ". Allowed: INSERT, UPDATE, DELETE, LOGIN, LOGOUT");
            }
        }
    }

    private Integer logId;
    private String entityName;
    private int entityId;
    private ActionType actionType;
    private String performedBy;
    private LocalDateTime actionTimestamp;
    private String details;

    public AuditLog() {
        this.actionTimestamp = LocalDateTime.now();
    }

    public AuditLog(String entityName, int entityId, ActionType actionType,
                    String performedBy, String details) throws InvalidDataException {
        setEntityName(entityName);
        setEntityId(entityId);
        setActionType(actionType);
        setPerformedBy(performedBy);
        setDetails(details);
        this.actionTimestamp = LocalDateTime.now();
    }

    public AuditLog(Integer logId, String entityName, int entityId, ActionType actionType,
                    String performedBy, LocalDateTime actionTimestamp, String details) throws InvalidDataException {
        this(entityName, entityId, actionType, performedBy, details);
        this.logId = logId;
        this.actionTimestamp = actionTimestamp;
    }

    public Integer getLogId() {
        return logId;
    }

    public void setLogId(Integer logId) {
        this.logId = logId;
    }

    public String getEntityName() {
        return entityName;
    }

    public void setEntityName(String entityName) throws InvalidDataException {
        if (entityName == null || entityName.trim().isEmpty()) {
            throw new InvalidDataException("Entity name cannot be empty.");
        }
        if (entityName.trim().length() > 50) {
            throw new InvalidDataException("Entity name cannot exceed 50 characters.");
        }
        this.entityName = entityName.trim().toUpperCase();
    }

    public int getEntityId() {
        return entityId;
    }

    public void setEntityId(int entityId) {
        this.entityId = entityId;
    }

    public ActionType getActionType() {
        return actionType;
    }

    public void setActionType(ActionType actionType) throws InvalidDataException {
        if (actionType == null) {
            throw new InvalidDataException("Action type cannot be null.");
        }
        this.actionType = actionType;
    }

    public void setActionType(String actionTypeStr) throws InvalidDataException {
        this.actionType = ActionType.fromString(actionTypeStr);
    }

    public String getPerformedBy() {
        return performedBy;
    }

    public void setPerformedBy(String performedBy) throws InvalidDataException {
        if (performedBy == null || performedBy.trim().isEmpty()) {
            throw new InvalidDataException("Performed by username cannot be empty.");
        }
        if (performedBy.trim().length() > 100) {
            throw new InvalidDataException("Performed by username cannot exceed 100 characters.");
        }
        this.performedBy = performedBy.trim();
    }

    public LocalDateTime getActionTimestamp() {
        return actionTimestamp;
    }

    public void setActionTimestamp(LocalDateTime actionTimestamp) {
        this.actionTimestamp = actionTimestamp != null ? actionTimestamp : LocalDateTime.now();
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details != null ? details.trim() : null;
    }

    @Override
    public String toString() {
        return String.format("AuditLog[ID=%d, Entity='%s'#%d, Action=%s, User='%s', Time=%s]",
                logId, entityName, entityId, actionType, performedBy, actionTimestamp);
    }
}
