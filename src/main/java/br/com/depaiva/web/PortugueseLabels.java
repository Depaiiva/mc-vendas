package br.com.depaiva.web;

import br.com.depaiva.domain.Measure;
import br.com.depaiva.domain.Status;
import br.com.depaiva.domain.Type;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import org.springframework.stereotype.Component;

@Component("labels")
public class PortugueseLabels {
    private static final Locale BRAZIL = Locale.forLanguageTag("pt-BR");

    public String money(BigDecimal value) {
        NumberFormat formatter = NumberFormat.getNumberInstance(BRAZIL);
        formatter.setMinimumFractionDigits(2);
        formatter.setMaximumFractionDigits(2);
        return "R$ " + formatter.format(value.setScale(2, RoundingMode.HALF_UP));
    }

    public String type(Type value) {
        return switch (value) {
            case CHILLED_CAKE -> "Bolo gelado";
            case SANDWICH -> "Sanduíche";
            case CAKE_IN_A_JAR -> "Bolo no pote";
        };
    }

    public String measure(Measure value) {
        return switch (value) {
            case GRAMS -> "g";
            case KILO_GRAMS -> "kg";
            case LITERS -> "L";
            case MILLILITERS -> "ml";
        };
    }

    public String status(Status value) {
        return switch (value) {
            case PENDING -> "Pendente";
            case CONFIRMED -> "Confirmado";
            case DELIVERED -> "Entregue";
            case CANCELED -> "Cancelado";
        };
    }

    public String date(LocalDate value) {
        return value.format(DateTimeFormatter.ofPattern("dd/MM/yyyy", BRAZIL));
    }

    public String dateTime(LocalDateTime value) {
        return value.format(DateTimeFormatter.ofPattern("dd/MM/yyyy 'às' HH:mm", BRAZIL));
    }
}
