package org.mardon.entity;

import jakarta.persistence.*;
import jakarta.persistence.CascadeType;
import jakarta.persistence.NamedEntityGraph;
import jakarta.persistence.NamedQuery;
import jakarta.validation.Valid;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.*;
import org.hibernate.annotations.Cache;
import org.hibernate.type.SqlTypes;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;


@NamedEntityGraph(
        name = "WithCompanyAndPayments",
        attributeNodes = {
                @NamedAttributeNode("company"),
                @NamedAttributeNode(value = "payments", subgraph = "paymentsMore")
        },
        subgraphs = {
                @NamedSubgraph(name = "paymentsMore", attributeNodes = @NamedAttributeNode("amount"))
        }
)
@FetchProfile(name = "withCompanyAndPayments", fetchOverrides = {
        @FetchProfile.FetchOverride(
                entity = User.class, association = "company", mode = FetchMode.JOIN
        ),
        @FetchProfile.FetchOverride(
                entity = User.class, association = "payments", mode = FetchMode.JOIN
        )
})
@NamedQuery(name = "FindUser", query = "select u from User u " +
        "left join u.company c " +
        "where u.personalInfo.firstname = :firstname and c.name = :companyName " +
        "order by u.personalInfo.lastname desc")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(onlyExplicitlyIncluded = true)
@SuperBuilder
@Entity
@Table(name = "users", schema = "public")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "type")
@Cacheable
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE, region = "Users")
public class User implements Serializable {
    @Serial
    private static final long serialVersionUID = -140244517970513571L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "user_gen")
    @SequenceGenerator(
            name = "user_gen",
            sequenceName = "users_id_seq",
            allocationSize = 1
    )
    @ToString.Include
    private Long id;

    @EqualsAndHashCode.Include
    @Column(unique = true, nullable = false, updatable = false)
    @ToString.Include
    private String username;  // must be serializable

    @Valid // because personalInfo has constraint
    @Embedded
    @AttributeOverride(name = "birthDate", column = @Column(name = "birth_date"))
    @ToString.Include
    private PersonalInfo personalInfo;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    @ToString.Include
    private String info;

    @Enumerated(EnumType.STRING)
    @ToString.Include
    private Role role;

    @ToString.Include
    @ManyToOne(optional = true , cascade = {CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REFRESH, CascadeType.DETACH}, fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id")
//    @Fetch(FetchMode.JOIN)
    private Company company;

    @Builder.Default
//    @BatchSize(size = 2) // теперь будут сразу батчем доставаться
//    @Fetch(FetchMode.SUBSELECT)
    @OneToMany(mappedBy = "receiver", cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    private List<Payment> payments = new ArrayList<>();

//    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL)
//    private Profile profile;

    public void setProfile(Profile profile){
//        this.profile = profile;
        if (profile != null && profile.getUser() != this){
            profile.setUser(this);
        }
    }

    public void setCompany(Company c){
        this.company = c;
        if (c != null && !c.getEmployees().contains(this))
            c.addUser(this);
    }

    public void addPayment(Payment payment){
        if (payment != null) {
            payments.add(payment);
            payment.setReceiver(this);
        }
    }
}
