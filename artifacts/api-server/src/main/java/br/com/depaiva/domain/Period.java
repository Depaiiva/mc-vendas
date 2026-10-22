package br.com.depaiva.domain;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;

public record Period(LocalDate start, LocalDate end) {
    public Period {
        if (start == null || end == null || end.isBefore(start)) {
            throw new IllegalArgumentException("O período informado é inválido.");
        }
    }

    public static Period day(LocalDate date) {
        return new Period(date, date);
    }

    public static Period week(LocalDate date) {
        LocalDate monday = date.with(DayOfWeek.MONDAY);
        return new Period(monday, monday.plusDays(6));
    }

    public static Period month(YearMonth month) {
        return new Period(month.atDay(1), month.atEndOfMonth());
    }
}
