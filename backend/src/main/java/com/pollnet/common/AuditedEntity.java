package com.pollnet.common;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;

@Getter
@Setter
@MappedSuperclass
public abstract class AuditedEntity extends BaseEntity {

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    protected void onCreateAudited() {
        // BaseEntity.onCreate() runs first via JPA's superclass callback chain;
        // by the time we get here, createdAt is set.
        if (updatedAt == null) {
            updatedAt = getCreatedAt() != null ? getCreatedAt() : OffsetDateTime.now();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }
}
