package br.com.depaiva.service;

import br.com.depaiva.domain.Product;
import br.com.depaiva.domain.Type;
import br.com.depaiva.repository.ProductRepository;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProductService {
    private final ProductRepository products;

    public ProductService(ProductRepository products) {
        this.products = products;
    }

    public List<Product> all() {
        return products.findAllByOrderByNameAsc();
    }

    public List<Product> active() {
        return products.findAllByActiveTrueOrderByNameAsc();
    }

    @Transactional
    public Product save(Long id, String name, BigDecimal price, Type type, String description, boolean active) {
        Product product = id == null
                ? new Product(name, price, type, description)
                : products.findById(id).orElseThrow(() -> new IllegalArgumentException("Produto não encontrado."));
        if (id == null) {
            if (!active) product.deactivate();
        } else {
            product.updateDetails(name, type, description);
            product.changePrice(price);
            if (active) product.activate(); else product.deactivate();
        }
        return products.save(product);
    }

    @Transactional
    public void setActive(Long id, boolean active) {
        Product product = products.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Produto não encontrado."));
        if (active) product.activate(); else product.deactivate();
        products.save(product);
    }
}
