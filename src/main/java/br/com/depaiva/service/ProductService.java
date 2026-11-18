package br.com.depaiva.service;

import br.com.depaiva.domain.Ingredient;
import br.com.depaiva.domain.Order;
import br.com.depaiva.domain.OrderItem;
import br.com.depaiva.domain.Product;
import br.com.depaiva.domain.ProductIngredient;
import br.com.depaiva.domain.Type;
import br.com.depaiva.repository.DailyStockRepository;
import br.com.depaiva.repository.IngredientRepository;
import br.com.depaiva.repository.OrderItemRepository;
import br.com.depaiva.repository.OrderRepository;
import br.com.depaiva.repository.ProductIngredientRepository;
import br.com.depaiva.repository.ProductRepository;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProductService {
    private final ProductRepository products;
    private final DailyStockRepository dailyStocks;
    private final OrderItemRepository orderItems;
    private final ProductIngredientRepository productIngredients;
    private final IngredientRepository ingredientRepository;
    private final OrderRepository orderRepository;

    public ProductService(ProductRepository products, DailyStockRepository dailyStocks, OrderItemRepository orderItems) {
        this(products, dailyStocks, orderItems, null, null, null);
    }

    public ProductService(ProductRepository products, DailyStockRepository dailyStocks, OrderItemRepository orderItems,
                          ProductIngredientRepository productIngredients, IngredientRepository ingredientRepository) {
        this(products, dailyStocks, orderItems, productIngredients, ingredientRepository, null);
    }

    @Autowired
    public ProductService(ProductRepository products, DailyStockRepository dailyStocks, OrderItemRepository orderItems,
                          ProductIngredientRepository productIngredients, IngredientRepository ingredientRepository,
                          OrderRepository orderRepository) {
        this.products = products;
        this.dailyStocks = dailyStocks;
        this.orderItems = orderItems;
        this.productIngredients = productIngredients;
        this.ingredientRepository = ingredientRepository;
        this.orderRepository = orderRepository;
    }

    public List<Product> all() {
        return products.findAllByOrderByNameAsc();
    }

    public List<Product> active() {
        return products.findAllByActiveTrueOrderByNameAsc();
    }

    public Product findById(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("Identificador do produto é obrigatório.");
        }
        return products.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Produto não encontrado."));
    }

    @Transactional
    public Product create(String name, BigDecimal price, Type type, String description, boolean active) {
        Product product = new Product(name, price, type, description);
        if (!active) {
            product.deactivate();
        }
        return products.save(product);
    }

    @Transactional
    public Product update(Long id, String name, BigDecimal price, Type type, String description, boolean active) {
        Product product = findById(id);
        product.updateDetails(name, type, description);
        product.changePrice(price);
        if (active) {
            product.activate();
        } else {
            product.deactivate();
        }
        return products.save(product);
    }

    @Transactional
    public Product duplicate(Long id) {
        Product original = findById(id);
        Product copy = new Product(original.getName() + " (cópia)", original.getPrice(), original.getType(), original.getDescription());
        Product savedCopy = products.save(copy);
        if (productIngredients != null) {
            List<ProductIngredient> originalRecipe = productIngredients.findAllByProductId(id);
            for (ProductIngredient pi : originalRecipe) {
                productIngredients.save(new ProductIngredient(savedCopy, pi.getIngredient(), pi.getQuantity(), pi.getRecipeYield()));
            }
        }
        return savedCopy;
    }

    public ProductDependencySummary getDependencies(Long id) {
        Product product = findById(id);
        long orderCount = orderItems != null ? orderItems.countByProductId(id) : 0;
        long stockCount = dailyStocks != null ? dailyStocks.countByProductId(id) : 0;
        List<String> recipeIngredients = productIngredients != null
                ? productIngredients.findAllByProductId(id).stream().map(pi -> pi.getIngredient().getName()).toList()
                : List.of();
        return new ProductDependencySummary(product, orderCount, stockCount, recipeIngredients);
    }

    @Transactional
    public void delete(Long id) {
        delete(id, false);
    }

    @Transactional
    public void delete(Long id, boolean force) {
        Product product = findById(id);
        ProductDependencySummary deps = getDependencies(id);

        boolean hasOrders = (orderItems != null && orderItems.existsByProductId(id)) || deps.orderCount() > 0;
        boolean hasStocks = (dailyStocks != null && dailyStocks.existsByProductId(id)) || deps.stockCount() > 0;
        boolean hasRecipe = !deps.recipeIngredientNames().isEmpty();

        if (!force && (hasOrders || hasStocks || hasRecipe)) {
            List<String> parts = new ArrayList<>();
            if (hasOrders) parts.add((deps.orderCount() > 0 ? deps.orderCount() : 1) + " vendas registradas");
            if (hasStocks) parts.add((deps.stockCount() > 0 ? deps.stockCount() : 1) + " registros de estoque vinculados");
            if (hasRecipe) parts.add("ingredientes na ficha técnica (" + String.join(", ", deps.recipeIngredientNames()) + ")");
            throw new IllegalStateException("Não é possível excluir o produto '" + product.getName()
                    + "' pois ele possui " + String.join(" e ", parts)
                    + ". Confirme a exclusão para removê-lo junto com seus vínculos.");
        }

        if (orderItems != null) {
            List<OrderItem> items = orderItems.findAllByProductId(id);
            for (OrderItem item : items) {
                Order order = item.getOrder();
                if (order != null && orderRepository != null) {
                    if (order.getItems().size() <= 1) {
                        orderRepository.delete(order);
                    } else {
                        order.getItems().remove(item);
                        orderItems.delete(item);
                    }
                } else {
                    orderItems.delete(item);
                }
            }
        }
        if (dailyStocks != null) {
            dailyStocks.deleteAllByProductId(id);
        }
        if (productIngredients != null) {
            productIngredients.deleteAllByProductId(id);
        }
        products.delete(product);
    }

    @Transactional
    public Product save(Long id, String name, BigDecimal price, Type type, String description, boolean active) {
        return id == null
                ? create(name, price, type, description, active)
                : update(id, name, price, type, description, active);
    }

    @Transactional
    public void setActive(Long id, boolean active) {
        Product product = findById(id);
        if (active) {
            product.activate();
        } else {
            product.deactivate();
        }
        products.save(product);
    }

    public List<ProductIngredient> getRecipe(Long productId) {
        if (productIngredients == null) return List.of();
        return productIngredients.findAllByProductId(productId);
    }

    @Transactional
    public ProductIngredient addOrUpdateRecipeIngredient(Long productId, Long ingredientId, BigDecimal quantity, int recipeYield) {
        if (productIngredients == null || ingredientRepository == null) {
            throw new IllegalStateException("Repositórios não configurados para gerenciar fichas técnicas.");
        }
        Product product = findById(productId);
        Ingredient ingredient = ingredientRepository.findById(ingredientId)
                .orElseThrow(() -> new IllegalArgumentException("Ingrediente não encontrado."));
        List<ProductIngredient> existing = productIngredients.findAllByProductId(productId);
        for (ProductIngredient pi : existing) {
            if (pi.getIngredient().getId().equals(ingredientId)) {
                pi.update(quantity, recipeYield);
                return productIngredients.save(pi);
            }
        }
        return productIngredients.save(new ProductIngredient(product, ingredient, quantity, recipeYield));
    }

    @Transactional
    public void removeRecipeIngredient(Long productIngredientId) {
        if (productIngredients == null) return;
        productIngredients.deleteById(productIngredientId);
    }

    public record ProductDependencySummary(
            Product product,
            long orderCount,
            long stockCount,
            List<String> recipeIngredientNames
    ) {
        public boolean hasDependencies() {
            return orderCount > 0 || stockCount > 0 || !recipeIngredientNames.isEmpty();
        }
    }
}
