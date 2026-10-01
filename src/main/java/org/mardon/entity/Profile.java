package org.mardon.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "profile")
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
@ToString(onlyExplicitlyIncluded = true)
public class Profile {
    @Id
    @ToString.Include
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "profile_gen")
    @SequenceGenerator(
            name = "profile_gen",
            sequenceName = "profile_id_seq",
            allocationSize = 1
    )
    private Long id;

    @OneToOne
    @JoinColumn(name = "user_id")
//    @PrimaryKeyJoinColumn  // если первичный ключ это foreign key
    private User user;

    @ToString.Include
    private String street;

    @ToString.Include
    private String language;

    public void setUser(User user ){
        this.user = user;
//        if (user != null && user.getProfile() != this){
            user.setProfile(this);
//        }
    }
}
