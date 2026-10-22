package br.com.depaiva.web;

import br.com.depaiva.domain.Order;
import br.com.depaiva.domain.Product;
import br.com.depaiva.service.InsufficientStockException;
import br.com.depaiva.service.OrderService;
import br.com.depaiva.service.ProductService;
import br.com.depaiva.service.StockService;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@RestController
public class SalesController {
    private static final ZoneId ZONE = ZoneId.of("America/Sao_Paulo");
    private static final DateTimeFormatter DATETIME_INPUT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");
    private final ProductService productService;
    private final StockService stockService;
    private final OrderService orderService;

    public SalesController(ProductService productService, StockService stockService, OrderService orderService) {
        this.productService = productService;
        this.stockService = stockService;
        this.orderService = orderService;
    }

    @GetMapping("/")
    public ModelAndView root() {
        return new ModelAndView("redirect:/sale");
    }

    @GetMapping("/login")
    public ModelAndView login(@RequestParam(required = false) String error) {
        ModelAndView modelAndView = new ModelAndView("login");
        if ("locked".equals(error)) {
            modelAndView.addObject("loginError", "Muitas tentativas. Aguarde 15 minutos para tentar novamente.");
        } else if ("invalid".equals(error)) {
            modelAndView.addObject("loginError", "Usuário ou senha incorretos.");
        }
        return modelAndView;
    }

    @GetMapping("/sale")
    public ModelAndView quickSale() {
        LocalDateTime now = LocalDateTime.now(ZONE).withSecond(0).withNano(0);
        return showQuickSale(now.toLocalDate(), now);
    }

    @GetMapping("/sale/date")
    public ModelAndView quickSaleForDate(@RequestParam(required = false) String date) {
        LocalDate selectedDate = parseDate(date, LocalDate.now(ZONE));
        LocalDateTime now = LocalDateTime.now(ZONE).withSecond(0).withNano(0);
        return showQuickSale(selectedDate, now.toLocalDate().equals(selectedDate) ? now : selectedDate.atTime(LocalTime.NOON));
    }

    @PostMapping("/sale")
    public ModelAndView registerSale(@RequestParam Map<String, String> fields, RedirectAttributes flash) {
        try {
            LocalDateTime date = LocalDateTime.parse(fields.get("date"), DATETIME_INPUT);
            Map<Long, Integer> quantities = new LinkedHashMap<>();
            for (Map.Entry<String, String> entry : fields.entrySet()) {
                if (entry.getKey().startsWith("qty_") && !entry.getValue().isBlank()) {
                    int quantity = Integer.parseInt(entry.getValue());
                    if (quantity < 0) throw new IllegalArgumentException("A quantidade não pode ser negativa.");
                    quantities.put(Long.parseLong(entry.getKey().substring(4)), quantity);
                }
            }
            Order order = orderService.registerInPersonSale(
                    date, fields.get("customerName"), fields.get("customerContact"), quantities);
            flash.addFlashAttribute("success", "Venda registrada: " + money(order.calculateTotal()) + ".");
        } catch (IllegalArgumentException exception) {
            flash.addFlashAttribute("error", exception.getMessage());
            flash.addFlashAttribute("keepDate", fields.get("date"));
        } catch (RuntimeException exception) {
            flash.addFlashAttribute("error", "Não foi possível registrar a venda. Confira os dados e tente novamente.");
        }
        return new ModelAndView("redirect:/sale");
    }

    @GetMapping("/orders")
    public ModelAndView orders(@RequestParam(required = false) String date) {
        LocalDate selectedDate = parseDate(date, LocalDate.now(ZONE));
        List<Order> list = orderService.findForDate(selectedDate.atStartOfDay(), selectedDate.atTime(LocalTime.MAX));
        ModelAndView modelAndView = new ModelAndView("orders");
        modelAndView.addObject("selectedDate", selectedDate);
        modelAndView.addObject("orders", list);
        return modelAndView;
    }

    @PostMapping("/orders/{id}/cancel")
    public ModelAndView cancelOrder(@PathVariable Long id, @RequestParam String date, RedirectAttributes flash) {
        try {
            orderService.cancel(id);
            flash.addFlashAttribute("success", "Venda cancelada.");
        } catch (IllegalArgumentException exception) {
            flash.addFlashAttribute("error", exception.getMessage());
        }
        return new ModelAndView("redirect:/orders?date=" + date);
    }

    private ModelAndView showQuickSale(LocalDate date, LocalDateTime time) {
        ModelAndView modelAndView = new ModelAndView("sale");
        List<SaleProduct> products = productService.active().stream()
                .map(product -> new SaleProduct(product, stockService.getAvailable(product, date)))
                .toList();
        modelAndView.addObject("products", products);
        modelAndView.addObject("selectedDate", date);
        modelAndView.addObject("dateTime", time.format(DATETIME_INPUT));
        modelAndView.addObject("selectedDateValue", date.toString());
        return modelAndView;
    }

    private static LocalDate parseDate(String value, LocalDate fallback) {
        try {
            return value == null || value.isBlank() ? fallback : LocalDate.parse(value);
        } catch (RuntimeException exception) {
            return fallback;
        }
    }

    private static String money(java.math.BigDecimal value) {
        return "R$ " + java.text.NumberFormat.getNumberInstance(java.util.Locale.forLanguageTag("pt-BR"))
                .format(value.setScale(2, RoundingMode.HALF_UP));
    }

    public record SaleProduct(Product product, int available) {
        public boolean soldOut() { return available <= 0; }
    }
}
