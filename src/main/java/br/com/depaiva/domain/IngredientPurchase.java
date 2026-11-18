package br.com.depaiva.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "ingredient_purchases")
public class IngredientPurchase {
    @Id
    @JdbcTypeCode(SqlTypes.INTEGER)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, columnDefinition = "TEXT")
    private LocalDate date;

    @Convert(converter = QuantityConverter.class)
    @Column(nullable = false, columnDefinition = "TEXT")
    private BigDecimal quantity;

    @Convert(converter = MoneyConverter.class)
    @Column(name = "unit_price", nullable = false, columnDefinition = "TEXT")
    private BigDecimal unitPrice;

    @Convert(converter = MoneyConverter.class)
    @Column(name = "total_cost", columnDefinition = "TEXT")
    private BigDecimal totalCost;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Origin origin;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "ingredient_id", nullable = false)
    private Ingredient ingredient;

    protected IngredientPurchase() {}

    public IngredientPurchase(LocalDate date, BigDecimal quantity, BigDecimal unitPrice,
                              Origin origin, Ingredient ingredient) {
        this(date, quantity, unitPrice, null, origin, ingredient);
    }

    public IngredientPurchase(LocalDate date, BigDecimal quantity, BigDecimal unitPrice,
                              BigDecimal totalCost, Origin origin, Ingredient ingredient) {
        if (date == null || quantity == null || quantity.signum() <= 0 || ingredient == null) {
            throw new IllegalArgumentException("Data, quantidade e ingrediente são obrigatórios.");
        }
        this.date = date;
        this.quantity = quantity.setScale(3, RoundingMode.HALF_UP);
        this.origin = origin == null ? Origin.MANUAL : origin;
        this.ingredient = ingredient;

        if (totalCost != null && totalCost.signum() > 0) {
            this.totalCost = totalCost.setScale(2, RoundingMode.HALF_UP);
            this.unitPrice = unitPrice != null ? unitPrice.setScale(4, RoundingMode.HALF_UP)
                    : this.totalCost.divide(this.quantity, 4, RoundingMode.HALF_UP);
        } else if (unitPrice != null && unitPrice.signum() > 0) {
            this.unitPrice = unitPrice.setScale(4, RoundingMode.HALF_UP);
            this.totalCost = this.quantity.multiply(this.unitPrice).setScale(2, RoundingMode.HALF_UP);
        } else {
            throw new IllegalArgumentException("Informe o valor total pago ou o preço unitário.");
        }
    }

    public BigDecimal calculateTotalCost() {
        return getTotalCost();
    }

    public BigDecimal getTotalCost() {
        if (totalCost != null) {
            return totalCost;
        }
        return quantity.multiply(unitPrice).setScale(2, RoundingMode.HALF_UP);
    }

    public Long getId() { return id; }
    public LocalDate getDate() { return date; }
    public BigDecimal getQuantity() { return quantity; }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public Origin getOrigin() { return origin; }
    public Ingredient getIngredient() { return ingredient; }
}
