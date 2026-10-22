package br.com.depaiva.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import br.com.depaiva.domain.Order;
import br.com.depaiva.domain.Product;
import br.com.depaiva.domain.Type;
import br.com.depaiva.repository.OrderRepository;
import br.com.depaiva.repository.ProductRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class OrderServiceConcurrencyTest {
    @Test
    void simultaneousSalesCannotSellTheLastUnitTwice() throws Exception {
        ProductRepository products = org.mockito.Mockito.mock(ProductRepository.class);
        OrderRepository orders = org.mockito.Mockito.mock(OrderRepository.class);
        StockService stock = org.mockito.Mockito.mock(StockService.class);
        Product lastCake = new Product("Último bolo", new BigDecimal("10.00"), Type.CHILLED_CAKE, null);
        ReflectionTestUtils.setField(lastCake, "id", 88L);
        AtomicInteger available = new AtomicInteger(1);
        when(products.findById(88L)).thenReturn(Optional.of(lastCake));
        when(stock.getAvailable(eq(lastCake), any())).thenAnswer(invocation -> available.get());
        when(stock.canFulfill(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            return order.getItems().stream().allMatch(item -> available.get() >= item.getQuantity());
        });
        when(orders.save(any(Order.class))).thenAnswer(invocation -> {
            Order saved = invocation.getArgument(0);
            available.addAndGet(-saved.getItems().stream().mapToInt(item -> item.getQuantity()).sum());
            return saved;
        });

        OrderService service = new OrderService(orders, products, stock);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
            Future<Boolean> first = executor.submit(() -> register(service, ready, start));
            Future<Boolean> second = executor.submit(() -> register(service, ready, start));
            ready.await();
            start.countDown();
            int successes = (first.get() ? 1 : 0) + (second.get() ? 1 : 0);
            assertEquals(1, successes);
            assertEquals(0, available.get());
        }
    }

    private boolean register(OrderService service, CountDownLatch ready, CountDownLatch start) throws Exception {
        ready.countDown();
        start.await();
        try {
            service.registerInPersonSale(
                    LocalDateTime.of(2026, 10, 13, 11, 0), null, null, Map.of(88L, 1));
            return true;
        } catch (InsufficientStockException exception) {
            return false;
        }
    }
}
