package br.com.depaiva.service.cost;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import br.com.depaiva.domain.Ingredient;
import br.com.depaiva.domain.IngredientPurchase;
import br.com.depaiva.domain.Measure;
import br.com.depaiva.domain.Origin;
import br.com.depaiva.domain.Product;
import br.com.depaiva.domain.ProductIngredient;
import br.com.depaiva.domain.Type;
import br.com.depaiva.repository.IngredientPurchaseRepository;
import br.com.depaiva.repository.ProductIngredientRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class ProductCostServiceTest {

    private final ProductIngredientRepository productIngredients = mock(ProductIngredientRepository.class);
    private final IngredientPurchaseRepository purchases = mock(IngredientPurchaseRepository.class);
    private final IngredientCostStrategy costStrategy = new WeightedAverageCostStrategy();
    private ProductCostService service;

    private final Product cake = new Product("Bolo Simples", new BigDecimal("10.00"), Type.CHILLED_CAKE, null);
    private final Ingredient flour = new Ingredient("Farinha", Measure.GRAMS);
    private final Ingredient egg = new Ingredient("Ovo", Measure.UNIT);

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(cake, "id", 1L);
        ReflectionTestUtils.setField(flour, "id", 10L);
        ReflectionTestUtils.setField(egg, "id", 20L);
        service = new ProductCostService(productIngredients, purchases, costStrategy, BigDecimal.ZERO);
    }

    @Test
    @DisplayName("Calcula custo unitário baseado na ficha técnica (quantidade / rendimento * custo do ingrediente)")
    void calculatesUnitCostFromRecipe() {
        // Receita: 500g de farinha e 5 ovos produzem 10 bolos (rendimento = 10)
        // Por bolo: 50g de farinha e 0.5 ovo
        ProductIngredient itemFlour = new ProductIngredient(cake, flour, new BigDecimal("500.000"), 10);
        ProductIngredient itemEgg = new ProductIngredient(cake, egg, new BigDecimal("5.000"), 10);
        when(productIngredients.findAllByProductId(1L)).thenReturn(List.of(itemFlour, itemEgg));

        // Compras:
        // Farinha: 1000g por R$ 10.00 -> R$ 0.010/g
        IngredientPurchase pFlour = new IngredientPurchase(
                LocalDate.of(2026, 11, 1), new BigDecimal("1000.000"), new BigDecimal("0.01"),
                new BigDecimal("10.00"), Origin.MANUAL, flour);
        when(purchases.findAllByIngredientIdOrderByDateDesc(10L)).thenReturn(List.of(pFlour));

        // Ovo: 10 un por R$ 8.00 -> R$ 0.80/un
        IngredientPurchase pEgg = new IngredientPurchase(
                LocalDate.of(2026, 11, 1), new BigDecimal("10.000"), new BigDecimal("0.80"),
                new BigDecimal("8.00"), Origin.MANUAL, egg);
        when(purchases.findAllByIngredientIdOrderByDateDesc(20L)).thenReturn(List.of(pEgg));

        // Custo farinha por bolo = 50g * 0.01 = R$ 0.50
        // Custo ovo por bolo = 0.5 un * 0.80 = R$ 0.40
        // Custo unitário base = R$ 0.90
        ProductCostResult result = service.calculateUnitCost(cake);

        assertTrue(result.complete());
        assertEquals(new BigDecimal("0.90"), result.unitCost());
    }

    @Test
    @DisplayName("Aplica margem de perda configurável multiplicando o custo")
    void appliesConfigurableLossMargin() {
        ProductIngredient itemFlour = new ProductIngredient(cake, flour, new BigDecimal("500.000"), 10);
        when(productIngredients.findAllByProductId(1L)).thenReturn(List.of(itemFlour));

        IngredientPurchase pFlour = new IngredientPurchase(
                LocalDate.of(2026, 11, 1), new BigDecimal("1000.000"), new BigDecimal("0.01"),
                new BigDecimal("10.00"), Origin.MANUAL, flour);
        when(purchases.findAllByIngredientIdOrderByDateDesc(10L)).thenReturn(List.of(pFlour));

        // Custo base = 50g * 0.01 = R$ 0.50
        // Com margem de perda de 10% (0.10) -> 0.50 * 1.10 = R$ 0.55
        ProductCostResult result = service.calculateUnitCost(cake, new BigDecimal("0.10"));

        assertTrue(result.complete());
        assertEquals(new BigDecimal("0.55"), result.unitCost());
    }

    @Test
    @DisplayName("Retorna custo incompleto quando o produto não tem ficha técnica")
    void returnsIncompleteWhenNoRecipe() {
        when(productIngredients.findAllByProductId(1L)).thenReturn(List.of());

        ProductCostResult result = service.calculateUnitCost(cake);

        assertFalse(result.complete());
        assertTrue(result.message().contains("sem ficha técnica"));
    }

    @Test
    @DisplayName("Retorna custo incompleto e sinaliza ingredientes faltantes quando ingrediente não tem compras")
    void returnsIncompleteAndSignalsMissingIngredientsWhenNoPurchases() {
        ProductIngredient itemFlour = new ProductIngredient(cake, flour, new BigDecimal("500.000"), 10);
        ProductIngredient itemEgg = new ProductIngredient(cake, egg, new BigDecimal("5.000"), 10);
        when(productIngredients.findAllByProductId(1L)).thenReturn(List.of(itemFlour, itemEgg));

        // Farinha tem compras, mas Ovo NÃO tem compras registradas
        IngredientPurchase pFlour = new IngredientPurchase(
                LocalDate.of(2026, 11, 1), new BigDecimal("1000.000"), new BigDecimal("0.01"),
                new BigDecimal("10.00"), Origin.MANUAL, flour);
        when(purchases.findAllByIngredientIdOrderByDateDesc(10L)).thenReturn(List.of(pFlour));
        when(purchases.findAllByIngredientIdOrderByDateDesc(20L)).thenReturn(List.of());

        ProductCostResult result = service.calculateUnitCost(cake);

        assertFalse(result.complete());
        assertEquals(1, result.missingIngredients().size());
        assertEquals("Ovo", result.missingIngredients().getFirst().getName());
        assertTrue(result.message().contains("Ovo"));
    }
}
