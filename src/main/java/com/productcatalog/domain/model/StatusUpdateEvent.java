package com.productcatalog.domain.model;

public class StatusUpdateEvent {

    public enum EntityType { PRODUCT, TRACK }

    private final EntityType entityType;
    private final String entityId;
    private final String productId;  // null for product updates
    private final String status;
    private final String notes;
    private final ChangedByType changedByType;

    public StatusUpdateEvent(EntityType entityType, String entityId, String productId,
                             String status, String notes, ChangedByType changedByType) {
        this.entityType = entityType;
        this.entityId = entityId;
        this.productId = productId;
        this.status = status;
        this.notes = notes;
        this.changedByType = changedByType;
    }

    public EntityType getEntityType() { return entityType; }
    public String getEntityId() { return entityId; }
    public String getProductId() { return productId; }
    public String getStatus() { return status; }
    public String getNotes() { return notes; }
    public ChangedByType getChangedByType() { return changedByType; }
}