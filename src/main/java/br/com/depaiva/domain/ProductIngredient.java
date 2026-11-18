package br.com.depaiva.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.math.RoundingMode;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "product_ingredients")
public class ProductIngredient {
    @Id
    @JdbcTypeCode(SqlTypes.INTEGER)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "ingredient_id", nullable = false)
    private Ingredient ingredient;

    @Convert(converter = QuantityConverter.class)
    @Column(nullable = false, columnDefinition = "TEXT")
    private BigDecimal quantity;

    @Column(name = "recipe_yield", nullable = false)
    private int recipeYield;

    protected ProductIngredient() {}

    public ProductIngredient(Product product, Ingredient ingredient, BigDecimal quantity, int recipeYield) {
        if (product == null || ingredient == null) {
            throw new IllegalArgumentException("Produto e ingrediente são obrigatórios.");
        }
        if (quantity == null || quantity.signum() <= 0) {
            throw new IllegalArgumentException("A quantidade deve ser maior que zero.");
        }
        if (recipeYield <= 0) {
            throw new IllegalArgumentException("O rendimento da receita deve ser maior que zero.");
        }
        this.product = product;
        this.ingredient = ingredient;
        this.quantity = quantity.setScale(3, RoundingMode.HALF_UP);
        this.recipeYield = recipeYield;
    }

    public BigDecimal getQuantityPerUnit() {
        return quantity.divide(BigDecimal.valueOf(recipeYield), 6, RoundingMode.HALF_UP);
    }

    public void update(BigDecimal nextQuantity, int nextRecipeYield) {
        if (nextQuantity == null || nextQuantity.signum() <= 0) {
            throw new IllegalArgumentException("A quantidade deve ser maior que zero.");
        }
        if (nextRecipeYield <= 0) {
            throw new IllegalArgumentException("O rendimento da receita deve ser maior que zero.");
        }
        this.quantity = nextQuantity.setScale(3, RoundingMode.HALF_UP);
        this.recipeYield = nextRecipeYield;
    }

    public Long getId() { return id; }
    public Product getProduct() { return product; }
    public Ingredient getIngredient() { return ingredient; }
    public BigDecimal getQuantity() { return quantity; }
    public int getRecipeYield() { return recipeYield; }
}
