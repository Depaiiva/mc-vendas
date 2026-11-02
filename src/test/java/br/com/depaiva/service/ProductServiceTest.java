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

import br.com.depaiva.domain.Product;
import br.com.depaiva.domain.Type;
import br.com.depaiva.repository.DailyStockRepository;
import br.com.depaiva.repository.OrderItemRepository;
import br.com.depaiva.repository.ProductRepository;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class ProductServiceTest {
    private final ProductRepository products = mock(ProductRepository.class);
    private final DailyStockRepository dailyStocks = mock(DailyStockRepository.class);
    private final OrderItemRepository orderItems = mock(OrderItemRepository.class);
    private ProductService service;

    @BeforeEach
    void setUp() {
        service = new ProductService(products, dailyStocks, orderItems);
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
}
