package br.com.depaiva.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class UnitConverter {
    private UnitConverter() {}

    public static BigDecimal convertToBase(BigDecimal quantity, Measure sourceUnit, Measure targetBaseUnit) {
        if (quantity == null || quantity.signum() <= 0) {
            throw new IllegalArgumentException("A quantidade deve ser maior que zero.");
        }
        if (sourceUnit == null || targetBaseUnit == null) {
            throw new IllegalArgumentException("As unidades devem ser informadas.");
        }
        if (!targetBaseUnit.isBaseUnit()) {
            throw new IllegalArgumentException("A unidade alvo deve ser uma unidade base (g, ml ou un).");
        }
        if (sourceUnit.getBaseUnit() != targetBaseUnit) {
            throw new IllegalArgumentException("Unidade incompatível: " + sourceUnit + " não pode ser convertida para " + targetBaseUnit + ".");
        }
        return sourceUnit.convertToBase(quantity);
    }
}
