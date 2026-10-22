package br.com.depaiva.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.web.filter.OncePerRequestFilter;

public class LoginThrottleFilter extends OncePerRequestFilter {
    private final LoginThrottle throttle;

    public LoginThrottleFilter(LoginThrottle throttle) {
        this.throttle = throttle;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        if ("POST".equalsIgnoreCase(request.getMethod())
                && "/login".equals(request.getServletPath())
                && throttle.isBlocked(request)) {
            response.setStatus(429);
            response.sendRedirect(request.getContextPath() + "/login?error=locked");
            return;
        }
        chain.doFilter(request, response);
    }
}
