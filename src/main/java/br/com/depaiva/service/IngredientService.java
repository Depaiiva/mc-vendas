package br.com.depaiva.service;

import br.com.depaiva.domain.Ingredient;
import br.com.depaiva.domain.IngredientPurchase;
import br.com.depaiva.domain.Measure;
import br.com.depaiva.domain.Origin;
import br.com.depaiva.repository.IngredientPurchaseRepository;
import br.com.depaiva.repository.IngredientRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class IngredientService {
    private final IngredientRepository ingredients;
    private final IngredientPurchaseRepository purchases;

    public IngredientService(IngredientRepository ingredients, IngredientPurchaseRepository purchases) {
        this.ingredients = ingredients;
        this.purchases = purchases;
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

    @Transactional
    public void delete(Long id) {
        Ingredient ingredient = findById(id);
        if (purchases.existsByIngredientId(id)) {
            throw new IllegalStateException("Não é possível excluir o ingrediente '" + ingredient.getName()
                    + "' pois existem compras vinculadas a ele.");
        }
        ingredients.delete(ingredient);
    }

    @Transactional
    public Ingredient saveIngredient(Long id, String name, Measure unit) {
        return id == null ? create(name, unit) : update(id, name, unit);
    }

    public IngredientSummary getSummary(Long id) {
        Ingredient ingredient = findById(id);
        List<IngredientPurchase> ingredientPurchases = purchases.findAllByIngredientIdOrderByDateDesc(id);
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
        Ingredient ingredient = findById(ingredientId);
        return purchases.save(new IngredientPurchase(date, quantity, unitPrice, Origin.MANUAL, ingredient));
    }

    public List<IngredientPurchase> purchases(LocalDate start, LocalDate end) {
        return purchases.findAllByDateBetweenOrderByDateDesc(start, end);
    }

    public record IngredientSummary(
            Ingredient ingredient,
            long purchaseCount,
            BigDecimal totalQuantity,
            BigDecimal totalSpent,
            List<IngredientPurchase> purchases
    ) {}
}
