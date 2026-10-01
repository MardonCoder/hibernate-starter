package org.mardon.entity;

import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.envers.Audited;
import org.mardon.listener.AuditDateListener;

import java.io.Serializable;
import java.time.Instant;

@Getter
@Setter
@MappedSuperclass
@EntityListeners(AuditDateListener.class)
//@Audited
public abstract class AuditableEntity<T extends Serializable> implements BaseEntity<T> {
    private Instant createdAt;
    private String createdBy;

    private Instant updatedAt;
    private String updatedBy;

//    @PrePersist
//    public void prePersist() {
//        setCreatedAt(Instant.now());
////        setCreatedBy();
//    }
//
//    @PreUpdate
//    public void preUpdate(){
//        setUpdatedAt(Instant.now());
////        setUpdatedBy();
//    }
}
