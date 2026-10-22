package br.com.depaiva.web;

import br.com.depaiva.domain.Ingredient;
import br.com.depaiva.domain.IngredientPurchase;
import br.com.depaiva.domain.Measure;
import br.com.depaiva.domain.Product;
import br.com.depaiva.domain.Type;
import br.com.depaiva.service.IngredientService;
import br.com.depaiva.service.ProductService;
import br.com.depaiva.service.StockService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@RestController
public class InventoryController {
    private static final ZoneId ZONE = ZoneId.of("America/Sao_Paulo");
    private final ProductService products;
    private final StockService stock;
    private final IngredientService ingredients;

    public InventoryController(ProductService products, StockService stock, IngredientService ingredients) {
        this.products = products;
        this.stock = stock;
        this.ingredients = ingredients;
    }

    @GetMapping("/stock")
    public ModelAndView stockPage(@RequestParam(required = false) String date) {
        LocalDate selectedDate = parseDate(date, LocalDate.now(ZONE));
        List<StockLine> lines = new ArrayList<>();
        for (Product product : products.active()) {
            int initial = stock.getInitialQuantity(product.getId(), selectedDate);
            int sold = stock.getSoldQuantity(product.getId(), selectedDate);
            lines.add(new StockLine(product, initial, sold, initial - sold));
        }
        ModelAndView modelAndView = new ModelAndView("stock");
        modelAndView.addObject("selectedDate", selectedDate);
        modelAndView.addObject("stockLines", lines);
        return modelAndView;
    }

    @PostMapping("/stock")
    public ModelAndView saveStock(@RequestParam String date, @RequestParam Map<String, String> fields,
                                  RedirectAttributes flash) {
        try {
            LocalDate selectedDate = LocalDate.parse(date);
            for (Product product : products.active()) {
                String value = fields.get("initial_" + product.getId());
                if (value != null && !value.isBlank()) {
                    stock.editInitialStock(product.getId(), selectedDate, Integer.parseInt(value));
                }
            }
            flash.addFlashAttribute("success", "Estoque inicial salvo.");
        } catch (RuntimeException exception) {
            flash.addFlashAttribute("error", safeMessage(exception));
        }
        return new ModelAndView("redirect:/stock?date=" + date);
    }

    @GetMapping("/products")
    public ModelAndView products() {
        ModelAndView modelAndView = new ModelAndView("products");
        modelAndView.addObject("products", products.all());
        modelAndView.addObject("types", Type.values());
        return modelAndView;
    }

    @PostMapping("/products/save")
    public ModelAndView saveProduct(@RequestParam(required = false) Long id, @RequestParam String name,
                                    @RequestParam BigDecimal price, @RequestParam Type type,
                                    @RequestParam(required = false) String description,
                                    @RequestParam(defaultValue = "false") boolean active,
                                    RedirectAttributes flash) {
        try {
            products.save(id, name, price, type, description, active);
            flash.addFlashAttribute("success", id == null ? "Produto criado." : "Produto atualizado.");
        } catch (RuntimeException exception) {
            flash.addFlashAttribute("error", safeMessage(exception));
        }
        return new ModelAndView("redirect:/products");
    }

    @PostMapping("/products/{id}/toggle")
    public ModelAndView toggleProduct(@PathVariable Long id, @RequestParam boolean active, RedirectAttributes flash) {
        try {
            products.setActive(id, active);
            flash.addFlashAttribute("success", active ? "Produto ativado." : "Produto desativado.");
        } catch (RuntimeException exception) {
            flash.addFlashAttribute("error", safeMessage(exception));
        }
        return new ModelAndView("redirect:/products");
    }

    @GetMapping("/ingredients")
    public ModelAndView ingredients(@RequestParam(required = false) String start,
                                    @RequestParam(required = false) String end) {
        YearMonth current = YearMonth.from(LocalDate.now(ZONE));
        LocalDate startDate = parseDate(start, current.atDay(1));
        LocalDate endDate = parseDate(end, current.atEndOfMonth());
        List<IngredientPurchase> purchases = ingredients.purchases(startDate, endDate);
        BigDecimal total = purchases.stream().map(IngredientPurchase::calculateTotalCost)
                .reduce(BigDecimal.ZERO.setScale(2), BigDecimal::add);
        ModelAndView modelAndView = new ModelAndView("ingredients");
        modelAndView.addObject("ingredients", ingredients.all());
        modelAndView.addObject("measures", Measure.values());
        modelAndView.addObject("purchases", purchases);
        modelAndView.addObject("startDate", startDate);
        modelAndView.addObject("endDate", endDate);
        modelAndView.addObject("today", LocalDate.now(ZONE));
        modelAndView.addObject("purchaseTotal", total);
        return modelAndView;
    }

    @PostMapping("/ingredients/save")
    public ModelAndView saveIngredient(@RequestParam(required = false) Long id, @RequestParam String name,
                                       @RequestParam Measure unit, RedirectAttributes flash) {
        try {
            ingredients.saveIngredient(id, name, unit);
            flash.addFlashAttribute("success", id == null ? "Ingrediente criado." : "Ingrediente atualizado.");
        } catch (RuntimeException exception) {
            flash.addFlashAttribute("error", safeMessage(exception));
        }
        return new ModelAndView("redirect:/ingredients");
    }

    @PostMapping("/ingredients/purchases")
    public ModelAndView registerPurchase(@RequestParam String date, @RequestParam Long ingredientId,
                                         @RequestParam BigDecimal quantity, @RequestParam BigDecimal unitPrice,
                                         RedirectAttributes flash) {
        try {
            ingredients.registerPurchase(LocalDate.parse(date), ingredientId, quantity, unitPrice);
            flash.addFlashAttribute("success", "Compra registrada.");
        } catch (RuntimeException exception) {
            flash.addFlashAttribute("error", safeMessage(exception));
        }
        return new ModelAndView("redirect:/ingredients");
    }

    private static LocalDate parseDate(String value, LocalDate fallback) {
        try {
            return value == null || value.isBlank() ? fallback : LocalDate.parse(value);
        } catch (RuntimeException exception) {
            return fallback;
        }
    }

    private static String safeMessage(RuntimeException exception) {
        return exception.getMessage() == null ? "Não foi possível salvar. Confira os dados." : exception.getMessage();
    }

    public record StockLine(Product product, int initial, int sold, int available) {}
}
