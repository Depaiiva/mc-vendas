package br.com.depaiva.web;

import br.com.depaiva.domain.Period;
import br.com.depaiva.service.MetricsService;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.ModelAndView;

@RestController
public class DashboardController {
    private static final ZoneId ZONE = ZoneId.of("America/Sao_Paulo");
    private final MetricsService metrics;

    public DashboardController(MetricsService metrics) {
        this.metrics = metrics;
    }

    @GetMapping("/dashboard")
    public ModelAndView dashboard(@RequestParam(defaultValue = "day") String view,
                                  @RequestParam(required = false) String date) {
        LocalDate selectedDate;
        try {
            selectedDate = date == null || date.isBlank() ? LocalDate.now(ZONE) : LocalDate.parse(date);
        } catch (RuntimeException exception) {
            selectedDate = LocalDate.now(ZONE);
        }
        String selectedView = ListOfViews.normalize(view);
        Period period = switch (selectedView) {
            case "week" -> Period.week(selectedDate);
            case "month" -> Period.month(YearMonth.from(selectedDate));
            default -> Period.day(selectedDate);
        };
        ModelAndView modelAndView = new ModelAndView("dashboard");
        modelAndView.addObject("view", selectedView);
        modelAndView.addObject("selectedDate", selectedDate);
        modelAndView.addObject("period", period);
        modelAndView.addObject("summary", metrics.calculate(period));
        return modelAndView;
    }

    private static final class ListOfViews {
        static String normalize(String view) {
            return "week".equals(view) || "month".equals(view) ? view : "day";
        }
    }
}
