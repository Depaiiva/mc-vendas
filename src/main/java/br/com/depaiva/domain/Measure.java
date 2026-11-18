package br.com.depaiva.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;

public enum Measure {
    GRAMS,
    KILO_GRAMS,
    LITERS,
    MILLILITERS,
    UNIT,
    DOZEN;

    public boolean isBaseUnit() {
        return this == GRAMS || this == MILLILITERS || this == UNIT;
    }

    public Measure getBaseUnit() {
        return switch (this) {
            case GRAMS, KILO_GRAMS -> GRAMS;
            case MILLILITERS, LITERS -> MILLILITERS;
            case UNIT, DOZEN -> UNIT;
        };
    }

    public BigDecimal getConversionFactor() {
        return switch (this) {
            case GRAMS, MILLILITERS, UNIT -> BigDecimal.ONE;
            case KILO_GRAMS, LITERS -> new BigDecimal("1000");
            case DOZEN -> new BigDecimal("12");
        };
    }

    public BigDecimal convertToBase(BigDecimal quantity) {
        if (quantity == null) {
            return BigDecimal.ZERO.setScale(3, RoundingMode.HALF_UP);
        }
        return quantity.multiply(getConversionFactor()).setScale(3, RoundingMode.HALF_UP);
    }
}
