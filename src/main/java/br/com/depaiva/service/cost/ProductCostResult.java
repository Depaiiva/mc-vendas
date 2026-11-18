package br.com.depaiva.service.cost;

import br.com.depaiva.domain.Ingredient;
import br.com.depaiva.domain.Product;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.stream.Collectors;

public record ProductCostResult(
        Product product,
        boolean complete,
        BigDecimal unitCost,
        BigDecimal baseCost,
        List<Ingredient> missingIngredients,
        String message
) {
    public static ProductCostResult complete(Product product, BigDecimal finalCost, BigDecimal baseCost) {
        return new ProductCostResult(product, true, finalCost.setScale(2, RoundingMode.HALF_UP),
                baseCost.setScale(4, RoundingMode.HALF_UP), List.of(), "Custo completo.");
    }

    public static ProductCostResult missingRecipe(Product product) {
        return new ProductCostResult(product, false, null, null, List.of(), "Produto sem ficha técnica cadastrada.");
    }

    public static ProductCostResult missingPurchases(Product product, List<Ingredient> missing) {
        String names = missing.stream().map(Ingredient::getName).collect(Collectors.joining(", "));
        return new ProductCostResult(product, false, null, null, List.copyOf(missing),
                "Faltam compras registradas para os ingredientes: " + names + ".");
    }
}
