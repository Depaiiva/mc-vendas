package br.com.depaiva.web;

import br.com.depaiva.domain.IngredientPurchase;
import br.com.depaiva.domain.Measure;
import br.com.depaiva.domain.Product;
import br.com.depaiva.domain.ProductIngredient;
import br.com.depaiva.domain.Type;
import br.com.depaiva.service.IngredientService;
import br.com.depaiva.service.ProductService;
import br.com.depaiva.service.StockService;
import br.com.depaiva.service.cost.ProductCostResult;
import br.com.depaiva.service.cost.ProductCostService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
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
    private final ProductCostService productCostService;

    public InventoryController(ProductService products, StockService stock, IngredientService ingredients) {
        this(products, stock, ingredients, null);
    }

    @Autowired
    public InventoryController(ProductService products, StockService stock, IngredientService ingredients,
                               ProductCostService productCostService) {
        this.products = products;
        this.stock = stock;
        this.ingredients = ingredients;
        this.productCostService = productCostService;
    }

    @GetMapping("/stock")
    public ModelAndView stockPage(@RequestParam(required = false) String date) {
        LocalDate selectedDate = parseDate(date, LocalDate.now(ZONE));
        List<StockLine> lines = new ArrayList<>();
        for (Product product : products.active()) {
            int initial = stock.getInitialQuantity(product.getId(), selectedDate);
            int sold = stock.getSoldQuantity(product.getId(), selectedDate);
            boolean explicit = stock.hasExplicitStock(product.getId(), selectedDate);
            int carriedOver = stock.getCarriedOverStock(product.getId(), selectedDate);
            lines.add(new StockLine(product, initial, sold, initial - sold, explicit, carriedOver));
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
            flash.addFlashAttribute("success", "Estoque salvo com sucesso.");
        } catch (RuntimeException exception) {
            flash.addFlashAttribute("error", safeMessage(exception));
        }
        return new ModelAndView("redirect:/stock?date=" + date);
    }

    @PostMapping("/stock/reset")
    public ModelAndView resetStock(@RequestParam String date, @RequestParam Long productId,
                                   RedirectAttributes flash) {
        try {
            LocalDate selectedDate = LocalDate.parse(date);
            stock.resetStock(productId, selectedDate);
            flash.addFlashAttribute("success", "Estoque restaurado para o cálculo automático.");
        } catch (RuntimeException exception) {
            flash.addFlashAttribute("error", safeMessage(exception));
        }
        return new ModelAndView("redirect:/stock?date=" + date);
    }

    @GetMapping("/products")
    public ModelAndView products() {
        ModelAndView modelAndView = new ModelAndView("products");
        List<Product> allProducts = products.all();
        Map<Long, ProductCostResult> costs = new HashMap<>();
        Map<Long, List<ProductIngredient>> recipes = new HashMap<>();
        for (Product product : allProducts) {
            recipes.put(product.getId(), products.getRecipe(product.getId()));
            if (productCostService != null) {
                costs.put(product.getId(), productCostService.calculateUnitCost(product));
            }
        }
        modelAndView.addObject("products", allProducts);
        modelAndView.addObject("types", Type.values());
        modelAndView.addObject("costs", costs);
        modelAndView.addObject("recipes", recipes);
        modelAndView.addObject("availableIngredients", ingredients.all());
        return modelAndView;
    }

    @PostMapping(path = {"/products/save", "/products"})
    public ModelAndView saveProduct(@RequestParam(required = false) Long id, @RequestParam String name,
                                    @RequestParam BigDecimal price, @RequestParam Type type,
                                    @RequestParam(required = false) String description,
                                    @RequestParam(defaultValue = "false") boolean active,
                                    RedirectAttributes flash) {
        try {
            products.save(id, name, price, type, description, active);
            flash.addFlashAttribute("success", id == null ? "Produto criado com sucesso." : "Produto atualizado com sucesso.");
        } catch (RuntimeException exception) {
            flash.addFlashAttribute("error", safeMessage(exception));
        }
        return new ModelAndView("redirect:/products");
    }

    @PostMapping(path = {"/products/{id}/edit", "/products/{id}"})
    public ModelAndView editProduct(@PathVariable Long id, @RequestParam String name,
                                    @RequestParam BigDecimal price, @RequestParam Type type,
                                    @RequestParam(required = false) String description,
                                    @RequestParam(defaultValue = "false") boolean active,
                                    RedirectAttributes flash) {
        return saveProduct(id, name, price, type, description, active, flash);
    }

    @PostMapping("/products/{id}/ingredients")
    public ModelAndView addRecipeIngredient(@PathVariable Long id,
                                            @RequestParam Long ingredientId,
                                            @RequestParam BigDecimal quantity,
                                            @RequestParam(name = "recipeYield", defaultValue = "1") int recipeYield,
                                            RedirectAttributes flash) {
        try {
            products.addOrUpdateRecipeIngredient(id, ingredientId, quantity, recipeYield);
            flash.addFlashAttribute("success", "Ingrediente adicionado à ficha técnica.");
        } catch (RuntimeException exception) {
            flash.addFlashAttribute("error", safeMessage(exception));
        }
        return new ModelAndView("redirect:/products");
    }

    @PostMapping("/products/{id}/ingredients/{recipeId}/delete")
    public ModelAndView removeRecipeIngredient(@PathVariable Long id,
                                               @PathVariable Long recipeId,
                                               RedirectAttributes flash) {
        try {
            products.removeRecipeIngredient(recipeId);
            flash.addFlashAttribute("success", "Ingrediente removido da ficha técnica.");
        } catch (RuntimeException exception) {
            flash.addFlashAttribute("error", safeMessage(exception));
        }
        return new ModelAndView("redirect:/products");
    }

    @PostMapping("/products/{id}/toggle")
    public ModelAndView toggleProductActive(@PathVariable Long id, @RequestParam boolean active,
                                            RedirectAttributes flash) {
        try {
            products.setActive(id, active);
            flash.addFlashAttribute("success", active ? "Produto ativado." : "Produto desativado.");
        } catch (RuntimeException exception) {
            flash.addFlashAttribute("error", safeMessage(exception));
        }
        return new ModelAndView("redirect:/products");
    }

    @PostMapping("/products/{id}/duplicate")
    public ModelAndView duplicateProduct(@PathVariable Long id, RedirectAttributes flash) {
        try {
            Product copy = products.duplicate(id);
            flash.addFlashAttribute("success", "Produto duplicado com sucesso: " + copy.getName());
        } catch (RuntimeException exception) {
            flash.addFlashAttribute("error", safeMessage(exception));
        }
        return new ModelAndView("redirect:/products");
    }

    @GetMapping(path = "/products/{id}/dependencies", produces = "application/json")
    public ProductDependencyDto getProductDependencies(@PathVariable Long id) {
        ProductService.ProductDependencySummary summary = products.getDependencies(id);
        return new ProductDependencyDto(
                summary.product().getId(),
                summary.product().getName(),
                summary.orderCount(),
                summary.stockCount(),
                summary.recipeIngredientNames(),
                summary.hasDependencies()
        );
    }

    @GetMapping("/products/{id}/delete")
    public ModelAndView confirmDeleteProductPage(@PathVariable Long id) {
        ProductService.ProductDependencySummary deps = products.getDependencies(id);
        ModelAndView mav = new ModelAndView("confirm-delete");
        mav.addObject("itemType", "produto");
        mav.addObject("itemId", id);
        mav.addObject("itemName", deps.product().getName());
        mav.addObject("cancelUrl", "/products");
        mav.addObject("confirmUrl", "/products/" + id + "/delete");
        mav.addObject("hasDependencies", deps.hasDependencies());
        mav.addObject("orderCount", deps.orderCount());
        mav.addObject("stockCount", deps.stockCount());
        mav.addObject("recipeIngredients", deps.recipeIngredientNames());
        return mav;
    }

    public ModelAndView deleteProduct(Long id, Long productId, RedirectAttributes flash) {
        return deleteProduct(id, productId, false, flash);
    }

    @PostMapping(path = {"/products/{id}/delete", "/products/delete"})
    public ModelAndView deleteProduct(@PathVariable(required = false) Long id,
                                      @RequestParam(required = false) Long productId,
                                      @RequestParam(defaultValue = "false") boolean force,
                                      RedirectAttributes flash) {
        Long targetId = id != null ? id : productId;
        try {
            if (targetId == null) {
                throw new IllegalArgumentException("Identificador do produto é obrigatório.");
            }
            if (force) {
                products.delete(targetId, true);
            } else {
                products.delete(targetId);
            }
            flash.addFlashAttribute("success", "Produto excluído com sucesso.");
        } catch (RuntimeException exception) {
            flash.addFlashAttribute("error", safeMessage(exception));
        }
        return new ModelAndView("redirect:/products");
    }

    @DeleteMapping(path = {"/products/{id}", "/products/{id}/delete"})
    public ModelAndView deleteProductViaDelete(@PathVariable Long id,
                                              @RequestParam(defaultValue = "false") boolean force,
                                              RedirectAttributes flash) {
        return deleteProduct(id, null, force, flash);
    }

    @GetMapping(path = "/ingredients/{id}/dependencies", produces = "application/json")
    public IngredientDependencyDto getIngredientDependencies(@PathVariable Long id) {
        IngredientService.IngredientDependencySummary summary = ingredients.getDependencies(id);
        return new IngredientDependencyDto(
                summary.ingredient().getId(),
                summary.ingredient().getName(),
                summary.purchaseCount(),
                summary.productNames(),
                summary.hasDependencies()
        );
    }

    @GetMapping("/ingredients/{id}/delete")
    public ModelAndView confirmDeleteIngredientPage(@PathVariable Long id) {
        IngredientService.IngredientDependencySummary deps = ingredients.getDependencies(id);
        ModelAndView mav = new ModelAndView("confirm-delete");
        mav.addObject("itemType", "ingrediente");
        mav.addObject("itemId", id);
        mav.addObject("itemName", deps.ingredient().getName());
        mav.addObject("cancelUrl", "/ingredients");
        mav.addObject("confirmUrl", "/ingredients/" + id + "/delete");
        mav.addObject("hasDependencies", deps.hasDependencies());
        mav.addObject("purchaseCount", deps.purchaseCount());
        mav.addObject("productNames", deps.productNames());
        return mav;
    }

    @GetMapping("/ingredients")
    public ModelAndView ingredients(@RequestParam(required = false) String start,
                                    @RequestParam(required = false) String end,
                                    @RequestParam(required = false) Long viewId) {
        return buildIngredientsModelAndView(start, end, viewId);
    }

    @GetMapping("/ingredients/{id}")
    public ModelAndView viewIngredient(@PathVariable Long id,
                                       @RequestParam(required = false) String start,
                                       @RequestParam(required = false) String end) {
        return buildIngredientsModelAndView(start, end, id);
    }

    @PostMapping(path = {"/ingredients/save", "/ingredients"})
    public ModelAndView saveIngredient(@RequestParam(required = false) Long id,
                                       @RequestParam String name,
                                       @RequestParam Measure unit, RedirectAttributes flash) {
        try {
            ingredients.saveIngredient(id, name, unit);
            flash.addFlashAttribute("success", id == null ? "Ingrediente criado com sucesso." : "Ingrediente atualizado com sucesso.");
        } catch (RuntimeException exception) {
            flash.addFlashAttribute("error", safeMessage(exception));
        }
        return new ModelAndView("redirect:/ingredients");
    }

    @PostMapping("/ingredients/{id}/edit")
    public ModelAndView editIngredient(@PathVariable Long id,
                                       @RequestParam String name,
                                       @RequestParam Measure unit, RedirectAttributes flash) {
        try {
            ingredients.update(id, name, unit);
            flash.addFlashAttribute("success", "Ingrediente atualizado com sucesso.");
        } catch (RuntimeException exception) {
            flash.addFlashAttribute("error", safeMessage(exception));
        }
        return new ModelAndView("redirect:/ingredients");
    }

    public ModelAndView deleteIngredient(Long id, Long ingredientId, RedirectAttributes flash) {
        return deleteIngredient(id, ingredientId, false, flash);
    }

    @PostMapping(path = {"/ingredients/{id}/delete", "/ingredients/delete"})
    public ModelAndView deleteIngredient(@PathVariable(required = false) Long id,
                                         @RequestParam(required = false) Long ingredientId,
                                         @RequestParam(defaultValue = "false") boolean force,
                                         RedirectAttributes flash) {
        Long targetId = id != null ? id : ingredientId;
        try {
            if (targetId == null) {
                throw new IllegalArgumentException("Identificador do ingrediente é obrigatório.");
            }
            if (force) {
                ingredients.delete(targetId, true);
            } else {
                ingredients.delete(targetId);
            }
            flash.addFlashAttribute("success", "Ingrediente excluído com sucesso.");
        } catch (RuntimeException exception) {
            flash.addFlashAttribute("error", safeMessage(exception));
        }
        return new ModelAndView("redirect:/ingredients");
    }

    @DeleteMapping(path = {"/ingredients/{id}", "/ingredients/{id}/delete"})
    public ModelAndView deleteIngredientViaDelete(@PathVariable Long id,
                                                 @RequestParam(defaultValue = "false") boolean force,
                                                 RedirectAttributes flash) {
        return deleteIngredient(id, null, force, flash);
    }

    @PostMapping("/ingredients/purchases")
    public ModelAndView registerPurchase(@RequestParam String date, @RequestParam Long ingredientId,
                                         @RequestParam BigDecimal quantity,
                                         @RequestParam(required = false) Measure unit,
                                         @RequestParam(required = false) BigDecimal unitPrice,
                                         @RequestParam(required = false) BigDecimal totalCost,
                                         RedirectAttributes flash) {
        try {
            ingredients.registerPurchase(LocalDate.parse(date), ingredientId, quantity, unit, unitPrice, totalCost);
            flash.addFlashAttribute("success", "Compra registrada.");
        } catch (RuntimeException exception) {
            flash.addFlashAttribute("error", safeMessage(exception));
        }
        return new ModelAndView("redirect:/ingredients");
    }

    private ModelAndView buildIngredientsModelAndView(String start, String end, Long viewId) {
        YearMonth current = YearMonth.from(LocalDate.now(ZONE));
        LocalDate startDate = parseDate(start, current.atDay(1));
        LocalDate endDate = parseDate(end, current.atEndOfMonth());
        List<IngredientPurchase> purchases = ingredients.purchases(startDate, endDate);
        BigDecimal total = purchases.stream().map(IngredientPurchase::calculateTotalCost)
                .reduce(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP), BigDecimal::add);
        ModelAndView modelAndView = new ModelAndView("ingredients");
        modelAndView.addObject("ingredients", ingredients.all());
        modelAndView.addObject("measures", Measure.values());
        modelAndView.addObject("purchases", purchases);
        modelAndView.addObject("startDate", startDate);
        modelAndView.addObject("endDate", endDate);
        modelAndView.addObject("today", LocalDate.now(ZONE));
        modelAndView.addObject("purchaseTotal", total);

        if (viewId != null) {
            try {
                IngredientService.IngredientSummary summary = ingredients.getSummary(viewId);
                modelAndView.addObject("selectedIngredient", summary.ingredient());
                modelAndView.addObject("selectedSummary", summary);
            } catch (RuntimeException exception) {
                modelAndView.addObject("error", safeMessage(exception));
            }
        }
        return modelAndView;
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

    public record StockLine(Product product, int initial, int sold, int available, boolean explicit, int carriedOver) {}

    public record ProductDependencyDto(
            Long id,
            String name,
            long orderCount,
            long stockCount,
            List<String> recipeIngredients,
            boolean hasDependencies
    ) {}

    public record IngredientDependencyDto(
            Long id,
            String name,
            long purchaseCount,
            List<String> productNames,
            boolean hasDependencies
    ) {}
}
