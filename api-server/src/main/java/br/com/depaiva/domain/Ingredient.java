package br.com.depaiva.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "ingredients")
public class Ingredient {
    @Id
    @JdbcTypeCode(SqlTypes.INTEGER)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Measure unit;

    protected Ingredient() {}

    public Ingredient(String name, Measure unit) {
        update(name, unit);
    }

    public void update(String nextName, Measure nextUnit) {
        if (nextName == null || nextName.isBlank() || nextUnit == null) {
            throw new IllegalArgumentException("Informe o nome e a unidade do ingrediente.");
        }
        name = nextName.trim();
        unit = nextUnit;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public Measure getUnit() { return unit; }
}
