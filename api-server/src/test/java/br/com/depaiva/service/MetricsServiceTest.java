package br.com.depaiva.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import br.com.depaiva.domain.Channel;
import br.com.depaiva.domain.IngredientPurchase;
import br.com.depaiva.domain.Measure;
import br.com.depaiva.domain.Order;
import br.com.depaiva.domain.Period;
import br.com.depaiva.domain.Product;
import br.com.depaiva.domain.Status;
import br.com.depaiva.domain.Type;
import br.com.depaiva.repository.IngredientPurchaseRepository;
import br.com.depaiva.repository.OrderRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class MetricsServiceTest {
    private final OrderRepository orders = org.mockito.Mockito.mock(OrderRepository.class);
    private final IngredientPurchaseRepository purchases = org.mockito.Mockito.mock(IngredientPurchaseRepository.class);
    private final Product product = new Product("Bolo", new BigDecimal("3.00"), Type.CHILLED_CAKE, null);
    private MetricsService service;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(product, "id", 4L);
        service = new MetricsService(orders, purchases);
    }

    @Test
    void onlyConfirmedAndDeliveredSalesCountAndProductRevenueIsGrouped() {
        LocalDate from = LocalDate.of(2026, 10, 1);
        LocalDate through = LocalDate.of(2026, 10, 31);
        List<Order> fixture = List.of(
                sale(LocalDateTime.of(2026, 10, 1, 0, 0), Status.CONFIRMED, 2),
                sale(LocalDateTime.of(2026, 10, 31, 23, 59), Status.DELIVERED, 3),
                sale(LocalDateTime.of(2026, 10, 15, 11, 0), Status.PENDING, 40),
                sale(LocalDateTime.of(2026, 10, 15, 12, 0), Status.CANCELED, 50),
                sale(LocalDateTime.of(2026, 9, 30, 23, 59), Status.DELIVERED, 7),
                sale(LocalDateTime.of(2026, 11, 1, 0, 0), Status.DELIVERED, 8));
        when(orders.findAllByDateBetweenAndStatusIn(any(), any(), any())).thenAnswer(invocation -> {
            LocalDateTime start = invocation.getArgument(0);
            LocalDateTime end = invocation.getArgument(1);
            @SuppressWarnings("unchecked")
            List<Status> statuses = invocation.getArgument(2);
            return fixture.stream()
                    .filter(order -> !order.getDate().isBefore(start) && !order.getDate().isAfter(end))
                    .filter(order -> statuses.contains(order.getStatus()))
                    .toList();
        });
        when(purchases.findAllByDateBetweenOrderByDateDesc(from, through)).thenReturn(List.of());

        MetricsService.Summary summary = service.calculate(new Period(from, through));

        assertEquals(new BigDecimal("15.00"), summary.sales());
        assertEquals(new BigDecimal("0.00"), summary.expenses());
        assertEquals(new BigDecimal("15.00"), summary.profit());
        assertEquals(1, summary.products().size());
        assertEquals(5, summary.products().getFirst().quantity());
        assertEquals(new BigDecimal("15.00"), summary.products().getFirst().revenue());
    }

    @Test
    void purchaseExpensesAndProfitUseWholePeriod() {
        LocalDate from = LocalDate.of(2026, 10, 13);
        LocalDate through = LocalDate.of(2026, 10, 19);
        when(orders.findAllByDateBetweenAndStatusIn(any(), any(), any()))
                .thenReturn(List.of(sale(from.atTime(9, 0), Status.DELIVERED, 1)));
        IngredientPurchase purchase = new IngredientPurchase(
                from, new BigDecimal("2.000"), new BigDecimal("4.25"),
                br.com.depaiva.domain.Origin.MANUAL,
                new br.com.depaiva.domain.Ingredient("Farinha", Measure.KILO_GRAMS));
        when(purchases.findAllByDateBetweenOrderByDateDesc(from, through)).thenReturn(List.of(purchase));

        MetricsService.Summary summary = service.calculate(new Period(from, through));

        assertEquals(new BigDecimal("3.00"), summary.sales());
        assertEquals(new BigDecimal("8.50"), summary.expenses());
        assertEquals(new BigDecimal("-5.50"), summary.profit());
    }

    @Test
    void weekStartsMondayAndEndsSunday() {
        Period period = Period.week(LocalDate.of(2026, 10, 14));
        assertEquals(LocalDate.of(2026, 10, 12), period.start());
        assertEquals(LocalDate.of(2026, 10, 18), period.end());
    }

    @Test
    void monthIncludesItsLastCalendarDay() {
        Period period = Period.month(YearMonth.of(2024, 2));
        assertEquals(LocalDate.of(2024, 2, 1), period.start());
        assertEquals(LocalDate.of(2024, 2, 29), period.end());
    }

    private Order sale(LocalDateTime date, Status status, int quantity) {
        Order order = Order.create(date, status, null, null, Channel.IN_PERSON);
        order.addItem(product, quantity);
        return order;
    }
}
