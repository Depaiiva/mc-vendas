package br.com.depaiva.service;

import br.com.depaiva.domain.IngredientPurchase;
import br.com.depaiva.domain.Order;
import br.com.depaiva.domain.OrderItem;
import br.com.depaiva.domain.Period;
import br.com.depaiva.domain.Product;
import br.com.depaiva.domain.Status;
import br.com.depaiva.repository.IngredientPurchaseRepository;
import br.com.depaiva.repository.OrderRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MetricsService {
    private static final List<Status> SALE_STATUSES = List.of(Status.CONFIRMED, Status.DELIVERED);
    private final OrderRepository orders;
    private final IngredientPurchaseRepository purchases;

    public MetricsService(OrderRepository orders, IngredientPurchaseRepository purchases) {
        this.orders = orders;
        this.purchases = purchases;
    }

    @Transactional(readOnly = true)
    public Summary calculate(Period period) {
        List<Order> saleOrders = orders.findAllByDateBetweenAndStatusIn(
                period.start().atStartOfDay(), period.end().atTime(LocalTime.MAX), SALE_STATUSES);
        List<IngredientPurchase> periodPurchases =
                purchases.findAllByDateBetweenOrderByDateDesc(period.start(), period.end());

        BigDecimal sales = saleOrders.stream().map(Order::calculateTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal expenses = periodPurchases.stream()
                .map(IngredientPurchase::calculateTotalCost)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        Map<Long, ProductAccumulator> byProduct = new HashMap<>();
        for (Order order : saleOrders) {
            for (OrderItem item : order.getItems()) {
                Long productId = item.getProduct().getId();
                ProductAccumulator accumulator = byProduct.computeIfAbsent(
                        productId, ignored -> new ProductAccumulator(item.getProduct()));
                accumulator.quantity += item.getQuantity();
                accumulator.revenue = accumulator.revenue.add(item.calculateSubtotal());
            }
        }
        List<ProductSales> productSales = new ArrayList<>();
        for (ProductAccumulator value : byProduct.values()) {
            productSales.add(new ProductSales(value.product, value.quantity, value.revenue.setScale(2, RoundingMode.HALF_UP)));
        }
        productSales.sort(Comparator.comparing(metric -> metric.product().getName(), String.CASE_INSENSITIVE_ORDER));
        return new Summary(
                sales.setScale(2, RoundingMode.HALF_UP),
                expenses.setScale(2, RoundingMode.HALF_UP),
                sales.subtract(expenses).setScale(2, RoundingMode.HALF_UP),
                List.copyOf(productSales));
    }

    private static final class ProductAccumulator {
        private final Product product;
        private int quantity;
        private BigDecimal revenue = BigDecimal.ZERO;

        private ProductAccumulator(Product product) {
            this.product = product;
        }
    }

    public record ProductSales(Product product, int quantity, BigDecimal revenue) {}
    public record Summary(BigDecimal sales, BigDecimal expenses, BigDecimal profit, List<ProductSales> products) {}
}
