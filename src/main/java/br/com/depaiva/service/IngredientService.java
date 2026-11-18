package br.com.depaiva.service;

import br.com.depaiva.domain.Ingredient;
import br.com.depaiva.domain.IngredientPurchase;
import br.com.depaiva.domain.Measure;
import br.com.depaiva.domain.Origin;
import br.com.depaiva.repository.IngredientPurchaseRepository;
import br.com.depaiva.repository.IngredientRepository;
import br.com.depaiva.repository.ProductIngredientRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class IngredientService {
    private final IngredientRepository ingredients;
    private final IngredientPurchaseRepository purchases;
    private final ProductIngredientRepository productIngredients;

    public IngredientService(IngredientRepository ingredients, IngredientPurchaseRepository purchases) {
        this(ingredients, purchases, null);
    }

    @Autowired
    public IngredientService(IngredientRepository ingredients, IngredientPurchaseRepository purchases,
                             ProductIngredientRepository productIngredients) {
        this.ingredients = ingredients;
        this.purchases = purchases;
        this.productIngredients = productIngredients;
    }

    public List<Ingredient> all() {
        return ingredients.findAllByOrderByNameAsc();
    }

    public Ingredient findById(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("Identificador do ingrediente é obrigatório.");
        }
        return ingredients.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Ingrediente não encontrado."));
    }

    @Transactional
    public Ingredient create(String name, Measure unit) {
        return ingredients.save(new Ingredient(name, unit));
    }

    @Transactional
    public Ingredient update(Long id, String name, Measure unit) {
        Ingredient ingredient = findById(id);
        ingredient.update(name, unit);
        return ingredients.save(ingredient);
    }

    public IngredientDependencySummary getDependencies(Long id) {
        Ingredient ingredient = findById(id);
        long purchaseCount = purchases != null ? purchases.countByIngredientId(id) : 0;
        List<String> productNames = productIngredients != null
                ? productIngredients.findAllByIngredientId(id).stream()
                        .map(pi -> pi.getProduct().getName())
                        .distinct()
                        .toList()
                : List.of();
        return new IngredientDependencySummary(ingredient, purchaseCount, productNames);
    }

    @Transactional
    public void delete(Long id) {
        delete(id, false);
    }

    @Transactional
    public void delete(Long id, boolean force) {
        Ingredient ingredient = findById(id);
        IngredientDependencySummary deps = getDependencies(id);

        boolean hasPurchases = (purchases != null && purchases.existsByIngredientId(id)) || deps.purchaseCount() > 0;
        boolean hasProducts = !deps.productNames().isEmpty();

        if (!force && (hasPurchases || hasProducts)) {
            List<String> parts = new ArrayList<>();
            if (hasPurchases) {
                parts.add("existem compras vinculadas a ele");
            }
            if (hasProducts) {
                parts.add("está vinculado à ficha técnica dos produtos: " + String.join(", ", deps.productNames()));
            }
            throw new IllegalStateException("Não é possível excluir o ingrediente '" + ingredient.getName()
                    + "' pois " + String.join(" e ", parts) + ".");
        }

        if (productIngredients != null) {
            productIngredients.deleteAllByIngredientId(id);
        }
        if (purchases != null) {
            purchases.deleteAllByIngredientId(id);
        }
        ingredients.delete(ingredient);
    }

    @Transactional
    public Ingredient saveIngredient(Long id, String name, Measure unit) {
        return id == null ? create(name, unit) : update(id, name, unit);
    }

    public IngredientSummary getSummary(Long id) {
        Ingredient ingredient = findById(id);
        List<IngredientPurchase> ingredientPurchases = purchases != null
                ? purchases.findAllByIngredientIdOrderByDateDesc(id)
                : List.of();
        BigDecimal totalQuantity = ingredientPurchases.stream()
                .map(IngredientPurchase::getQuantity)
                .reduce(BigDecimal.ZERO.setScale(3, RoundingMode.HALF_UP), BigDecimal::add);
        BigDecimal totalSpent = ingredientPurchases.stream()
                .map(IngredientPurchase::calculateTotalCost)
                .reduce(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP), BigDecimal::add);
        return new IngredientSummary(ingredient, ingredientPurchases.size(), totalQuantity, totalSpent, ingredientPurchases);
    }

    @Transactional
    public IngredientPurchase registerPurchase(LocalDate date, Long ingredientId, BigDecimal quantity,
                                                BigDecimal unitPrice) {
        return registerPurchase(date, ingredientId, quantity, null, unitPrice, null);
    }

    @Transactional
    public IngredientPurchase registerPurchase(LocalDate date, Long ingredientId, BigDecimal quantity,
                                                Measure purchaseUnit, BigDecimal unitPrice, BigDecimal totalCost) {
        Ingredient ingredient = findById(ingredientId);
        Measure effectiveUnit = purchaseUnit != null ? purchaseUnit : ingredient.getUnit();
        if (effectiveUnit.getBaseUnit() != ingredient.getUnit()) {
            throw new IllegalArgumentException("A unidade " + effectiveUnit + " é incompatível com a unidade base do ingrediente (" + ingredient.getUnit() + ").");
        }
        BigDecimal baseQuantity = effectiveUnit.convertToBase(quantity);
        return purchases.save(new IngredientPurchase(date, baseQuantity, unitPrice, totalCost, Origin.MANUAL, ingredient));
    }

    public List<IngredientPurchase> purchases(LocalDate start, LocalDate end) {
        return purchases != null ? purchases.findAllByDateBetweenOrderByDateDesc(start, end) : List.of();
    }

    public record IngredientSummary(
            Ingredient ingredient,
            long purchaseCount,
            BigDecimal totalQuantity,
            BigDecimal totalSpent,
            List<IngredientPurchase> purchases
    ) {}

    public record IngredientDependencySummary(
            Ingredient ingredient,
            long purchaseCount,
            List<String> productNames
    ) {
        public boolean hasDependencies() {
            return purchaseCount > 0 || !productNames.isEmpty();
        }
    }
}
