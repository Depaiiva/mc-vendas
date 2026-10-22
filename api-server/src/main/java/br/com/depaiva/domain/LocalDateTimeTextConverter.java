package br.com.depaiva.domain;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.time.LocalDateTime;

@Converter(autoApply = true)
public class LocalDateTimeTextConverter implements AttributeConverter<LocalDateTime, String> {
    @Override
    public String convertToDatabaseColumn(LocalDateTime value) {
        return value == null ? null : value.toString();
    }

    @Override
    public LocalDateTime convertToEntityAttribute(String value) {
        return value == null ? null : LocalDateTime.parse(value);
    }
}
