package service;

import dao.AuditLogDAO;
import exception.InvalidDataException;
import model.AuditLog;

import java.sql.SQLException;
import java.util.List;

public class AuditService {

    private final AuditLogDAO auditLogDAO;

    public AuditService() {
        this.auditLogDAO = new AuditLogDAO();
    }

    public AuditService(AuditLogDAO auditLogDAO) {
        this.auditLogDAO = auditLogDAO;
    }

        public List<AuditLog> getRecentAuditLogs(int limit) throws SQLException, InvalidDataException {
        return auditLogDAO.findRecent(limit > 0 ? limit : 25);
    }

        public List<AuditLog> getAuditLogsByEntity(String entityName, int entityId) throws SQLException, InvalidDataException {
        return auditLogDAO.findByEntity(entityName, entityId);
    }
}
