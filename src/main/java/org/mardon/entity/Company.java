package org.mardon.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;

import java.io.Serializable;
import java.util.*;

@Entity
@Table(name = "company", schema = "public")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(onlyExplicitlyIncluded = true)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Audited
@Cacheable
@org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.READ_WRITE, region = "Companies")
public class Company {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "company_gen")
    @SequenceGenerator(
            name = "company_gen",
            sequenceName = "company_id_seq",
            allocationSize = 1
    )
    @ToString.Include
    private Integer id;

    @EqualsAndHashCode.Include
    @Column(nullable = false, unique = true)
    @ToString.Include
    private String name;

    @Builder.Default
    @ElementCollection
    @CollectionTable(name = "company_locale", joinColumns = @JoinColumn(name = "company_id"))
    @AttributeOverride(name = "lang", column = @Column(name = "lang"))
    @NotAudited
    private List<LocaleInfo> locales = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "company")  // mappedBY для bidirectional
    @OrderBy("username ASC")
//    @JoinColumn(name = "company_id") // необязательно если bidirectional
    @NotAudited
    @org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    private Set<User> employees = new LinkedHashSet<>();

//    @Builder.Default
//    @OneToMany(mappedBy = "company", cascade = CascadeType.ALL)
//    @MapKey(name = "username")
//    @SortNatural
//    private Map<String, User> users = new TreeMap<>();

    public void addUser(User user){
        employees.add(user);
        if (user != null && user.getCompany() != this)
            user.setCompany(this);
    }

}
