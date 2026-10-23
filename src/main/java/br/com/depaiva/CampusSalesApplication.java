package br.com.depaiva;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import java.nio.file.Path;

@SpringBootApplication
public class CampusSalesApplication {
    public static void main(String[] args) {
        String databasePath = System.getenv().getOrDefault("DB_PATH", "./data/app.db");
        Path parent = Path.of(databasePath).toAbsolutePath().getParent();
        if (parent != null) {
            try {
                java.nio.file.Files.createDirectories(parent);
            } catch (java.io.IOException exception) {
                throw new IllegalStateException("Não foi possível criar a pasta do banco SQLite.", exception);
            }
        }
        SpringApplication.run(CampusSalesApplication.class, args);
    }
}
