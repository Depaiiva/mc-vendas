package br.com.depaiva.service;

import br.com.depaiva.domain.IngredientPurchase;
import br.com.depaiva.domain.Order;
import br.com.depaiva.domain.OrderItem;
import br.com.depaiva.domain.PaymentMethod;
import br.com.depaiva.domain.Period;
import br.com.depaiva.domain.Product;
import br.com.depaiva.domain.Status;
import br.com.depaiva.repository.IngredientPurchaseRepository;
import br.com.depaiva.repository.OrderRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MetricsService {
    private static final List<Status> SALE_STATUSES = List.of(Status.CONFIRMED, Status.DELIVERED);
    private static final Locale PT_BR = Locale.forLanguageTag("pt-BR");
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

        BigDecimal revenue = BigDecimal.ZERO;
        BigDecimal cmv = BigDecimal.ZERO;
        int totalOrders = saleOrders.size();

        // Payment breakdown
        Map<PaymentMethod, BigDecimal> paymentTotals = new EnumMap<>(PaymentMethod.class);
        for (PaymentMethod pm : PaymentMethod.values()) paymentTotals.put(pm, BigDecimal.ZERO);
        BigDecimal pendingTotal = BigDecimal.ZERO;

        // Daily series: date -> (revenue, profit, orderCount)
        Map<LocalDate, DayAccumulator> dailyMap = new LinkedHashMap<>();

        Map<Long, ProductAccumulator> byProduct = new HashMap<>();

        for (Order order : saleOrders) {
            BigDecimal orderTotal = order.calculateTotal();
            BigDecimal orderCmv = BigDecimal.ZERO;

            for (OrderItem item : order.getItems()) {
                BigDecimal itemSubtotal = item.calculateSubtotal();
                BigDecimal itemCmv = item.calculateCmv();

                revenue = revenue.add(itemSubtotal);
                cmv = cmv.add(itemCmv);
                orderCmv = orderCmv.add(itemCmv);

                Long productId = item.getProduct().getId();
                ProductAccumulator accumulator = byProduct.computeIfAbsent(
                        productId, ignored -> new ProductAccumulator(item.getProduct()));
                accumulator.quantity += item.getQuantity();
                accumulator.revenue = accumulator.revenue.add(itemSubtotal);
                accumulator.cmv = accumulator.cmv.add(itemCmv);
            }

            // payment breakdown
            PaymentMethod pm = order.getPaymentMethod();
            if (pm != null) {
                paymentTotals.put(pm, paymentTotals.get(pm).add(orderTotal));
            } else {
                pendingTotal = pendingTotal.add(orderTotal);
            }

            // daily accumulation
            LocalDate day = order.getDate().toLocalDate();
            DayAccumulator da = dailyMap.computeIfAbsent(day, d -> new DayAccumulator());
            da.revenue = da.revenue.add(orderTotal);
            da.profit = da.profit.add(orderTotal.subtract(orderCmv));
            da.orders++;
        }

        BigDecimal grossProfit = revenue.subtract(cmv).setScale(2, RoundingMode.HALF_UP);
        BigDecimal margin = revenue.signum() == 0
                ? BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP)
                : grossProfit.divide(revenue, 4, RoundingMode.HALF_UP);

        BigDecimal ticketMedio = totalOrders == 0
                ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
                : revenue.divide(BigDecimal.valueOf(totalOrders), 2, RoundingMode.HALF_UP);

        BigDecimal cashExpenses = periodPurchases.stream()
                .map(IngredientPurchase::calculateTotalCost)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal cashBalance = revenue.subtract(cashExpenses).setScale(2, RoundingMode.HALF_UP);

        // Build product sales list sorted by revenue desc (top 5 for ranking)
        List<ProductSales> productSales = new ArrayList<>();
        for (ProductAccumulator value : byProduct.values()) {
            productSales.add(new ProductSales(
                    value.product,
                    value.quantity,
                    value.revenue.setScale(2, RoundingMode.HALF_UP),
                    value.cmv.setScale(2, RoundingMode.HALF_UP),
                    value.revenue.subtract(value.cmv).setScale(2, RoundingMode.HALF_UP)
            ));
        }
        List<ProductSales> byRevenue = new ArrayList<>(productSales);
        byRevenue.sort(Comparator.comparing(ProductSales::revenue).reversed());
        List<ProductSales> top5 = byRevenue.stream().limit(5).toList();

        productSales.sort(Comparator.comparing(metric -> metric.product().getName(), String.CASE_INSENSITIVE_ORDER));

        // Build payment breakdown list
        List<PaymentBreakdown> paymentBreakdown = new ArrayList<>();
        BigDecimal totalRevenue = revenue;
        for (PaymentMethod pm : PaymentMethod.values()) {
            BigDecimal amount = paymentTotals.get(pm).setScale(2, RoundingMode.HALF_UP);
            BigDecimal pct = totalRevenue.signum() == 0 ? BigDecimal.ZERO
                    : amount.divide(totalRevenue, 4, RoundingMode.HALF_UP);
            paymentBreakdown.add(new PaymentBreakdown(pm.getDisplayName(), amount, pct));
        }
        // pending entry
        BigDecimal pendingAmt = pendingTotal.setScale(2, RoundingMode.HALF_UP);
        BigDecimal pendingPct = totalRevenue.signum() == 0 ? BigDecimal.ZERO
                : pendingAmt.divide(totalRevenue, 4, RoundingMode.HALF_UP);
        paymentBreakdown.add(new PaymentBreakdown("Pendente / A receber", pendingAmt, pendingPct));

        // Build daily series for chart (filling gaps for period)
        List<DayData> daySeries = buildDaySeries(period, dailyMap);

        return new Summary(
                revenue.setScale(2, RoundingMode.HALF_UP),
                cmv.setScale(2, RoundingMode.HALF_UP),
                grossProfit,
                margin,
                cashExpenses,
                cashBalance,
                List.copyOf(productSales),
                ticketMedio,
                totalOrders,
                List.copyOf(paymentBreakdown),
                List.copyOf(daySeries),
                List.copyOf(top5)
        );
    }

    /** Fills each calendar day in the period, even with zero revenue. */
    private List<DayData> buildDaySeries(Period period, Map<LocalDate, DayAccumulator> dailyMap) {
        List<DayData> series = new ArrayList<>();
        LocalDate cursor = period.start();
        while (!cursor.isAfter(period.end())) {
            DayAccumulator da = dailyMap.getOrDefault(cursor, new DayAccumulator());
            String label = cursor.getDayOfWeek().getDisplayName(TextStyle.SHORT, PT_BR)
                    + " " + cursor.getDayOfMonth();
            series.add(new DayData(
                    cursor,
                    label,
                    da.revenue.setScale(2, RoundingMode.HALF_UP),
                    da.profit.setScale(2, RoundingMode.HALF_UP),
                    da.orders
            ));
            cursor = cursor.plusDays(1);
        }
        return series;
    }

    // ── inner helpers ────────────────────────────────────────────────────────

    private static final class ProductAccumulator {
        private final Product product;
        private int quantity;
        private BigDecimal revenue = BigDecimal.ZERO;
        private BigDecimal cmv = BigDecimal.ZERO;

        private ProductAccumulator(Product product) {
            this.product = product;
        }
    }

    private static final class DayAccumulator {
        private BigDecimal revenue = BigDecimal.ZERO;
        private BigDecimal profit = BigDecimal.ZERO;
        private int orders = 0;
    }

    // ── public records ───────────────────────────────────────────────────────

    public record ProductSales(Product product, int quantity, BigDecimal revenue, BigDecimal cmv, BigDecimal profit) {
        public ProductSales(Product product, int quantity, BigDecimal revenue) {
            this(product, quantity, revenue, BigDecimal.ZERO, revenue);
        }
    }

    public record PaymentBreakdown(String label, BigDecimal amount, BigDecimal share) {}

    public record DayData(LocalDate date, String label, BigDecimal revenue, BigDecimal profit, int orders) {}

    public record Summary(
            BigDecimal sales,
            BigDecimal cmv,
            BigDecimal profit,
            BigDecimal margin,
            BigDecimal cashExpenses,
            BigDecimal cashBalance,
            List<ProductSales> products,
            BigDecimal ticketMedio,
            int totalOrders,
            List<PaymentBreakdown> paymentBreakdown,
            List<DayData> daySeries,
            List<ProductSales> top5
    ) {
        public BigDecimal revenue() { return sales; }
        public BigDecimal grossProfit() { return profit; }
        public BigDecimal expenses() { return cashExpenses; }
    }
}
