package org.mardon.entity;

import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Embeddable
@Access(AccessType.FIELD)
public class PersonalInfo {
    private String firstname;
    private String lastname;

//    @Convert(converter = BirthdayConverter.class)
//    @Column(name = "birth_date")
    @NotNull
    private Birthday birthDate;
}
