package br.com.depaiva.config;

import jakarta.servlet.http.HttpServletRequest;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

public class LoginThrottle {
    private static final int MAX_FAILURES = 5;
    private static final Duration WINDOW = Duration.ofMinutes(15);
    private final ConcurrentHashMap<String, AttemptWindow> windows = new ConcurrentHashMap<>();

    public boolean isBlocked(HttpServletRequest request) {
        AttemptWindow window = currentWindow(request);
        return window.failures >= MAX_FAILURES && Instant.now().isBefore(window.startedAt.plus(WINDOW));
    }

    public boolean recordFailure(HttpServletRequest request) {
        AttemptWindow window = currentWindow(request);
        window.failures++;
        return window.failures >= MAX_FAILURES;
    }

    public void clear(HttpServletRequest request) {
        windows.remove(clientIp(request));
    }

    private AttemptWindow currentWindow(HttpServletRequest request) {
        String ip = clientIp(request);
        Instant now = Instant.now();
        return windows.compute(ip, (ignored, existing) -> {
            if (existing == null || now.isAfter(existing.startedAt.plus(WINDOW))) {
                return new AttemptWindow(now);
            }
            return existing;
        });
    }

    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private static final class AttemptWindow {
        private final Instant startedAt;
        private int failures;

        private AttemptWindow(Instant startedAt) {
            this.startedAt = startedAt;
        }
    }
}
