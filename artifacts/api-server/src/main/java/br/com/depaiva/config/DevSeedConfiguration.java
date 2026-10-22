package br.com.depaiva.config;

import br.com.depaiva.domain.DailyStock;
import br.com.depaiva.domain.Ingredient;
import br.com.depaiva.domain.Measure;
import br.com.depaiva.domain.Product;
import br.com.depaiva.domain.Type;
import br.com.depaiva.repository.DailyStockRepository;
import br.com.depaiva.repository.IngredientRepository;
import br.com.depaiva.repository.ProductRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("dev")
public class DevSeedConfiguration {
    @Bean
    CommandLineRunner seedDevelopmentData(ProductRepository products, DailyStockRepository stock,
                                         IngredientRepository ingredients) {
        return args -> {
            if (products.count() == 0) {
                LocalDate today = LocalDate.now(ZoneId.of("America/Sao_Paulo"));
                Product bolo = products.save(new Product("Bolo gelado", new BigDecimal("9.00"),
                        Type.CHILLED_CAKE, "Fatia individual"));
                Product sanduiche = products.save(new Product("Sanduíche natural", new BigDecimal("12.00"),
                        Type.SANDWICH, "Frango com creme"));
                Product pote = products.save(new Product("Bolo no pote", new BigDecimal("10.00"),
                        Type.CAKE_IN_A_JAR, "Chocolate"));
                stock.save(new DailyStock(bolo, today, 12));
                stock.save(new DailyStock(sanduiche, today, 10));
                stock.save(new DailyStock(pote, today, 8));
            }
            if (ingredients.count() == 0) {
                ingredients.save(new Ingredient("Farinha de trigo", Measure.KILO_GRAMS));
                ingredients.save(new Ingredient("Leite condensado", Measure.GRAMS));
                ingredients.save(new Ingredient("Frango", Measure.KILO_GRAMS));
            }
        };
    }
}
