package br.com.depaiva.repository;

import br.com.depaiva.domain.DailyStock;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DailyStockRepository extends JpaRepository<DailyStock, Long> {
    Optional<DailyStock> findByProductIdAndDate(Long productId, LocalDate date);
    List<DailyStock> findAllByDate(LocalDate date);
}
