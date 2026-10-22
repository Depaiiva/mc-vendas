package br.com.depaiva.domain;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.math.BigDecimal;
import java.math.RoundingMode;

@Converter
public class QuantityConverter implements AttributeConverter<BigDecimal, String> {
    @Override
    public String convertToDatabaseColumn(BigDecimal value) {
        return value == null ? null : value.setScale(3, RoundingMode.HALF_UP).toPlainString();
    }

    @Override
    public BigDecimal convertToEntityAttribute(String value) {
        return value == null ? null : new BigDecimal(value).setScale(3, RoundingMode.HALF_UP);
    }
}
