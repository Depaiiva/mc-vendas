package br.com.depaiva.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.math.RoundingMode;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.NumericBooleanConverter;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "products")
public class Product {
    @Id
    @JdbcTypeCode(SqlTypes.INTEGER)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Convert(converter = MoneyConverter.class)
    @Column(nullable = false, columnDefinition = "TEXT")
    private BigDecimal price;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Type type;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    @Convert(converter = NumericBooleanConverter.class)
    private boolean active = true;

    protected Product() {}

    public Product(String name, BigDecimal price, Type type, String description) {
        this.name = name;
        changePrice(price);
        this.type = type;
        this.description = description;
        this.active = true;
    }

    public void activate() {
        active = true;
    }

    public void deactivate() {
        active = false;
    }

    public void changePrice(BigDecimal nextPrice) {
        if (nextPrice == null || nextPrice.signum() <= 0) {
            throw new IllegalArgumentException("O preço deve ser maior que zero.");
        }
        price = nextPrice.setScale(2, RoundingMode.HALF_UP);
    }

    public void updateDetails(String nextName, Type nextType, String nextDescription) {
        if (nextName == null || nextName.isBlank() || nextType == null) {
            throw new IllegalArgumentException("Informe o nome e o tipo do produto.");
        }
        name = nextName.trim();
        type = nextType;
        description = nextDescription == null || nextDescription.isBlank() ? null : nextDescription.trim();
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public BigDecimal getPrice() { return price; }
    public Type getType() { return type; }
    public String getDescription() { return description; }
    public boolean isActive() { return active; }
}
