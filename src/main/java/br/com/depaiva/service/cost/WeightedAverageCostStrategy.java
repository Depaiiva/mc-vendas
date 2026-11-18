package br.com.depaiva.service.cost;

import br.com.depaiva.domain.Ingredient;
import br.com.depaiva.domain.IngredientPurchase;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class WeightedAverageCostStrategy implements IngredientCostStrategy {

    @Override
    public Optional<BigDecimal> calculateUnitCost(Ingredient ingredient, List<IngredientPurchase> purchases) {
        if (purchases == null || purchases.isEmpty()) {
            return Optional.empty();
        }

        BigDecimal totalSpent = BigDecimal.ZERO;
        BigDecimal totalQuantity = BigDecimal.ZERO;

        for (IngredientPurchase purchase : purchases) {
            totalSpent = totalSpent.add(purchase.getTotalCost());
            totalQuantity = totalQuantity.add(purchase.getQuantity());
        }

        if (totalQuantity.signum() <= 0) {
            return Optional.empty();
        }

        // Custo médio ponderado = soma dos valores pagos / soma das quantidades compradas
        return Optional.of(totalSpent.divide(totalQuantity, 6, RoundingMode.HALF_UP));
    }
}
