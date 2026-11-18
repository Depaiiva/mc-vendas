package br.com.depaiva.service.cost;

import br.com.depaiva.domain.Ingredient;
import br.com.depaiva.domain.IngredientPurchase;
import br.com.depaiva.domain.Product;
import br.com.depaiva.domain.ProductIngredient;
import br.com.depaiva.repository.IngredientPurchaseRepository;
import br.com.depaiva.repository.ProductIngredientRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProductCostService {
    private final ProductIngredientRepository productIngredients;
    private final IngredientPurchaseRepository purchases;
    private final IngredientCostStrategy costStrategy;
    private final BigDecimal defaultLossMargin;

    public ProductCostService(ProductIngredientRepository productIngredients,
                              IngredientPurchaseRepository purchases,
                              IngredientCostStrategy costStrategy,
                              @Value("${app.cost.loss-margin:0.0}") BigDecimal defaultLossMargin) {
        this.productIngredients = productIngredients;
        this.purchases = purchases;
        this.costStrategy = costStrategy;
        this.defaultLossMargin = defaultLossMargin != null ? defaultLossMargin : BigDecimal.ZERO;
    }

    @Transactional(readOnly = true)
    public ProductCostResult calculateUnitCost(Product product) {
        return calculateUnitCost(product, defaultLossMargin);
    }

    @Transactional(readOnly = true)
    public ProductCostResult calculateUnitCost(Product product, BigDecimal lossMargin) {
        if (product == null) {
            throw new IllegalArgumentException("Produto é obrigatório.");
        }
        List<ProductIngredient> recipe = productIngredients.findAllByProductId(product.getId());
        if (recipe == null || recipe.isEmpty()) {
            return ProductCostResult.missingRecipe(product);
        }

        BigDecimal effectiveLossMargin = lossMargin != null ? lossMargin : BigDecimal.ZERO;
        if (effectiveLossMargin.signum() < 0) {
            throw new IllegalArgumentException("A margem de perda não pode ser negativa.");
        }

        BigDecimal baseTotalCost = BigDecimal.ZERO;
        List<Ingredient> missingIngredients = new ArrayList<>();

        for (ProductIngredient item : recipe) {
            Ingredient ingredient = item.getIngredient();
            List<IngredientPurchase> ingredientPurchases = purchases.findAllByIngredientIdOrderByDateDesc(ingredient.getId());
            Optional<BigDecimal> unitCostOpt = costStrategy.calculateUnitCost(ingredient, ingredientPurchases);

            if (unitCostOpt.isEmpty()) {
                missingIngredients.add(ingredient);
            } else {
                BigDecimal quantityPerUnit = item.getQuantityPerUnit();
                BigDecimal ingredientTotalCost = quantityPerUnit.multiply(unitCostOpt.get());
                baseTotalCost = baseTotalCost.add(ingredientTotalCost);
            }
        }

        if (!missingIngredients.isEmpty()) {
            return ProductCostResult.missingPurchases(product, missingIngredients);
        }

        BigDecimal multiplier = BigDecimal.ONE.add(effectiveLossMargin);
        BigDecimal finalCost = baseTotalCost.multiply(multiplier).setScale(2, RoundingMode.HALF_UP);
        return ProductCostResult.complete(product, finalCost, baseTotalCost);
    }
}
