package org.mardon.listener;

import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import org.mardon.entity.AuditableEntity;

import java.time.Instant;

public class AuditDateListener {
    @PrePersist
    public void prePersist(AuditableEntity<?> entity) {
        entity.setCreatedAt(Instant.now());
//        setCreatedBy();
    }

    @PreUpdate
    public void preUpdate(AuditableEntity<?> entity){
        entity.setUpdatedAt(Instant.now());
//        setUpdatedBy();
    }
}
