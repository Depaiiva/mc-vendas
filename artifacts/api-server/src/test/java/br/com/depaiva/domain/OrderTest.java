package br.com.depaiva.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class OrderTest {
    @Test
    void addItemMergesQuantitiesAndKeepsPriceSnapshot() {
        Product product = product("4.50");
        Order order = Order.inPersonSale(LocalDateTime.now(), null, null);

        order.addItem(product, 2);
        product.changePrice(new BigDecimal("5.25"));
        order.addItem(product, 3);

        assertEquals(1, order.getItems().size());
        assertEquals(5, order.getItems().getFirst().getQuantity());
        assertEquals(new BigDecimal("4.50"), order.getItems().getFirst().getUnitPrice());
        assertEquals(new BigDecimal("22.50"), order.calculateTotal());
        assertEquals("Presencial", order.getCustomerName());
        assertEquals(Channel.IN_PERSON, order.getChannel());
    }

    @Test
    void inactiveProductIsRejected() {
        Product product = product("4.50");
        product.deactivate();
        Order order = Order.inPersonSale(LocalDateTime.now(), "Cliente", null);

        assertThrows(IllegalArgumentException.class, () -> order.addItem(product, 1));
    }

    @Test
    void validTransitionsAreEnforced() {
        Order pending = order(Status.PENDING);
        assertTrue(pending.canCancel());
        pending.confirm();
        assertTrue(pending.canCancel());
        pending.deliver();
        assertEquals(Status.DELIVERED, pending.getStatus());
        assertTrue(pending.canCancel());

        Order canceledFromPending = order(Status.PENDING);
        canceledFromPending.cancel();
        assertEquals(Status.CANCELED, canceledFromPending.getStatus());

        Order canceledFromConfirmed = order(Status.CONFIRMED);
        canceledFromConfirmed.cancel();
        assertEquals(Status.CANCELED, canceledFromConfirmed.getStatus());

        Order canceledFromDelivered = order(Status.DELIVERED);
        canceledFromDelivered.cancel();
        assertEquals(Status.CANCELED, canceledFromDelivered.getStatus());
        assertFalse(canceledFromDelivered.canCancel());
    }

    @Test
    void invalidTransitionsThrowInsteadOfFailingSilently() {
        assertThrows(IllegalStateException.class, () -> order(Status.PENDING).deliver());

        Order delivered = order(Status.DELIVERED);
        assertThrows(IllegalStateException.class, delivered::confirm);
        assertThrows(IllegalStateException.class, delivered::deliver);

        Order canceled = order(Status.CANCELED);
        assertThrows(IllegalStateException.class, canceled::confirm);
        assertThrows(IllegalStateException.class, canceled::deliver);
        assertThrows(IllegalStateException.class, canceled::cancel);
    }

    @Test
    void calculateTotalSumsEveryItemAtTwoDecimalPlaces() {
        Product cake = product("8.75");
        Product sandwich = new Product("Sanduíche", new BigDecimal("11.20"), Type.SANDWICH, null);
        Order order = Order.inPersonSale(LocalDateTime.now(), null, null);
        order.addItem(cake, 2);
        order.addItem(sandwich, 1);

        assertEquals(new BigDecimal("28.70"), order.calculateTotal());
    }

    private static Product product(String price) {
        return new Product("Bolo", new BigDecimal(price), Type.CHILLED_CAKE, null);
    }

    private static Order order(Status status) {
        Order order = Order.create(LocalDateTime.now(), status, "Teste", null, Channel.IN_PERSON);
        order.addItem(product("1.00"), 1);
        return order;
    }
}
