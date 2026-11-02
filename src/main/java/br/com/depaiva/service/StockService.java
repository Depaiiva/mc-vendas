package br.com.depaiva.service;

import br.com.depaiva.domain.DailyStock;
import br.com.depaiva.domain.Order;
import br.com.depaiva.domain.OrderItem;
import br.com.depaiva.domain.Product;
import br.com.depaiva.domain.Status;
import br.com.depaiva.repository.DailyStockRepository;
import br.com.depaiva.repository.OrderRepository;
import br.com.depaiva.repository.ProductRepository;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StockService {
    private static final Logger log = LoggerFactory.getLogger(StockService.class);
    private static final List<Status> STOCK_STATUSES = List.of(Status.CONFIRMED, Status.DELIVERED);
    private final DailyStockRepository stockRepository;
    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;

    public StockService(DailyStockRepository stockRepository, OrderRepository orderRepository,
                        ProductRepository productRepository) {
        this.stockRepository = stockRepository;
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
    }

    @Transactional
    public DailyStock registerInitialStock(Long productId, LocalDate date, int quantity) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("O produto selecionado não existe."));
        if (stockRepository.findByProductIdAndDate(productId, date).isPresent()) {
            throw new IllegalArgumentException("O estoque inicial deste produto já foi registrado para essa data.");
        }
        return stockRepository.save(new DailyStock(product, date, quantity));
    }

    @Transactional
    public DailyStock editInitialStock(Long productId, LocalDate date, int quantity) {
        if (quantity < 0) {
            throw new IllegalArgumentException("O estoque inicial não pode ser negativo.");
        }
        int soldOnDate = getSoldQuantity(productId, date);
        if (quantity < soldOnDate) {
            Product product = productRepository.findById(productId)
                    .orElseThrow(() -> new IllegalArgumentException("O produto selecionado não existe."));
            throw new IllegalArgumentException("O estoque de " + product.getName()
                    + " (" + quantity + ") não pode ser menor que o total já vendido nesta data (" + soldOnDate + ").");
        }
        DailyStock stock = stockRepository.findByProductIdAndDate(productId, date)
                .orElseGet(() -> {
                    Product product = productRepository.findById(productId)
                            .orElseThrow(() -> new IllegalArgumentException("O produto selecionado não existe."));
                    return new DailyStock(product, date, quantity);
                });
        stock.changeInitialQuantity(quantity);
        return stockRepository.save(stock);
    }

    @Transactional
    public void resetStock(Long productId, LocalDate date) {
        stockRepository.deleteByProductIdAndDate(productId, date);
    }

    @Transactional(readOnly = true)
    public boolean hasExplicitStock(Long productId, LocalDate date) {
        return stockRepository.findByProductIdAndDate(productId, date).isPresent();
    }

    @Transactional(readOnly = true)
    public int getInitialQuantity(Long productId, LocalDate date) {
        Optional<DailyStock> stock = stockRepository.findByProductIdAndDate(productId, date);
        if (stock.isPresent()) {
            return stock.get().getInitialQuantity();
        }
        return getCarriedOverStock(productId, date);
    }

    @Transactional(readOnly = true)
    public int getCarriedOverStock(Long productId, LocalDate date) {
        Optional<DailyStock> anchorRecord = stockRepository
                .findFirstByProductIdAndDateLessThanOrderByDateDesc(productId, date);
        if (anchorRecord.isEmpty()) {
            return 0;
        }
        DailyStock anchor = anchorRecord.get();
        int soldInBetween = getSoldQuantityBetween(productId, anchor.getDate(), date.minusDays(1));
        return Math.max(0, anchor.getInitialQuantity() - soldInBetween);
    }

    @Transactional(readOnly = true)
    public int getSoldQuantity(Long productId, LocalDate date) {
        return getSoldQuantityBetween(productId, date, date);
    }

    @Transactional(readOnly = true)
    public int getSoldQuantityBetween(Long productId, LocalDate startDate, LocalDate endDate) {
        if (endDate.isBefore(startDate)) {
            return 0;
        }
        return orderRepository.findAllByDateBetweenAndStatusIn(
                        startDate.atStartOfDay(), endDate.atTime(LocalTime.MAX), STOCK_STATUSES)
                .stream()
                .flatMap(order -> order.getItems().stream())
                .filter(item -> productId.equals(item.getProduct().getId()))
                .mapToInt(OrderItem::getQuantity)
                .sum();
    }

    @Transactional(readOnly = true)
    public int getAvailable(Product product, LocalDate date) {
        int available = getInitialQuantity(product.getId(), date) - getSoldQuantity(product.getId(), date);
        if (available < 0) {
            log.error("Estoque negativo para o produto {} em {}: {}", product.getId(), date, available);
        }
        return available;
    }

    @Transactional(readOnly = true)
    public boolean canFulfill(Order order) {
        LocalDate saleDate = order.getDate().toLocalDate();
        return order.getItems().stream()
                .allMatch(item -> getAvailable(item.getProduct(), saleDate) >= item.getQuantity());
    }
}
