package br.com.depaiva.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.depaiva.domain.Ingredient;
import br.com.depaiva.domain.IngredientPurchase;
import br.com.depaiva.domain.Measure;
import br.com.depaiva.domain.Origin;
import br.com.depaiva.repository.IngredientPurchaseRepository;
import br.com.depaiva.repository.IngredientRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class IngredientServiceTest {
    private final IngredientRepository ingredients = org.mockito.Mockito.mock(IngredientRepository.class);
    private final IngredientPurchaseRepository purchases = org.mockito.Mockito.mock(IngredientPurchaseRepository.class);
    private IngredientService service;

    @BeforeEach
    void setUp() {
        service = new IngredientService(ingredients, purchases);
    }

    @Test
    void createSavesNewIngredient() {
        Ingredient saved = new Ingredient("Açúcar", Measure.KILO_GRAMS);
        ReflectionTestUtils.setField(saved, "id", 10L);
        when(ingredients.save(any(Ingredient.class))).thenReturn(saved);

        Ingredient result = service.create("Açúcar", Measure.KILO_GRAMS);

        assertNotNull(result);
        assertEquals("Açúcar", result.getName());
        assertEquals(Measure.KILO_GRAMS, result.getUnit());
        verify(ingredients).save(any(Ingredient.class));
    }

    @Test
    void findByIdReturnsIngredientWhenExists() {
        Ingredient existing = new Ingredient("Cacau", Measure.GRAMS);
        ReflectionTestUtils.setField(existing, "id", 5L);
        when(ingredients.findById(5L)).thenReturn(Optional.of(existing));

        Ingredient result = service.findById(5L);

        assertEquals("Cacau", result.getName());
        assertEquals(Measure.GRAMS, result.getUnit());
    }

    @Test
    void findByIdThrowsWhenNotFound() {
        when(ingredients.findById(99L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> service.findById(99L));
    }

    @Test
    void updateModifiesIngredientAndSaves() {
        Ingredient existing = new Ingredient("Farinha", Measure.KILO_GRAMS);
        ReflectionTestUtils.setField(existing, "id", 2L);
        when(ingredients.findById(2L)).thenReturn(Optional.of(existing));
        when(ingredients.save(existing)).thenReturn(existing);

        Ingredient updated = service.update(2L, "Farinha Integral", Measure.GRAMS);

        assertEquals("Farinha Integral", updated.getName());
        assertEquals(Measure.GRAMS, updated.getUnit());
        verify(ingredients).save(existing);
    }

    @Test
    void deleteRemovesIngredientWhenNoPurchasesExist() {
        Ingredient existing = new Ingredient("Baunilha", Measure.MILLILITERS);
        ReflectionTestUtils.setField(existing, "id", 3L);
        when(ingredients.findById(3L)).thenReturn(Optional.of(existing));
        when(purchases.existsByIngredientId(3L)).thenReturn(false);

        service.delete(3L);

        verify(ingredients).delete(existing);
    }

    @Test
    void deleteRejectsWhenPurchasesExist() {
        Ingredient existing = new Ingredient("Frango", Measure.KILO_GRAMS);
        ReflectionTestUtils.setField(existing, "id", 4L);
        when(ingredients.findById(4L)).thenReturn(Optional.of(existing));
        when(purchases.existsByIngredientId(4L)).thenReturn(true);

        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> service.delete(4L));
        assertEquals("Não é possível excluir o ingrediente 'Frango' pois existem compras vinculadas a ele.", exception.getMessage());
        verify(ingredients, never()).delete(any());
    }

    @Test
    void getSummaryCalculatesTotalsCorrectly() {
        Ingredient existing = new Ingredient("Ovo", Measure.GRAMS);
        ReflectionTestUtils.setField(existing, "id", 7L);
        when(ingredients.findById(7L)).thenReturn(Optional.of(existing));

        IngredientPurchase p1 = new IngredientPurchase(LocalDate.of(2026, 10, 1), new BigDecimal("1000.000"),
                new BigDecimal("0.02"), Origin.MANUAL, existing);
        IngredientPurchase p2 = new IngredientPurchase(LocalDate.of(2026, 10, 15), new BigDecimal("500.000"),
                new BigDecimal("0.02"), Origin.MANUAL, existing);

        when(purchases.findAllByIngredientIdOrderByDateDesc(7L)).thenReturn(List.of(p1, p2));

        IngredientService.IngredientSummary summary = service.getSummary(7L);

        assertEquals(2, summary.purchaseCount());
        assertEquals(new BigDecimal("1500.000"), summary.totalQuantity());
        assertEquals(new BigDecimal("30.00"), summary.totalSpent());
        assertEquals(2, summary.purchases().size());
    }
}
