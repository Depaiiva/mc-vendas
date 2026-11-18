package br.com.depaiva.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class UnitConverterTest {

    @Test
    @DisplayName("Converte quilogramas para gramas (fator 1000)")
    void convertsKilogramsToGrams() {
        BigDecimal result = UnitConverter.convertToBase(new BigDecimal("2.500"), Measure.KILO_GRAMS, Measure.GRAMS);
        assertEquals(new BigDecimal("2500.000"), result);
    }

    @Test
    @DisplayName("Converte litros para mililitros (fator 1000)")
    void convertsLitersToMilliliters() {
        BigDecimal result = UnitConverter.convertToBase(new BigDecimal("1.750"), Measure.LITERS, Measure.MILLILITERS);
        assertEquals(new BigDecimal("1750.000"), result);
    }

    @Test
    @DisplayName("Converte dúzia para unidades (fator 12)")
    void convertsDozenToUnits() {
        BigDecimal result = UnitConverter.convertToBase(new BigDecimal("2.000"), Measure.DOZEN, Measure.UNIT);
        assertEquals(new BigDecimal("24.000"), result);
    }

    @Test
    @DisplayName("Mantém quantidade inalterada quando já está na unidade base")
    void keepsQuantityWhenAlreadyInBaseUnit() {
        BigDecimal g = UnitConverter.convertToBase(new BigDecimal("500.000"), Measure.GRAMS, Measure.GRAMS);
        assertEquals(new BigDecimal("500.000"), g);

        BigDecimal ml = UnitConverter.convertToBase(new BigDecimal("250.000"), Measure.MILLILITERS, Measure.MILLILITERS);
        assertEquals(new BigDecimal("250.000"), ml);

        BigDecimal un = UnitConverter.convertToBase(new BigDecimal("6.000"), Measure.UNIT, Measure.UNIT);
        assertEquals(new BigDecimal("6.000"), un);
    }

    @Test
    @DisplayName("Lança exceção ao tentar converter entre grandezas incompatíveis (ex: kg para ml)")
    void throwsOnIncompatibleUnits() {
        assertThrows(IllegalArgumentException.class, () ->
                UnitConverter.convertToBase(new BigDecimal("1.000"), Measure.KILO_GRAMS, Measure.MILLILITERS));

        assertThrows(IllegalArgumentException.class, () ->
                UnitConverter.convertToBase(new BigDecimal("1.000"), Measure.LITERS, Measure.UNIT));
    }

    @Test
    @DisplayName("Lança exceção para quantidade menor ou igual a zero")
    void throwsOnInvalidQuantity() {
        assertThrows(IllegalArgumentException.class, () ->
                UnitConverter.convertToBase(BigDecimal.ZERO, Measure.GRAMS, Measure.GRAMS));

        assertThrows(IllegalArgumentException.class, () ->
                UnitConverter.convertToBase(new BigDecimal("-1.000"), Measure.GRAMS, Measure.GRAMS));
    }
}
