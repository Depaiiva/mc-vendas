package br.com.depaiva.web;

import br.com.depaiva.domain.Period;
import br.com.depaiva.service.MetricsService;
import br.com.depaiva.service.MetricsService.Summary;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.ModelAndView;

@RestController
public class DashboardController {
    private static final ZoneId ZONE = ZoneId.of("America/Sao_Paulo");
    private final MetricsService metrics;
    private final ObjectMapper objectMapper;

    public DashboardController(MetricsService metrics, ObjectMapper objectMapper) {
        this.metrics = metrics;
        this.objectMapper = objectMapper;
    }

    @GetMapping("/dashboard")
    public ModelAndView dashboard(
            @RequestParam(defaultValue = "day") String view,
            @RequestParam(required = false) String date,
            @RequestParam(required = false) String start,
            @RequestParam(required = false) String end) {

        LocalDate today = LocalDate.now(ZONE);
        LocalDate selectedDate;
        try {
            selectedDate = date == null || date.isBlank() ? today : LocalDate.parse(date);
        } catch (RuntimeException e) {
            selectedDate = today;
        }

        String selectedView = normalizeView(view);
        Period period = resolvePeriod(selectedView, selectedDate, start, end);
        Period prevPeriod = previousPeriod(selectedView, period);

        Summary summary = metrics.calculate(period);
        Summary prevSummary = metrics.calculate(prevPeriod);

        // Pre-compute variances (avoid T() in Thymeleaf)
        Variance varSales   = variance(summary.sales(),  prevSummary.sales());
        Variance varProfit  = variance(summary.profit(), prevSummary.profit());
        Variance varTicket  = variance(summary.ticketMedio(), prevSummary.ticketMedio());
        Variance varOrders  = variance(
                BigDecimal.valueOf(summary.totalOrders()),
                BigDecimal.valueOf(prevSummary.totalOrders()));

        // Pre-compute payment bar widths (share * 100, 1 decimal)
        List<PaymentBar> paymentBars = summary.paymentBreakdown().stream()
                .map(pb -> new PaymentBar(
                        pb.label(),
                        pb.amount(),
                        pb.share(),
                        pb.share().multiply(BigDecimal.valueOf(100)).setScale(1, RoundingMode.HALF_UP).toPlainString()
                )).toList();

        // Serialize daySeries to JSON for Chart.js
        List<Map<String, Object>> chartData = summary.daySeries().stream().map(d -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("label", d.label());
            m.put("revenue", d.revenue());
            m.put("profit", d.profit());
            m.put("orders", d.orders());
            return m;
        }).toList();

        String chartJson;
        try {
            chartJson = objectMapper.writeValueAsString(chartData);
        } catch (JsonProcessingException e) {
            chartJson = "[]";
        }

        ModelAndView mv = new ModelAndView("dashboard");
        mv.addObject("view", selectedView);
        mv.addObject("selectedDate", selectedDate);
        mv.addObject("period", period);
        mv.addObject("summary", summary);
        mv.addObject("varSales",  varSales);
        mv.addObject("varProfit", varProfit);
        mv.addObject("varTicket", varTicket);
        mv.addObject("varOrders", varOrders);
        mv.addObject("paymentBars", paymentBars);
        mv.addObject("chartJson", chartJson);
        return mv;
    }

    // ── Variance helpers ──────────────────────────────────────────────────────

    /** Pre-computed KPI variance: sign, formatted "+X,X%" label, CSS class. */
    public record Variance(boolean hasBase, boolean positive, String label) {
        public String cssClass() {
            if (!hasBase) return "variance-neutral";
            return positive ? "variance-up" : "variance-down";
        }
    }

    private static Variance variance(BigDecimal current, BigDecimal base) {
        if (base == null || base.signum() == 0) {
            return new Variance(false, false, "— vs anterior");
        }
        BigDecimal pct = current.subtract(base)
                .multiply(BigDecimal.valueOf(100))
                .divide(base, 1, RoundingMode.HALF_UP);
        boolean positive = pct.signum() >= 0;
        String label = (positive ? "+" : "") + pct.toPlainString().replace('.', ',') + "%";
        return new Variance(true, positive, label);
    }

    /** Payment row with pre-computed CSS bar width string. */
    public record PaymentBar(String label, BigDecimal amount, BigDecimal share, String barWidth) {
        public boolean isPending() {
            return "Pendente / A receber".equals(label) && amount.signum() > 0;
        }
    }

    // ── Period resolution ─────────────────────────────────────────────────────

    private static String normalizeView(String view) {
        return switch (view == null ? "" : view) {
            case "week", "month", "custom" -> view;
            default -> "day";
        };
    }

    private static Period resolvePeriod(String view, LocalDate selectedDate,
                                        String startParam, String endParam) {
        return switch (view) {
            case "week"  -> Period.week(selectedDate);
            case "month" -> Period.month(YearMonth.from(selectedDate));
            case "custom" -> {
                LocalDate s = parseDate(startParam, selectedDate);
                LocalDate e = parseDate(endParam, selectedDate);
                if (e.isBefore(s)) e = s;
                yield new Period(s, e);
            }
            default -> Period.day(selectedDate);
        };
    }

    private static Period previousPeriod(String view, Period current) {
        long days = ChronoUnit.DAYS.between(current.start(), current.end()) + 1;
        return switch (view) {
            case "week"   -> Period.week(current.start().minusWeeks(1));
            case "month"  -> Period.month(YearMonth.from(current.start()).minusMonths(1));
            case "custom" -> new Period(current.start().minusDays(days), current.end().minusDays(days));
            default       -> Period.day(current.start().minusDays(1));
        };
    }

    private static LocalDate parseDate(String value, LocalDate fallback) {
        if (value == null || value.isBlank()) return fallback;
        try {
            return LocalDate.parse(value);
        } catch (RuntimeException e) {
            return fallback;
        }
    }
}
