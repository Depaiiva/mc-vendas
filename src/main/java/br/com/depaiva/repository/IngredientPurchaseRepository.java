package br.com.depaiva.repository;

import br.com.depaiva.domain.IngredientPurchase;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface IngredientPurchaseRepository extends JpaRepository<IngredientPurchase, Long> {
    List<IngredientPurchase> findAllByDateBetweenOrderByDateDesc(LocalDate start, LocalDate end);
    boolean existsByIngredientId(Long ingredientId);
    List<IngredientPurchase> findAllByIngredientIdOrderByDateDesc(Long ingredientId);
    long countByIngredientId(Long ingredientId);

    @Modifying
    @Query("DELETE FROM IngredientPurchase ip WHERE ip.ingredient.id = :ingredientId")
    void deleteAllByIngredientId(@Param("ingredientId") Long ingredientId);
}
