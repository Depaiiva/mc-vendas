package br.com.depaiva.service;

import br.com.depaiva.domain.Product;
import br.com.depaiva.domain.Type;
import br.com.depaiva.repository.DailyStockRepository;
import br.com.depaiva.repository.OrderItemRepository;
import br.com.depaiva.repository.ProductRepository;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProductService {
    private final ProductRepository products;
    private final DailyStockRepository dailyStocks;
    private final OrderItemRepository orderItems;

    public ProductService(ProductRepository products, DailyStockRepository dailyStocks, OrderItemRepository orderItems) {
        this.products = products;
        this.dailyStocks = dailyStocks;
        this.orderItems = orderItems;
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
        return products.save(copy);
    }

    @Transactional
    public void delete(Long id) {
        Product product = findById(id);
        if (orderItems.existsByProductId(id)) {
            throw new IllegalStateException("Não é possível excluir o produto '" + product.getName()
                    + "' pois ele possui vendas registradas. Caso não deseje mais vendê-lo, desative-o.");
        }
        if (dailyStocks.existsByProductId(id)) {
            throw new IllegalStateException("Não é possível excluir o produto '" + product.getName()
                    + "' pois ele possui registros de estoque vinculados. Caso não deseje mais vendê-lo, desative-o.");
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
}
