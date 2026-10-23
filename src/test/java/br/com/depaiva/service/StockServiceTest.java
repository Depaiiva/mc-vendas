package br.com.depaiva.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.depaiva.domain.DailyStock;
import br.com.depaiva.domain.Product;
import br.com.depaiva.domain.Status;
import br.com.depaiva.domain.Type;
import br.com.depaiva.repository.DailyStockRepository;
import br.com.depaiva.repository.OrderRepository;
import br.com.depaiva.repository.ProductRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class StockServiceTest {
    private final DailyStockRepository stocks = org.mockito.Mockito.mock(DailyStockRepository.class);
    private final OrderRepository orders = org.mockito.Mockito.mock(OrderRepository.class);
    private final ProductRepository products = org.mockito.Mockito.mock(ProductRepository.class);
    private final Product product = new Product("Bolo", new BigDecimal("8.00"), Type.CHILLED_CAKE, null);
    private final LocalDate date = LocalDate.of(2026, 10, 13);
    private StockService service;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(product, "id", 12L);
        service = new StockService(stocks, orders, products);
        when(orders.findAllByDateBetweenAndStatusIn(any(), any(), any())).thenReturn(List.of());
    }

    @Test
    void missingStockRecordMeansZeroAvailable() {
        when(stocks.findByProductIdAndDate(12L, date)).thenReturn(Optional.empty());
        assertEquals(0, service.getAvailable(product, date));
    }

    @Test
    void canceledOrdersAreExcludedFromStockSales() {
        when(stocks.findByProductIdAndDate(12L, date)).thenReturn(Optional.of(new DailyStock(product, date, 5)));

        assertEquals(5, service.getAvailable(product, date));
        verify(orders).findAllByDateBetweenAndStatusIn(
                eq(date.atStartOfDay()), any(), eq(List.of(Status.CONFIRMED, Status.DELIVERED)));
    }

    @Test
    void duplicateInitialStockRegistrationIsRejected() {
        when(products.findById(12L)).thenReturn(Optional.of(product));
        when(stocks.findByProductIdAndDate(12L, date)).thenReturn(Optional.of(new DailyStock(product, date, 4)));

        assertThrows(IllegalArgumentException.class, () -> service.registerInitialStock(12L, date, 7));
        verify(stocks, never()).save(any());
    }

    @Test
    void existingInitialStockCanBeCorrected() {
        DailyStock existing = new DailyStock(product, date, 4);
        when(stocks.findByProductIdAndDate(12L, date)).thenReturn(Optional.of(existing));
        when(stocks.save(existing)).thenReturn(existing);

        DailyStock updated = service.editInitialStock(12L, date, 9);

        assertEquals(9, updated.getInitialQuantity());
        verify(stocks).save(existing);
    }
}
