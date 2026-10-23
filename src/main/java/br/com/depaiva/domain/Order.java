package br.com.depaiva.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "orders")
public class Order {
    @Id
    @JdbcTypeCode(SqlTypes.INTEGER)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, columnDefinition = "TEXT")
    private LocalDateTime date;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status;

    @Column(name = "customer_name")
    private String customerName;

    @Column(name = "customer_contact")
    private String customerContact;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Channel channel;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @OrderBy("id ASC")
    private List<OrderItem> items = new ArrayList<>();

    protected Order() {}

    private Order(LocalDateTime date, Status status, String customerName, String customerContact, Channel channel) {
        this.date = Objects.requireNonNull(date);
        this.status = Objects.requireNonNull(status);
        this.customerName = customerName;
        this.customerContact = customerContact;
        this.channel = Objects.requireNonNull(channel);
    }

    public static Order inPersonSale(LocalDateTime date, String customerName, String customerContact) {
        String name = customerName == null || customerName.isBlank() ? "Presencial" : customerName.trim();
        String contact = customerContact == null || customerContact.isBlank() ? null : customerContact.trim();
        return new Order(date, Status.DELIVERED, name, contact, Channel.IN_PERSON);
    }

    public static Order create(LocalDateTime date, Status status, String customerName,
                               String customerContact, Channel channel) {
        return new Order(date, status, customerName, customerContact, channel);
    }

    public void addItem(Product product, int quantity) {
        if (product == null || !product.isActive()) {
            throw new IllegalArgumentException("O produto precisa estar ativo para ser vendido.");
        }
        if (quantity <= 0) {
            throw new IllegalArgumentException("A quantidade deve ser maior que zero.");
        }
        for (OrderItem existing : items) {
            if (sameProduct(existing.getProduct(), product)) {
                existing.increaseQuantity(quantity);
                return;
            }
        }
        OrderItem item = new OrderItem(product, quantity);
        item.setOrder(this);
        items.add(item);
    }

    private boolean sameProduct(Product left, Product right) {
        return left == right || (left.getId() != null && left.getId().equals(right.getId()));
    }

    public BigDecimal calculateTotal() {
        return items.stream()
                .map(OrderItem::calculateSubtotal)
                .reduce(BigDecimal.ZERO.setScale(2), BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }

    public void confirm() {
        transitionTo(Status.CONFIRMED);
    }

    public void deliver() {
        transitionTo(Status.DELIVERED);
    }

    public void cancel() {
        transitionTo(Status.CANCELED);
    }

    private void transitionTo(Status next) {
        if (!status.canTransitionTo(next)) {
            throw new IllegalStateException("Transição de " + status + " para " + next + " não é permitida.");
        }
        status = next;
    }

    @PrePersist
    private void requireItems() {
        if (items.isEmpty()) {
            throw new IllegalStateException("Uma venda precisa ter pelo menos um item.");
        }
    }

    @Transient
    public boolean canCancel() {
        return status == Status.PENDING || status == Status.CONFIRMED || status == Status.DELIVERED;
    }

    public Long getId() { return id; }
    public LocalDateTime getDate() { return date; }
    public Status getStatus() { return status; }
    public String getCustomerName() { return customerName; }
    public String getCustomerContact() { return customerContact; }
    public Channel getChannel() { return channel; }
    public List<OrderItem> getItems() { return items; }
}
