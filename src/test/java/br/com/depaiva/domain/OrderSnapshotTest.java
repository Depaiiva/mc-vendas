package br.com.depaiva.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class OrderSnapshotTest {

    @Test
    @DisplayName("Snapshot do custo unitário e preço de venda não mudam após alteração posterior de preço do produto ou receita")
    void unitCostSnapshotAndPriceDoNotChangeWhenProductOrCostChangesLater() {
        Product cake = new Product("Bolo Gelado", new BigDecimal("10.00"), Type.CHILLED_CAKE, "Individual");
        BigDecimal initialCostSnapshot = new BigDecimal("2.50");

        // Cria pedido com 3 unidades e grava o snapshot do custo naquele momento
        Order order = Order.inPersonSale(LocalDateTime.of(2026, 11, 1, 10, 0), "Cliente", null);
        order.addItem(cake, 3, initialCostSnapshot);

        OrderItem item = order.getItems().getFirst();
        assertEquals(new BigDecimal("10.00"), item.getUnitPrice());
        assertEquals(new BigDecimal("2.50"), item.getUnitCostSnapshot());
        assertEquals(new BigDecimal("30.00"), item.calculateSubtotal());
        assertEquals(new BigDecimal("7.50"), item.calculateCmv());

        // Alterações posteriores: o preço de venda do bolo sobe para R$ 15.00
        cake.changePrice(new BigDecimal("15.00"));

        // O item do pedido previamente realizado DEVE manter exatamente os valores daquele momento
        assertEquals(new BigDecimal("10.00"), item.getUnitPrice(), "Preço histórico de venda não deve mudar.");
        assertEquals(new BigDecimal("2.50"), item.getUnitCostSnapshot(), "Custo histórico snapshot não deve mudar.");
        assertEquals(new BigDecimal("30.00"), item.calculateSubtotal(), "Subtotal histórico não deve mudar.");
        assertEquals(new BigDecimal("7.50"), item.calculateCmv(), "CMV histórico não deve mudar.");
    }
}
