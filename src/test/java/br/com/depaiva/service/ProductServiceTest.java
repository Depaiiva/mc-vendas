package br.com.depaiva.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.depaiva.domain.Ingredient;
import br.com.depaiva.domain.Measure;
import br.com.depaiva.domain.Product;
import br.com.depaiva.domain.ProductIngredient;
import br.com.depaiva.domain.Type;
import br.com.depaiva.repository.DailyStockRepository;
import br.com.depaiva.repository.IngredientRepository;
import br.com.depaiva.repository.OrderItemRepository;
import br.com.depaiva.repository.OrderRepository;
import br.com.depaiva.repository.ProductIngredientRepository;
import br.com.depaiva.repository.ProductRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class ProductServiceTest {
    private final ProductRepository products = mock(ProductRepository.class);
    private final DailyStockRepository dailyStocks = mock(DailyStockRepository.class);
    private final OrderItemRepository orderItems = mock(OrderItemRepository.class);
    private final ProductIngredientRepository productIngredients = mock(ProductIngredientRepository.class);
    private final IngredientRepository ingredients = mock(IngredientRepository.class);
    private final OrderRepository orderRepository = mock(OrderRepository.class);
    private ProductService service;

    @BeforeEach
    void setUp() {
        service = new ProductService(products, dailyStocks, orderItems, productIngredients, ingredients, orderRepository);
    }

    @Test
    void createProductSavesNewProduct() {
        Product saved = new Product("Torta", new BigDecimal("15.00"), Type.CHILLED_CAKE, "Fatia");
        ReflectionTestUtils.setField(saved, "id", 1L);
        when(products.save(any(Product.class))).thenReturn(saved);

        Product result = service.create("Torta", new BigDecimal("15.00"), Type.CHILLED_CAKE, "Fatia", true);

        assertNotNull(result);
        assertEquals("Torta", result.getName());
        verify(products).save(any(Product.class));
    }

    @Test
    void findByIdReturnsProductWhenExists() {
        Product product = new Product("Bolo", new BigDecimal("10.00"), Type.CHILLED_CAKE, null);
        ReflectionTestUtils.setField(product, "id", 2L);
        when(products.findById(2L)).thenReturn(Optional.of(product));

        Product found = service.findById(2L);

        assertEquals("Bolo", found.getName());
    }

    @Test
    void findByIdThrowsWhenNotFound() {
        when(products.findById(99L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> service.findById(99L));
    }

    @Test
    void updateModifiesExistingProduct() {
        Product existing = new Product("Bolo Simples", new BigDecimal("8.00"), Type.CHILLED_CAKE, "Antigo");
        ReflectionTestUtils.setField(existing, "id", 3L);
        when(products.findById(3L)).thenReturn(Optional.of(existing));
        when(products.save(existing)).thenReturn(existing);

        Product updated = service.update(3L, "Bolo Especial", new BigDecimal("12.00"), Type.CAKE_IN_A_JAR, "Novo", false);

        assertEquals("Bolo Especial", updated.getName());
        assertEquals(new BigDecimal("12.00"), updated.getPrice());
        assertEquals(Type.CAKE_IN_A_JAR, updated.getType());
        assertEquals("Novo", updated.getDescription());
        assertFalse(updated.isActive());
        verify(products).save(existing);
    }

    @Test
    void duplicateCreatesCopyOfProduct() {
        Product original = new Product("Sanduíche Natural", new BigDecimal("12.00"), Type.SANDWICH, "Frango");
        ReflectionTestUtils.setField(original, "id", 4L);
        when(products.findById(4L)).thenReturn(Optional.of(original));

        Product copy = new Product("Sanduíche Natural (cópia)", new BigDecimal("12.00"), Type.SANDWICH, "Frango");
        ReflectionTestUtils.setField(copy, "id", 5L);
        when(products.save(any(Product.class))).thenReturn(copy);

        Product result = service.duplicate(4L);

        assertNotNull(result);
        assertEquals("Sanduíche Natural (cópia)", result.getName());
        verify(products).save(any(Product.class));
    }

    @Test
    void deleteRemovesProductWhenNoOrdersAndNoStocks() {
        Product product = new Product("Suco", new BigDecimal("6.00"), Type.CHILLED_CAKE, null);
        ReflectionTestUtils.setField(product, "id", 6L);
        when(products.findById(6L)).thenReturn(Optional.of(product));
        when(orderItems.existsByProductId(6L)).thenReturn(false);
        when(dailyStocks.existsByProductId(6L)).thenReturn(false);

        service.delete(6L);

        verify(products).delete(product);
    }

    @Test
    void deleteRejectsWhenProductHasOrders() {
        Product product = new Product("Bolo", new BigDecimal("8.00"), Type.CHILLED_CAKE, null);
        ReflectionTestUtils.setField(product, "id", 7L);
        when(products.findById(7L)).thenReturn(Optional.of(product));
        when(orderItems.existsByProductId(7L)).thenReturn(true);

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> service.delete(7L));
        assertTrue(ex.getMessage().contains("vendas registradas"));
        verify(products, never()).delete(any());
    }

    @Test
    void deleteRejectsWhenProductHasDailyStocks() {
        Product product = new Product("Sanduíche", new BigDecimal("10.00"), Type.SANDWICH, null);
        ReflectionTestUtils.setField(product, "id", 8L);
        when(products.findById(8L)).thenReturn(Optional.of(product));
        when(orderItems.existsByProductId(8L)).thenReturn(false);
        when(dailyStocks.existsByProductId(8L)).thenReturn(true);

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> service.delete(8L));
        assertTrue(ex.getMessage().contains("estoque vinculado"));
        verify(products, never()).delete(any());
    }

    @Test
    void addsAndRemovesRecipeIngredient() {
        Product product = new Product("Bolo", new BigDecimal("10.00"), Type.CHILLED_CAKE, null);
        ReflectionTestUtils.setField(product, "id", 10L);
        Ingredient ingredient = new Ingredient("Farinha", Measure.GRAMS);
        ReflectionTestUtils.setField(ingredient, "id", 20L);

        when(products.findById(10L)).thenReturn(Optional.of(product));
        when(ingredients.findById(20L)).thenReturn(Optional.of(ingredient));
        when(productIngredients.findAllByProductId(10L)).thenReturn(List.of());
        when(productIngredients.save(any(ProductIngredient.class))).thenAnswer(i -> i.getArgument(0));

        ProductIngredient pi = service.addOrUpdateRecipeIngredient(10L, 20L, new BigDecimal("500.000"), 10);
        assertNotNull(pi);
        assertEquals(new BigDecimal("500.000"), pi.getQuantity());
        assertEquals(10, pi.getRecipeYield());

        service.removeRecipeIngredient(100L);
        verify(productIngredients).deleteById(100L);
    }

    @Test
    void deleteWithForceRemovesOrdersStocksAndRecipe() {
        Product product = new Product("Bolo", new BigDecimal("10.00"), Type.CHILLED_CAKE, null);
        ReflectionTestUtils.setField(product, "id", 15L);

        when(products.findById(15L)).thenReturn(Optional.of(product));
        when(orderItems.findAllByProductId(15L)).thenReturn(List.of());

        service.delete(15L, true);

        verify(dailyStocks).deleteAllByProductId(15L);
        verify(productIngredients).deleteAllByProductId(15L);
        verify(products).delete(product);
    }

    @Test
    void getDependenciesReturnsCorrectSummary() {
        Product product = new Product("Bolo", new BigDecimal("10.00"), Type.CHILLED_CAKE, null);
        ReflectionTestUtils.setField(product, "id", 16L);
        Ingredient ingredient = new Ingredient("Cacau", Measure.GRAMS);
        ProductIngredient pi = new ProductIngredient(product, ingredient, new BigDecimal("50.000"), 1);

        when(products.findById(16L)).thenReturn(Optional.of(product));
        when(orderItems.countByProductId(16L)).thenReturn(4L);
        when(dailyStocks.countByProductId(16L)).thenReturn(2L);
        when(productIngredients.findAllByProductId(16L)).thenReturn(List.of(pi));

        ProductService.ProductDependencySummary summary = service.getDependencies(16L);

        assertEquals(4L, summary.orderCount());
        assertEquals(2L, summary.stockCount());
        assertEquals(List.of("Cacau"), summary.recipeIngredientNames());
        org.junit.jupiter.api.Assertions.assertTrue(summary.hasDependencies());
    }
}
