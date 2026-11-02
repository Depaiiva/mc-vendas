package br.com.depaiva.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.depaiva.domain.DailyStock;
import br.com.depaiva.domain.Order;
import br.com.depaiva.domain.Product;
import br.com.depaiva.domain.Status;
import br.com.depaiva.domain.Type;
import br.com.depaiva.repository.DailyStockRepository;
import br.com.depaiva.repository.OrderRepository;
import br.com.depaiva.repository.ProductRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
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
        when(stocks.findFirstByProductIdAndDateLessThanOrderByDateDesc(12L, date)).thenReturn(Optional.empty());
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

    @Test
    void stockCarriesOverFromMondayToTuesdayMinusMondaySales() {
        LocalDate monday = LocalDate.of(2026, 11, 2);
        LocalDate tuesday = LocalDate.of(2026, 11, 3);

        DailyStock mondayStock = new DailyStock(product, monday, 20);
        when(stocks.findByProductIdAndDate(12L, tuesday)).thenReturn(Optional.empty());
        when(stocks.findFirstByProductIdAndDateLessThanOrderByDateDesc(12L, tuesday))
                .thenReturn(Optional.of(mondayStock));

        Order saleOnMonday = Order.inPersonSale(monday.atTime(10, 0), "Cliente", "123");
        saleOnMonday.addItem(product, 5);

        when(orders.findAllByDateBetweenAndStatusIn(
                eq(monday.atStartOfDay()), eq(monday.atTime(LocalTime.MAX)), any()))
                .thenReturn(List.of(saleOnMonday));

        assertEquals(15, service.getInitialQuantity(12L, tuesday));
        assertEquals(15, service.getAvailable(product, tuesday));
        assertFalse(service.hasExplicitStock(12L, tuesday));
    }

    @Test
    void stockCarriesOverToFridayMinusWeeklySalesAndOverToNextMonday() {
        LocalDate monday = LocalDate.of(2026, 11, 2);
        LocalDate friday = LocalDate.of(2026, 11, 6);
        LocalDate nextMonday = LocalDate.of(2026, 11, 9);

        DailyStock mondayStock = new DailyStock(product, monday, 20);
        when(stocks.findByProductIdAndDate(12L, friday)).thenReturn(Optional.empty());
        when(stocks.findFirstByProductIdAndDateLessThanOrderByDateDesc(12L, friday))
                .thenReturn(Optional.of(mondayStock));

        // Monday to Thursday total sold = 14
        Order monToThuOrders = Order.inPersonSale(monday.atTime(12, 0), "Cliente", "123");
        monToThuOrders.addItem(product, 14);

        when(orders.findAllByDateBetweenAndStatusIn(
                eq(monday.atStartOfDay()), eq(LocalDate.of(2026, 11, 5).atTime(LocalTime.MAX)), any()))
                .thenReturn(List.of(monToThuOrders));

        // Friday sold = 4
        Order fridayOrder = Order.inPersonSale(friday.atTime(15, 0), "Cliente", "123");
        fridayOrder.addItem(product, 4);

        when(orders.findAllByDateBetweenAndStatusIn(
                eq(friday.atStartOfDay()), eq(friday.atTime(LocalTime.MAX)), any()))
                .thenReturn(List.of(fridayOrder));

        assertEquals(6, service.getInitialQuantity(12L, friday));
        assertEquals(2, service.getAvailable(product, friday)); // 6 - 4 = 2 leftover

        // Next Monday carries over the 2 leftover from Friday
        when(stocks.findByProductIdAndDate(12L, nextMonday)).thenReturn(Optional.empty());
        when(stocks.findFirstByProductIdAndDateLessThanOrderByDateDesc(12L, nextMonday))
                .thenReturn(Optional.of(mondayStock));

        Order allWeekOrders = Order.inPersonSale(monday.atTime(12, 0), "Cliente", "123");
        allWeekOrders.addItem(product, 18); // 14 + 4 = 18 total sold in week 1

        when(orders.findAllByDateBetweenAndStatusIn(
                eq(monday.atStartOfDay()), eq(LocalDate.of(2026, 11, 8).atTime(LocalTime.MAX)), any()))
                .thenReturn(List.of(allWeekOrders));

        assertEquals(2, service.getInitialQuantity(12L, nextMonday));
        assertEquals(2, service.getAvailable(product, nextMonday));
    }

    @Test
    void manualAdjustmentOnWednesdayForSpoilageOverridesCarryoverAndPropagatesForward() {
        LocalDate wednesday = LocalDate.of(2026, 11, 4);
        LocalDate thursday = LocalDate.of(2026, 11, 5);

        // Wednesday had 2 spoiled cakes, so it was adjusted to 10
        DailyStock wednesdayStock = new DailyStock(product, wednesday, 10);
        when(stocks.findByProductIdAndDate(12L, wednesday)).thenReturn(Optional.of(wednesdayStock));

        assertEquals(10, service.getInitialQuantity(12L, wednesday));
        assertTrue(service.hasExplicitStock(12L, wednesday));

        // Thursday now anchors to Wednesday's 10 minus Wednesday's sales
        when(stocks.findByProductIdAndDate(12L, thursday)).thenReturn(Optional.empty());
        when(stocks.findFirstByProductIdAndDateLessThanOrderByDateDesc(12L, thursday))
                .thenReturn(Optional.of(wednesdayStock));

        Order wednesdayOrder = Order.inPersonSale(wednesday.atTime(14, 0), "Cliente", "123");
        wednesdayOrder.addItem(product, 3);

        when(orders.findAllByDateBetweenAndStatusIn(
                eq(wednesday.atStartOfDay()), eq(wednesday.atTime(LocalTime.MAX)), any()))
                .thenReturn(List.of(wednesdayOrder));

        assertEquals(7, service.getInitialQuantity(12L, thursday)); // 10 - 3 = 7
        assertEquals(7, service.getAvailable(product, thursday));
    }

    @Test
    void resetStockDeletesExplicitStockRecord() {
        LocalDate date = LocalDate.of(2026, 11, 4);

        service.resetStock(12L, date);

        verify(stocks).deleteByProductIdAndDate(12L, date);
    }

    @Test
    void editInitialStockRejectsQuantityLowerThanSoldOnThatDay() {
        LocalDate date = LocalDate.of(2026, 11, 4);

        Order existingSale = Order.inPersonSale(date.atTime(10, 0), "Cliente", "123");
        existingSale.addItem(product, 5);

        when(products.findById(12L)).thenReturn(Optional.of(product));
        when(orders.findAllByDateBetweenAndStatusIn(
                eq(date.atStartOfDay()), eq(date.atTime(LocalTime.MAX)), any()))
                .thenReturn(List.of(existingSale));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.editInitialStock(12L, date, 3));
        assertTrue(ex.getMessage().contains("não pode ser menor que o total já vendido"));
    }
}

