package org.mardon.entity;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public record Birthday(LocalDate birthdate) implements Serializable {

    public long getAge(){
        return ChronoUnit.YEARS.between(birthdate, LocalDate.now());
    }
}
