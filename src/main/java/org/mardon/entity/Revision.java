package org.mardon.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.envers.RevisionEntity;
import org.hibernate.envers.RevisionTimestamp;
import org.mardon.listener.MardonRevisionListener;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Table(name = "revinfo")
@RevisionEntity(MardonRevisionListener.class)  // в проекте должна быть одна
public class Revision {

    @Id
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "revinfo_seqgen"

    )
    @SequenceGenerator(sequenceName = "revinfo_seq", name = "revinfo_seqgen", allocationSize = 50, initialValue = 50)
    private int id;

    @RevisionTimestamp
    private Long timeStamp;

    private String username;
}
