package br.com.depaiva.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDate;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(
        name = "daily_stocks",
        uniqueConstraints = @UniqueConstraint(name = "uq_daily_stock_product_date", columnNames = {"product_id", "date"}))
public class DailyStock {
    @Id
    @JdbcTypeCode(SqlTypes.INTEGER)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, columnDefinition = "TEXT")
    private LocalDate date;

    @Column(name = "initial_quantity", nullable = false)
    private int initialQuantity;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    protected DailyStock() {}

    public DailyStock(Product product, LocalDate date, int initialQuantity) {
        if (product == null || date == null || initialQuantity < 0) {
            throw new IllegalArgumentException("Informe produto, data e estoque inicial igual ou maior que zero.");
        }
        this.product = product;
        this.date = date;
        this.initialQuantity = initialQuantity;
    }

    public void changeInitialQuantity(int nextQuantity) {
        if (nextQuantity < 0) {
            throw new IllegalArgumentException("O estoque inicial não pode ser negativo.");
        }
        initialQuantity = nextQuantity;
    }

    public Long getId() { return id; }
    public LocalDate getDate() { return date; }
    public int getInitialQuantity() { return initialQuantity; }
    public Product getProduct() { return product; }
}
