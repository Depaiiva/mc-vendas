package br.com.depaiva.repository;

import br.com.depaiva.domain.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {
    boolean existsByProductId(Long productId);
    long countByProductId(Long productId);
}
