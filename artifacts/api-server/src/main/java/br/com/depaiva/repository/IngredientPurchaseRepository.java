package br.com.depaiva.repository;

import br.com.depaiva.domain.IngredientPurchase;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IngredientPurchaseRepository extends JpaRepository<IngredientPurchase, Long> {
    List<IngredientPurchase> findAllByDateBetweenOrderByDateDesc(LocalDate start, LocalDate end);
}
