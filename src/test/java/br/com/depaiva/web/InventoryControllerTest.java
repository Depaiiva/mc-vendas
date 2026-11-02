package br.com.depaiva.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.depaiva.domain.Ingredient;
import br.com.depaiva.domain.Measure;
import br.com.depaiva.domain.Product;
import br.com.depaiva.domain.Type;
import br.com.depaiva.service.IngredientService;
import br.com.depaiva.service.ProductService;
import br.com.depaiva.service.StockService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

class InventoryControllerTest {
    private final ProductService products = mock(ProductService.class);
    private final StockService stock = mock(StockService.class);
    private final IngredientService ingredients = mock(IngredientService.class);
    private InventoryController controller;

    @BeforeEach
    void setUp() {
        controller = new InventoryController(products, stock, ingredients);
    }

    @Test
    void stockPageBuildsStockLinesWithCarryoverAndExplicitFlags() {
        Product bolo = new Product("Bolo", new BigDecimal("10.00"), Type.CHILLED_CAKE, null);
        ReflectionTestUtils.setField(bolo, "id", 1L);
        when(products.active()).thenReturn(List.of(bolo));
        when(stock.getInitialQuantity(eq(1L), any())).thenReturn(15);
        when(stock.getSoldQuantity(eq(1L), any())).thenReturn(3);
        when(stock.hasExplicitStock(eq(1L), any())).thenReturn(false);
        when(stock.getCarriedOverStock(eq(1L), any())).thenReturn(15);

        ModelAndView mav = controller.stockPage("2026-11-03");

        assertEquals("stock", mav.getViewName());
        assertNotNull(mav.getModel().get("selectedDate"));
        @SuppressWarnings("unchecked")
        List<InventoryController.StockLine> lines = (List<InventoryController.StockLine>) mav.getModel().get("stockLines");
        assertEquals(1, lines.size());
        assertEquals(15, lines.get(0).initial());
        assertEquals(3, lines.get(0).sold());
        assertEquals(12, lines.get(0).available());
        assertFalse(lines.get(0).explicit());
        assertEquals(15, lines.get(0).carriedOver());
    }

    @Test
    void saveStockSavesQuantitiesForActiveProducts() {
        Product bolo = new Product("Bolo", new BigDecimal("10.00"), Type.CHILLED_CAKE, null);
        ReflectionTestUtils.setField(bolo, "id", 1L);
        when(products.active()).thenReturn(List.of(bolo));

        RedirectAttributesModelMap flash = new RedirectAttributesModelMap();
        ModelAndView mav = controller.saveStock("2026-11-04", Map.of("initial_1", "10"), flash);

        verify(stock).editInitialStock(1L, LocalDate.of(2026, 11, 4), 10);
        assertEquals("redirect:/stock?date=2026-11-04", mav.getViewName());
        assertEquals("Estoque salvo com sucesso.", flash.getFlashAttributes().get("success"));
    }

    @Test
    void resetStockCallsServiceAndRedirects() {
        RedirectAttributesModelMap flash = new RedirectAttributesModelMap();
        ModelAndView mav = controller.resetStock("2026-11-04", 1L, flash);

        verify(stock).resetStock(1L, LocalDate.of(2026, 11, 4));
        assertEquals("redirect:/stock?date=2026-11-04", mav.getViewName());
        assertEquals("Estoque restaurado para o cálculo automático.", flash.getFlashAttributes().get("success"));
    }

    @Test
    void ingredientsPageLoadsIngredientsAndPurchases() {
        Ingredient ing1 = new Ingredient("Farinha", Measure.KILO_GRAMS);
        when(ingredients.all()).thenReturn(List.of(ing1));
        when(ingredients.purchases(any(LocalDate.class), any(LocalDate.class))).thenReturn(List.of());

        ModelAndView mav = controller.ingredients(null, null, null);

        assertEquals("ingredients", mav.getViewName());
        assertNotNull(mav.getModel().get("ingredients"));
        assertNotNull(mav.getModel().get("measures"));
        assertNotNull(mav.getModel().get("purchases"));
    }

    @Test
    void viewIngredientLoadsSelectedSummary() {
        Ingredient ing = new Ingredient("Manteiga", Measure.GRAMS);
        ReflectionTestUtils.setField(ing, "id", 10L);
        IngredientService.IngredientSummary summary = new IngredientService.IngredientSummary(
                ing, 1, new BigDecimal("500.000"), new BigDecimal("15.00"), List.of()
        );
        when(ingredients.all()).thenReturn(List.of(ing));
        when(ingredients.purchases(any(LocalDate.class), any(LocalDate.class))).thenReturn(List.of());
        when(ingredients.getSummary(10L)).thenReturn(summary);

        ModelAndView mav = controller.viewIngredient(10L, null, null);

        assertEquals("ingredients", mav.getViewName());
        assertEquals(ing, mav.getModel().get("selectedIngredient"));
        assertEquals(summary, mav.getModel().get("selectedSummary"));
    }

