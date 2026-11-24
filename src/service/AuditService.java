package service;

import dao.AuditLogDAO;
import exception.InvalidDataException;
import model.AuditLog;

import java.sql.SQLException;
import java.util.List;

/**
 * Service Layer class providing secure read access to system audit logs
 * adhering strictly to the 3-layer architecture.
 */
public class AuditService {

    private final AuditLogDAO auditLogDAO;

    public AuditService() {
        this.auditLogDAO = new AuditLogDAO();
    }

    public AuditService(AuditLogDAO auditLogDAO) {
        this.auditLogDAO = auditLogDAO;
    }

    /**
     * Retrieves the most recent system audit trail entries.
     */
    public List<AuditLog> getRecentAuditLogs(int limit) throws SQLException, InvalidDataException {
        return auditLogDAO.findRecent(limit > 0 ? limit : 25);
    }

    /**
     * Retrieves audit trail entries for a specific entity.
     */
    public List<AuditLog> getAuditLogsByEntity(String entityName, int entityId) throws SQLException, InvalidDataException {
        return auditLogDAO.findByEntity(entityName, entityId);
    }
}
