package br.com.depaiva;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import br.com.depaiva.service.MetricsService;
import br.com.depaiva.service.OrderService;
import br.com.depaiva.service.ProductService;
import br.com.depaiva.service.cost.ProductCostService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class CampusSalesApplicationTest {

    @Autowired
    private MetricsService metricsService;

    @Autowired
    private ProductCostService productCostService;

    @Autowired
    private ProductService productService;

    @Autowired
    private OrderService orderService;

    @Test
    void contextLoadsAndServicesAreInjected() {
        assertNotNull(metricsService);
        assertNotNull(productCostService);
        assertNotNull(productService);
        assertNotNull(orderService);
    }
}
