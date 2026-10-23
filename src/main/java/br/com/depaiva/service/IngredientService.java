package br.com.depaiva.service;

import br.com.depaiva.domain.Ingredient;
import br.com.depaiva.domain.IngredientPurchase;
import br.com.depaiva.domain.Measure;
import br.com.depaiva.domain.Origin;
import br.com.depaiva.repository.IngredientPurchaseRepository;
import br.com.depaiva.repository.IngredientRepository;
import java.math.BigDecimal;
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

    @Transactional
    public Ingredient saveIngredient(Long id, String name, Measure unit) {
        Ingredient ingredient = id == null
                ? new Ingredient(name, unit)
                : ingredients.findById(id).orElseThrow(() -> new IllegalArgumentException("Ingrediente não encontrado."));
        if (id != null) ingredient.update(name, unit);
        return ingredients.save(ingredient);
    }

    @Transactional
    public IngredientPurchase registerPurchase(LocalDate date, Long ingredientId, BigDecimal quantity,
                                                BigDecimal unitPrice) {
        Ingredient ingredient = ingredients.findById(ingredientId)
                .orElseThrow(() -> new IllegalArgumentException("Ingrediente não encontrado."));
        return purchases.save(new IngredientPurchase(date, quantity, unitPrice, Origin.MANUAL, ingredient));
    }

    public List<IngredientPurchase> purchases(LocalDate start, LocalDate end) {
        return purchases.findAllByDateBetweenOrderByDateDesc(start, end);
    }
}
