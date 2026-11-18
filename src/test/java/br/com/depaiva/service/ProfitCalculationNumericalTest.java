package br.com.depaiva.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import br.com.depaiva.domain.Channel;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class ProfitCalculationNumericalTest {

    private final OrderRepository orders = mock(OrderRepository.class);
    private final IngredientPurchaseRepository purchases = mock(IngredientPurchaseRepository.class);
    private MetricsService service;

    private final Product cake = new Product("Bolo", new BigDecimal("10.00"), Type.CHILLED_CAKE, null);
    private final Product sandwich = new Product("Sanduíche", new BigDecimal("15.00"), Type.SANDWICH, null);

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(cake, "id", 1L);
        ReflectionTestUtils.setField(sandwich, "id", 2L);
        service = new MetricsService(orders, purchases);
        when(purchases.findAllByDateBetweenOrderByDateDesc(any(), any())).thenReturn(List.of());
    }

    @Test
    @DisplayName("Cálculo de lucro diário, semanal e mensal com exemplo numérico conferido à mão")
    void calculatesDailyWeeklyMonthlyProfitWithHandCalculatedExample() {
        // Exemplo numérico à mão:
        // Bolo: Preço R$ 10.00, Custo snapshot R$ 3.00
        // Sanduíche: Preço R$ 15.00, Custo snapshot R$ 5.00
        //
        // Dia 1 (Segunda 02/11/2026):
        //   Pedido 1: 5 bolos -> Receita R$ 50.00, CMV R$ 15.00, Lucro R$ 35.00
        //   Pedido 2: 2 sanduíches -> Receita R$ 30.00, CMV R$ 10.00, Lucro R$ 20.00
        //   Total Dia 1: Receita R$ 80.00, CMV R$ 25.00, Lucro R$ 55.00, Margem 55/80 = 0.6875 (68,75%)
        //
        // Dia 2 (Terça 03/11/2026):
        //   Pedido 3: 3 bolos -> Receita R$ 30.00, CMV R$ 9.00, Lucro R$ 21.00, Margem 21/30 = 0.7000 (70%)
        //
        // Semana (02/11 a 08/11):
        //   Total Semana: Receita 80 + 30 = R$ 110.00, CMV 25 + 9 = R$ 34.00, Lucro R$ 76.00, Margem 76/110 = 0.6909
        //
        // Mês (Novembro/2026):
        //   Mesma função, agregando os dias do mês: Receita R$ 110.00, CMV R$ 34.00, Lucro R$ 76.00, Margem 0.6909

        Order order1 = Order.create(LocalDateTime.of(2026, 11, 2, 10, 0), Status.DELIVERED, "Cliente 1", null, Channel.IN_PERSON);
        order1.addItem(cake, 5, new BigDecimal("3.00"));

        Order order2 = Order.create(LocalDateTime.of(2026, 11, 2, 15, 0), Status.CONFIRMED, "Cliente 2", null, Channel.IN_PERSON);
        order2.addItem(sandwich, 2, new BigDecimal("5.00"));

        Order order3 = Order.create(LocalDateTime.of(2026, 11, 3, 11, 0), Status.DELIVERED, "Cliente 3", null, Channel.IN_PERSON);
        order3.addItem(cake, 3, new BigDecimal("3.00"));

        List<Order> allOrders = List.of(order1, order2, order3);

        when(orders.findAllByDateBetweenAndStatusIn(any(), any(), any())).thenAnswer(invocation -> {
            LocalDateTime start = invocation.getArgument(0);
            LocalDateTime end = invocation.getArgument(1);
            @SuppressWarnings("unchecked")
            List<Status> statuses = invocation.getArgument(2);
            return allOrders.stream()
                    .filter(o -> !o.getDate().isBefore(start) && !o.getDate().isAfter(end))
                    .filter(o -> statuses.contains(o.getStatus()))
                    .toList();
        });

        // 1. Visão Diária: Dia 1 (02/11)
        MetricsService.Summary day1 = service.calculate(Period.day(LocalDate.of(2026, 11, 2)));
        assertEquals(new BigDecimal("80.00"), day1.sales());
        assertEquals(new BigDecimal("25.00"), day1.cmv());
        assertEquals(new BigDecimal("55.00"), day1.profit());
        assertEquals(new BigDecimal("0.6875"), day1.margin());

        // 2. Visão Diária: Dia 2 (03/11)
        MetricsService.Summary day2 = service.calculate(Period.day(LocalDate.of(2026, 11, 3)));
        assertEquals(new BigDecimal("30.00"), day2.sales());
        assertEquals(new BigDecimal("9.00"), day2.cmv());
        assertEquals(new BigDecimal("21.00"), day2.profit());
        assertEquals(new BigDecimal("0.7000"), day2.margin());

        // 3. Visão Semanal: Semana de 02/11 a 08/11 (função única)
        MetricsService.Summary week = service.calculate(Period.week(LocalDate.of(2026, 11, 2)));
        assertEquals(new BigDecimal("110.00"), week.sales());
        assertEquals(new BigDecimal("34.00"), week.cmv());
        assertEquals(new BigDecimal("76.00"), week.profit());
        assertEquals(new BigDecimal("0.6909"), week.margin());

        // 4. Visão Mensal: Mês de Novembro/2026 (função única)
        MetricsService.Summary month = service.calculate(Period.month(YearMonth.of(2026, 11)));
        assertEquals(new BigDecimal("110.00"), month.sales());
        assertEquals(new BigDecimal("34.00"), month.cmv());
        assertEquals(new BigDecimal("76.00"), month.profit());
        assertEquals(new BigDecimal("0.6909"), month.margin());
    }

    @Test
    @DisplayName("Período sem vendas retorna zeros sem divisão por zero")
    void periodWithoutSalesReturnsZeros() {
        when(orders.findAllByDateBetweenAndStatusIn(any(), any(), any())).thenReturn(List.of());

        MetricsService.Summary summary = service.calculate(Period.day(LocalDate.of(2026, 11, 10)));

        assertEquals(new BigDecimal("0.00"), summary.sales());
        assertEquals(new BigDecimal("0.00"), summary.cmv());
        assertEquals(new BigDecimal("0.00"), summary.profit());
        assertEquals(new BigDecimal("0.0000"), summary.margin());
        assertEquals(0, summary.products().size());
    }
}
