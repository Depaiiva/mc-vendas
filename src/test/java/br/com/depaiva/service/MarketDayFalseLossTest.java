package br.com.depaiva.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import br.com.depaiva.domain.Channel;
import br.com.depaiva.domain.Ingredient;
import br.com.depaiva.domain.IngredientPurchase;
import br.com.depaiva.domain.Measure;
import br.com.depaiva.domain.Order;
import br.com.depaiva.domain.Origin;
import br.com.depaiva.domain.Period;
import br.com.depaiva.domain.Product;
import br.com.depaiva.domain.Status;
import br.com.depaiva.domain.Type;
import br.com.depaiva.repository.IngredientPurchaseRepository;
import br.com.depaiva.repository.OrderRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class MarketDayFalseLossTest {

    private final OrderRepository orders = mock(OrderRepository.class);
    private final IngredientPurchaseRepository purchases = mock(IngredientPurchaseRepository.class);
    private final MetricsService service = new MetricsService(orders, purchases);

    @Test
    @DisplayName("O dia de compras no mercado não deve aparecer como prejuízo falso: compras entram no caixa e lucro usa CMV")
    void marketDayDoesNotShowFalseLoss() {
        LocalDate marketDay = LocalDate.of(2026, 11, 4);

        // Produto vendido no dia: Bolo gelado (Preço R$ 10.00, CMV unitário snapshot R$ 2.50)
        Product cake = new Product("Bolo Gelado", new BigDecimal("10.00"), Type.CHILLED_CAKE, null);
        ReflectionTestUtils.setField(cake, "id", 1L);

        // Venda de 2 bolos no dia
        Order sale = Order.create(marketDay.atTime(14, 0), Status.DELIVERED, "Cliente", null, Channel.IN_PERSON);
        sale.addItem(cake, 2, new BigDecimal("2.50"));
        when(orders.findAllByDateBetweenAndStatusIn(any(), any(), any())).thenReturn(List.of(sale));

        // Grande compra de mercado no mesmo dia: R$ 500.00 em farinha para abastecer o estoque
        Ingredient flour = new Ingredient("Farinha de Trigo", Measure.GRAMS);
        IngredientPurchase bigPurchase = new IngredientPurchase(
                marketDay,
                new BigDecimal("50000.000"), // 50kg
                new BigDecimal("0.0100"),
                new BigDecimal("500.00"),
                Origin.MANUAL,
                flour
        );
        when(purchases.findAllByDateBetweenOrderByDateDesc(marketDay, marketDay)).thenReturn(List.of(bigPurchase));

        MetricsService.Summary summary = service.calculate(Period.day(marketDay));

        // 1. Receita das vendas do dia
        assertEquals(new BigDecimal("20.00"), summary.sales());

        // 2. CMV dos itens vendidos (2 x 2.50)
        assertEquals(new BigDecimal("5.00"), summary.cmv());

        // 3. Lucro Bruto do dia: Receita - CMV = 20.00 - 5.00 = +R$ 15.00 (NÃO é prejuízo!)
        assertEquals(new BigDecimal("15.00"), summary.profit());
        assertTrue(summary.profit().signum() > 0, "O lucro do dia deve ser positivo e não prejuízo falso.");

        // 4. Margem de lucro: 15 / 20 = 75%
        assertEquals(new BigDecimal("0.7500"), summary.margin());

        // 5. Visão de fluxo de caixa: compras do período expostas separadamente
        assertEquals(new BigDecimal("500.00"), summary.cashExpenses());
        assertEquals(new BigDecimal("-480.00"), summary.cashBalance());
    }
}
