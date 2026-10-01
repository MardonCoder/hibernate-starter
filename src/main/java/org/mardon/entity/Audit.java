package org.mardon.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class Audit {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "audit_gen")
    @SequenceGenerator(
            name = "audit_gen",
            sequenceName = "audit_id_seq",
            allocationSize = 1
    )
    private Long id;

    @Column(name = "entity_id")
    private String entityId; // Заменено с Object

    @Column(name = "entity_name")
    private String entityName;

    @Column(name = "entity_content")
    private String entityContent;

    @Enumerated(EnumType.STRING)
    private Operation operation; // Теперь пишется как строка

    public enum Operation {
        SAVE, UPDATE, DELETE, INSERT
    }
}