    @Test
    void saveIngredientCreatesNewIngredient() {
        RedirectAttributesModelMap flash = new RedirectAttributesModelMap();

        ModelAndView mav = controller.saveIngredient(null, "Açúcar", Measure.KILO_GRAMS, flash);

        verify(ingredients).saveIngredient(null, "Açúcar", Measure.KILO_GRAMS);
        assertEquals("redirect:/ingredients", mav.getViewName());
        assertEquals("Ingrediente criado com sucesso.", flash.getFlashAttributes().get("success"));
    }

    @Test
    void editIngredientUpdatesExistingIngredient() {
        RedirectAttributesModelMap flash = new RedirectAttributesModelMap();

        ModelAndView mav = controller.editIngredient(5L, "Açúcar Mascavo", Measure.KILO_GRAMS, flash);

        verify(ingredients).update(5L, "Açúcar Mascavo", Measure.KILO_GRAMS);
        assertEquals("redirect:/ingredients", mav.getViewName());
        assertEquals("Ingrediente atualizado com sucesso.", flash.getFlashAttributes().get("success"));
    }

    @Test
    void deleteIngredientRemovesIngredientSuccessfully() {
        RedirectAttributesModelMap flash = new RedirectAttributesModelMap();

        ModelAndView mav = controller.deleteIngredient(3L, null, flash);

        verify(ingredients).delete(3L);
        assertEquals("redirect:/ingredients", mav.getViewName());
        assertEquals("Ingrediente excluído com sucesso.", flash.getFlashAttributes().get("success"));
    }

    @Test
    void deleteIngredientHandlesFailureGracefully() {
        RedirectAttributesModelMap flash = new RedirectAttributesModelMap();
        doThrow(new IllegalStateException("Possui compras vinculadas.")).when(ingredients).delete(3L);

        ModelAndView mav = controller.deleteIngredient(3L, null, flash);

        assertEquals("redirect:/ingredients", mav.getViewName());
        assertEquals("Possui compras vinculadas.", flash.getFlashAttributes().get("error"));
    }

    @Test
    void saveProductCreatesNewProduct() {
        RedirectAttributesModelMap flash = new RedirectAttributesModelMap();

        ModelAndView mav = controller.saveProduct(null, "Bolo de Cenoura", new BigDecimal("9.50"),
                Type.CHILLED_CAKE, "Cobertura de chocolate", true, flash);

        verify(products).save(null, "Bolo de Cenoura", new BigDecimal("9.50"), Type.CHILLED_CAKE, "Cobertura de chocolate", true);
        assertEquals("redirect:/products", mav.getViewName());
        assertEquals("Produto criado com sucesso.", flash.getFlashAttributes().get("success"));
    }

    @Test
    void duplicateProductCallsServiceAndRedirects() {
        RedirectAttributesModelMap flash = new RedirectAttributesModelMap();
        Product copy = new Product("Bolo (cópia)", new BigDecimal("10.00"), Type.CHILLED_CAKE, null);
        when(products.duplicate(2L)).thenReturn(copy);

        ModelAndView mav = controller.duplicateProduct(2L, flash);

        verify(products).duplicate(2L);
        assertEquals("redirect:/products", mav.getViewName());
        assertEquals("Produto duplicado com sucesso: Bolo (cópia)", flash.getFlashAttributes().get("success"));
    }

    @Test
    void deleteProductRemovesSuccessfully() {
        RedirectAttributesModelMap flash = new RedirectAttributesModelMap();

        ModelAndView mav = controller.deleteProduct(5L, null, flash);

        verify(products).delete(5L);
        assertEquals("redirect:/products", mav.getViewName());
        assertEquals("Produto excluído com sucesso.", flash.getFlashAttributes().get("success"));
    }

    @Test
    void deleteProductHandlesFailureGracefully() {
        RedirectAttributesModelMap flash = new RedirectAttributesModelMap();
        doThrow(new IllegalStateException("Possui vendas vinculadas.")).when(products).delete(5L);

        ModelAndView mav = controller.deleteProduct(5L, null, flash);

        assertEquals("redirect:/products", mav.getViewName());
        assertEquals("Possui vendas vinculadas.", flash.getFlashAttributes().get("error"));
    }
}
