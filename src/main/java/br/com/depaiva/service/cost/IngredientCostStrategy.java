package br.com.depaiva.service.cost;

import br.com.depaiva.domain.Ingredient;
import br.com.depaiva.domain.IngredientPurchase;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface IngredientCostStrategy {
    Optional<BigDecimal> calculateUnitCost(Ingredient ingredient, List<IngredientPurchase> purchases);
}
