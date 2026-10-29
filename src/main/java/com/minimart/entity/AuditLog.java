package com.minimart.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "AUDIT_LOG", schema = "dbo")
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "audit_id")
    private Long id;

    @Column(name = "actor_id")
    private Long actorId;

    @Column(nullable = false, length = 80)
    private String action;

    @Column(name = "entity_name", nullable = false, length = 80)
    private String entityName;

    @Column(name = "entity_id", length = 80)
    private String entityId;

    @Column(name = "old_values", columnDefinition = "nvarchar(max)")
    private String oldValues;

    @Column(name = "new_values", columnDefinition = "nvarchar(max)")
    private String newValues;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private LocalDateTime createdAt;

    protected AuditLog() {
    }

    public AuditLog(Long actorId, String action, String entityName, String entityId,
                    String oldValues, String newValues) {
        this.actorId = actorId;
        this.action = action;
        this.entityName = entityName;
        this.entityId = entityId;
        this.oldValues = oldValues;
        this.newValues = newValues;
    }

    public Long getId() {
        return id;
    }

    public Long getActorId() {
        return actorId;
    }

    public String getAction() {
        return action;
    }

    public String getEntityName() {
        return entityName;
    }

    public String getEntityId() {
        return entityId;
    }

    public String getOldValues() {
        return oldValues;
    }

    public String getNewValues() {
        return newValues;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
