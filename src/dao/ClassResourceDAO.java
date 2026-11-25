package dao;

import database.DBConnection;
import exception.InvalidDataException;
import exception.RecordNotFoundException;
import model.ClassResource;
import model.ClassResource.ResourceType;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ClassResourceDAO {

    private static final String SQL_INSERT =
            "INSERT INTO CLASS_RESOURCE (class_id, title, resource_type, resource_url) " +
            "VALUES (?, ?, ?, ?)";

    private static final String SQL_FIND_BY_ID =
            "SELECT resource_id, class_id, title, resource_type, resource_url, uploaded_at " +
            "FROM CLASS_RESOURCE WHERE resource_id = ?";

    private static final String SQL_FIND_BY_CLASS =
            "SELECT resource_id, class_id, title, resource_type, resource_url, uploaded_at " +
            "FROM CLASS_RESOURCE WHERE class_id = ? ORDER BY uploaded_at DESC";

    private static final String SQL_UPDATE =
            "UPDATE CLASS_RESOURCE SET title = ?, resource_type = ?, resource_url = ? WHERE resource_id = ?";

    private static final String SQL_DELETE =
            "DELETE FROM CLASS_RESOURCE WHERE resource_id = ?";

        public ClassResource create(ClassResource resource) throws SQLException {
        if (resource == null) {
            throw new IllegalArgumentException("ClassResource cannot be null.");
        }

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_INSERT, Statement.RETURN_GENERATED_KEYS)) {

            ps.setInt(1, resource.getClassId());
            ps.setString(2, resource.getTitle());
            ps.setString(3, resource.getResourceType() != null ? resource.getResourceType().name() : ResourceType.OTHER.name());
            ps.setString(4, resource.getResourceUrl());

            int affectedRows = ps.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Creating resource failed, no rows affected.");
            }

            try (ResultSet generatedKeys = ps.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    resource.setResourceId(generatedKeys.getInt(1));
                }
            }
            return resource;
        }
    }

        public Optional<ClassResource> findById(int resourceId) throws SQLException, InvalidDataException {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_FIND_BY_ID)) {

            ps.setInt(1, resourceId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToResource(rs));
                }
            }
        }
        return Optional.empty();
    }

        public List<ClassResource> findByClassId(int classId) throws SQLException, InvalidDataException {
        List<ClassResource> list = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_FIND_BY_CLASS)) {

            ps.setInt(1, classId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToResource(rs));
                }
            }
        }
        return list;
    }

        public boolean update(ClassResource resource) throws SQLException, RecordNotFoundException {
        if (resource == null || resource.getResourceId() == null) {
            throw new IllegalArgumentException("Resource and Resource ID cannot be null for update.");
        }

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_UPDATE)) {

            ps.setString(1, resource.getTitle());
            ps.setString(2, resource.getResourceType() != null ? resource.getResourceType().name() : ResourceType.OTHER.name());
            ps.setString(3, resource.getResourceUrl());
            ps.setInt(4, resource.getResourceId());

            int affectedRows = ps.executeUpdate();
            if (affectedRows == 0) {
                throw new RecordNotFoundException("Resource with ID " + resource.getResourceId() + " not found for update.");
            }
            return true;
        }
    }

        public boolean delete(int resourceId) throws SQLException, RecordNotFoundException {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_DELETE)) {

            ps.setInt(1, resourceId);
            int affectedRows = ps.executeUpdate();
            if (affectedRows == 0) {
                throw new RecordNotFoundException("Resource with ID " + resourceId + " not found for deletion.");
            }
            return true;
        }
    }

    private ClassResource mapResultSetToResource(ResultSet rs) throws SQLException, InvalidDataException {
        Timestamp ts = rs.getTimestamp("uploaded_at");
        return new ClassResource(
                rs.getInt("resource_id"),
                rs.getInt("class_id"),
                rs.getString("title"),
                ResourceType.fromString(rs.getString("resource_type")),
                rs.getString("resource_url"),
                ts != null ? ts.toLocalDateTime() : null
        );
    }
}
