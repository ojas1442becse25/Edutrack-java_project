package model;

import exception.InvalidDataException;
import java.time.LocalDateTime;

/**
 * Domain model representing the CLASS_RESOURCE table.
 */
public class ClassResource {

    public enum ResourceType {
        SYLLABUS, NOTES, ASSIGNMENT, VIDEO, SLIDES, OTHER;

        public static ResourceType fromString(String val) throws InvalidDataException {
            if (val == null || val.trim().isEmpty()) {
                return OTHER;
            }
            try {
                return ResourceType.valueOf(val.trim().toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new InvalidDataException("Invalid Resource type: " + val + ". Allowed: SYLLABUS, NOTES, ASSIGNMENT, VIDEO, SLIDES, OTHER");
            }
        }
    }

    private Integer resourceId;
    private int classId;
    private String title;
    private ResourceType resourceType;
    private String resourceUrl;
    private LocalDateTime uploadedAt;

    public ClassResource() {
        this.resourceType = ResourceType.NOTES;
    }

    public ClassResource(int classId, String title, ResourceType resourceType, String resourceUrl) throws InvalidDataException {
        setClassId(classId);
        setTitle(title);
        setResourceType(resourceType);
        setResourceUrl(resourceUrl);
    }

    public ClassResource(Integer resourceId, int classId, String title, ResourceType resourceType,
                         String resourceUrl, LocalDateTime uploadedAt) throws InvalidDataException {
        this(classId, title, resourceType, resourceUrl);
        this.resourceId = resourceId;
        this.uploadedAt = uploadedAt;
    }

    public Integer getResourceId() {
        return resourceId;
    }

    public void setResourceId(Integer resourceId) {
        this.resourceId = resourceId;
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
            throw new InvalidDataException("Resource title cannot be empty.");
        }
        if (title.trim().length() > 150) {
            throw new InvalidDataException("Resource title cannot exceed 150 characters.");
        }
        this.title = title.trim();
    }

    public ResourceType getResourceType() {
        return resourceType;
    }

    public void setResourceType(ResourceType resourceType) {
        this.resourceType = resourceType != null ? resourceType : ResourceType.OTHER;
    }

    public void setResourceType(String resourceTypeStr) throws InvalidDataException {
        this.resourceType = ResourceType.fromString(resourceTypeStr);
    }

    public String getResourceUrl() {
        return resourceUrl;
    }

    public void setResourceUrl(String resourceUrl) throws InvalidDataException {
        if (resourceUrl == null || resourceUrl.trim().isEmpty()) {
            throw new InvalidDataException("Resource URL cannot be empty.");
        }
        if (resourceUrl.trim().length() > 255) {
            throw new InvalidDataException("Resource URL cannot exceed 255 characters.");
        }
        this.resourceUrl = resourceUrl.trim();
    }

    public LocalDateTime getUploadedAt() {
        return uploadedAt;
    }

    public void setUploadedAt(LocalDateTime uploadedAt) {
        this.uploadedAt = uploadedAt;
    }

    @Override
    public String toString() {
        return String.format("ClassResource[ID=%d, ClassID=%d, Title='%s', Type=%s, URL='%s']",
                resourceId, classId, title, resourceType, resourceUrl);
    }
}
