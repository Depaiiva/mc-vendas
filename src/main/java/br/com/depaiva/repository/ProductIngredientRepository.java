package br.com.depaiva.repository;

import br.com.depaiva.domain.ProductIngredient;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductIngredientRepository extends JpaRepository<ProductIngredient, Long> {
    List<ProductIngredient> findAllByProductId(Long productId);
    List<ProductIngredient> findAllByIngredientId(Long ingredientId);
    boolean existsByIngredientId(Long ingredientId);
    boolean existsByProductId(Long productId);
    long countByIngredientId(Long ingredientId);
    long countByProductId(Long productId);

    @Modifying
    @Query("DELETE FROM ProductIngredient pi WHERE pi.product.id = :productId")
    void deleteAllByProductId(@Param("productId") Long productId);

    @Modifying
    @Query("DELETE FROM ProductIngredient pi WHERE pi.ingredient.id = :ingredientId")
    void deleteAllByIngredientId(@Param("ingredientId") Long ingredientId);
}
