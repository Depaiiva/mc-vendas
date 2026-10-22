package br.com.depaiva.config;

import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfiguration {
    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    UserDetailsService userDetailsService(
            PasswordEncoder encoder,
            @Value("${ADMIN_USERNAME:admin}") String username,
            @Value("${ADMIN_PASSWORD:}") String password) {
        if (password.isBlank()) {
            throw new IllegalStateException("Configure ADMIN_PASSWORD nos Secrets antes de iniciar o app.");
        }
        return new InMemoryUserDetailsManager(
                User.withUsername(username).password(encoder.encode(password)).roles("ADMIN").build());
    }

    @Bean
    LoginThrottle loginThrottle() {
        return new LoginThrottle();
    }

    @Bean
    LoginThrottleFilter loginThrottleFilter(LoginThrottle throttle) {
        return new LoginThrottleFilter(throttle);
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, LoginThrottle throttle,
                                            LoginThrottleFilter throttleFilter) throws Exception {
        http
                .authorizeHttpRequests(requests -> requests
                        .requestMatchers(
                            "/login",
                            "/css/**",
                            "/js/**",
                            "/favicon.svg",
                            "/swagger-ui/**",
                            "/v3/api-docs/**",
                            "/swagger-ui.html").permitAll()
                        .anyRequest().authenticated())
                .formLogin(form -> form
                        .loginPage("/login")
                        .permitAll()
                        .failureHandler((request, response, exception) -> {
                            boolean locked = throttle.recordFailure(request);
                            response.sendRedirect(request.getContextPath() + "/login?error=" + (locked ? "locked" : "invalid"));
                        })
                        .successHandler((request, response, authentication) -> {
                            throttle.clear(request);
                            response.sendRedirect(request.getContextPath() + "/");
                        }))
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login?logout")
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID"))
                .addFilterBefore(throttleFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }
}
