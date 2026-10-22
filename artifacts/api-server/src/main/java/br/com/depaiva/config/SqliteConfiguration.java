package br.com.depaiva.config;

import java.sql.Connection;
import javax.sql.DataSource;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SqliteConfiguration {
    @Bean
    ApplicationRunner enableSqliteWal(DataSource dataSource) {
        return args -> {
            try (Connection connection = dataSource.getConnection();
                 var statement = connection.createStatement()) {
                statement.execute("PRAGMA journal_mode=WAL");
                statement.execute("PRAGMA foreign_keys=ON");
                statement.execute("PRAGMA busy_timeout=5000");
            }
        };
    }
}
