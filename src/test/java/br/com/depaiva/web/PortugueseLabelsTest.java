package br.com.depaiva.web;

import static org.junit.jupiter.api.Assertions.assertEquals;

import br.com.depaiva.domain.Measure;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PortugueseLabelsTest {
    private PortugueseLabels labels;

    @BeforeEach
    void setUp() {
        labels = new PortugueseLabels();
    }

    @Test
    void formatsWholeQuantitiesWithoutDecimalZeros() {
        assertEquals("1", labels.quantity(new BigDecimal("1.000")));
        assertEquals("2", labels.quantity(new BigDecimal("2.000")));
        assertEquals("500", labels.quantity(new BigDecimal("500.000")));
        assertEquals("1.000", labels.quantity(new BigDecimal("1000.000")));
    }

    @Test
    void formatsDecimalQuantitiesWithComma() {
        assertEquals("1,25", labels.quantity(new BigDecimal("1.250")));
        assertEquals("0,5", labels.quantity(new BigDecimal("0.500")));
        assertEquals("2,755", labels.quantity(new BigDecimal("2.755")));
    }

    @Test
    void formatsNullAndZeroQuantities() {
        assertEquals("0", labels.quantity(null));
        assertEquals("0", labels.quantity(BigDecimal.ZERO));
        assertEquals("0", labels.quantity(new BigDecimal("0.000")));
    }

    @Test
    void formatsQuantityWithUnit() {
        assertEquals("1 kg", labels.quantityWithUnit(new BigDecimal("1.000"), Measure.KILO_GRAMS));
        assertEquals("1,25 kg", labels.quantityWithUnit(new BigDecimal("1.250"), Measure.KILO_GRAMS));
        assertEquals("500 g", labels.quantityWithUnit(new BigDecimal("500.000"), Measure.GRAMS));
        assertEquals("0,75 L", labels.quantityWithUnit(new BigDecimal("0.750"), Measure.LITERS));
        assertEquals("250 ml", labels.quantityWithUnit(new BigDecimal("250.000"), Measure.MILLILITERS));
    }
}
