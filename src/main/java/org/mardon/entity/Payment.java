package org.mardon.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;
import org.hibernate.envers.RelationTargetAuditMode;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString(onlyExplicitlyIncluded = true)
@Entity
@Builder
@Table(name = "payments")
@Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED) // не аудирует сущности, коллекции не в счёт
//@OptimisticLocking(type = OptimisticLockType.VERSION) // решает last commit wins
// dirty - смотрит контекст и првоеряет измененные поля только
// все поля, а не версию
//@OptimisticLocking(type = OptimisticLockType.ALL)
//@DynamicUpdate  // не будет использовать стандартрный update (обновление всех полей)
public class Payment extends AuditableEntity<Long>{
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "payment_gen")
    @SequenceGenerator(
            name = "payment_gen",
            sequenceName = "payments_id_seq",
            allocationSize = 1
    )
    @ToString.Include
    private Long id;

    // last commit wins problem
    @Version
    private Long version;

    @ToString.Include
    private Integer amount;

//    @NotAudited
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receiver_id")
    private User receiver;

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
