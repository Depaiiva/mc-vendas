package br.com.depaiva.repository;

import br.com.depaiva.domain.DailyStock;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DailyStockRepository extends JpaRepository<DailyStock, Long> {
    Optional<DailyStock> findByProductIdAndDate(Long productId, LocalDate date);
    Optional<DailyStock> findFirstByProductIdAndDateLessThanOrderByDateDesc(Long productId, LocalDate date);
    void deleteByProductIdAndDate(Long productId, LocalDate date);
    List<DailyStock> findAllByDate(LocalDate date);
    boolean existsByProductId(Long productId);
    long countByProductId(Long productId);

    @Modifying
    @Query("DELETE FROM DailyStock ds WHERE ds.product.id = :productId")
    void deleteAllByProductId(@Param("productId") Long productId);
}
