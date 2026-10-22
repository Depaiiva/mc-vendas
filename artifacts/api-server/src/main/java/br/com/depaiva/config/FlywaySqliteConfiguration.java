package br.com.depaiva.config;

import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FlywaySqliteConfiguration {
    @Bean
    Flyway sqliteFlyway(DataSource dataSource) {
        return Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .load();
    }

    @Bean
    FlywayMigrationInitializer sqliteFlywayInitializer(Flyway sqliteFlyway) {
        return new FlywayMigrationInitializer(sqliteFlyway);
    }
}
