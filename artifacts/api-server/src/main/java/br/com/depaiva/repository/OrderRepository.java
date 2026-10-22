package br.com.depaiva.repository;

import br.com.depaiva.domain.Order;
import br.com.depaiva.domain.Status;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findAllByDateBetweenOrderByDateDesc(LocalDateTime start, LocalDateTime end);
    List<Order> findAllByDateBetweenAndStatusIn(LocalDateTime start, LocalDateTime end, Collection<Status> statuses);
}
