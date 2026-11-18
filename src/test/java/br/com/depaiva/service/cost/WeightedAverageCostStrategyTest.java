package br.com.depaiva.service.cost;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.com.depaiva.domain.Ingredient;
import br.com.depaiva.domain.IngredientPurchase;
import br.com.depaiva.domain.Measure;
import br.com.depaiva.domain.Origin;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class WeightedAverageCostStrategyTest {

    private final WeightedAverageCostStrategy strategy = new WeightedAverageCostStrategy();
    private final Ingredient flour = new Ingredient("Farinha de Trigo", Measure.GRAMS);

    @Test
    @DisplayName("Custo médio ponderado deve dividir a soma dos valores pagos pela soma das quantidades compradas")
    void calculatesWeightedAverageCostCorrectly() {
        // Compra 1: 1000g por R$ 10.00 (R$ 0.010/g)
        IngredientPurchase p1 = new IngredientPurchase(
                LocalDate.of(2026, 11, 1),
                new BigDecimal("1000.000"),
                new BigDecimal("0.0100"),
                new BigDecimal("10.00"),
                Origin.MANUAL,
                flour
        );

        // Compra 2: 2000g por R$ 26.00 (R$ 0.013/g)
        IngredientPurchase p2 = new IngredientPurchase(
                LocalDate.of(2026, 11, 3),
                new BigDecimal("2000.000"),
                new BigDecimal("0.0130"),
                new BigDecimal("26.00"),
                Origin.MANUAL,
                flour
        );

        // Total pago = 10.00 + 26.00 = 36.00
        // Total quantidade = 1000 + 2000 = 3000g
        // Custo médio ponderado = 36.00 / 3000 = 0.012000 R$/g
        Optional<BigDecimal> cost = strategy.calculateUnitCost(flour, List.of(p1, p2));

        assertTrue(cost.isPresent());
        assertEquals(new BigDecimal("0.012000"), cost.get().setScale(6, RoundingMode.HALF_UP));
    }

    @Test
    @DisplayName("Retorna Optional.empty quando não houver compras registradas para o ingrediente")
    void returnsEmptyWhenNoPurchases() {
        Optional<BigDecimal> cost = strategy.calculateUnitCost(flour, List.of());
        assertTrue(cost.isEmpty());

        Optional<BigDecimal> costNull = strategy.calculateUnitCost(flour, null);
        assertTrue(costNull.isEmpty());
    }
}
