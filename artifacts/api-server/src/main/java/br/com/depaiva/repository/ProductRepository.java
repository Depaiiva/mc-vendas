package br.com.depaiva.repository;

import br.com.depaiva.domain.Product;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, Long> {
    List<Product> findAllByOrderByNameAsc();
    List<Product> findAllByActiveTrueOrderByNameAsc();
}
