package org.mardon.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.mardon.entity.Birthday;

import java.time.LocalDate;
import java.util.Optional;

@Converter(autoApply = true)
public class BirthdayConverter implements AttributeConverter<Birthday, LocalDate> {

    @Override
    public LocalDate convertToDatabaseColumn(Birthday birthday) {
        return Optional.ofNullable(birthday)
                .map(Birthday::birthdate)
                .orElse(null);
    }

    @Override
    public Birthday convertToEntityAttribute(LocalDate date) {
        return Optional.ofNullable(date)
                .map(Birthday::new)
                .orElse(null);
    }
}
