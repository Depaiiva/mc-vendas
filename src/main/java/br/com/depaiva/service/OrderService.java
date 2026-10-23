package br.com.depaiva.service;

import br.com.depaiva.domain.Order;
import br.com.depaiva.domain.Product;
import br.com.depaiva.repository.OrderRepository;
import br.com.depaiva.repository.ProductRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderService {
    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final StockService stockService;
    private final ReentrantLock saleLock = new ReentrantLock(true);

    public OrderService(OrderRepository orderRepository, ProductRepository productRepository, StockService stockService) {
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
        this.stockService = stockService;
    }

    @Transactional
    public Order registerInPersonSale(LocalDateTime date, String customerName, String customerContact,
                                     Map<Long, Integer> quantities) {
        saleLock.lock();
        try {
            Order order = Order.inPersonSale(date, customerName, customerContact);
            for (Map.Entry<Long, Integer> entry : quantities.entrySet()) {
                if (entry.getValue() == null || entry.getValue() <= 0) {
                    continue;
                }
                Product product = productRepository.findById(entry.getKey())
                        .orElseThrow(() -> new IllegalArgumentException("Um dos produtos selecionados não existe."));
                order.addItem(product, entry.getValue());
            }
            if (order.getItems().isEmpty()) {
                throw new IllegalArgumentException("Escolha pelo menos um produto para registrar a venda.");
            }
            boolean canFulfill = stockService.canFulfill(order);
            for (var item : order.getItems()) {
                int available = stockService.getAvailable(item.getProduct(), date.toLocalDate());
                if (available < item.getQuantity()) {
                    throw new InsufficientStockException(
                            "Estoque insuficiente para " + item.getProduct().getName()
                                    + ". Disponível: " + available + ".");
                }
            }
            if (!canFulfill) {
                throw new InsufficientStockException("Estoque insuficiente para concluir esta venda.");
            }
            return orderRepository.save(order);
        } finally {
            saleLock.unlock();
        }
    }

    @Transactional(readOnly = true)
    public List<Order> findForDate(LocalDateTime start, LocalDateTime end) {
        return orderRepository.findAllByDateBetweenOrderByDateDesc(start, end);
    }

    @Transactional
    public void cancel(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Venda não encontrada."));
        order.cancel();
        orderRepository.save(order);
    }
}
