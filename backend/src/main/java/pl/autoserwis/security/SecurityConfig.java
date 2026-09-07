package pl.autoserwis.security;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Configuration
public class SecurityConfig {
    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
            .csrf(Customizer.withDefaults())
            .requestCache(cache -> cache.disable())
            .authorizeHttpRequests(authorize -> authorize
                .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
                .requestMatchers(HttpMethod.GET,
                    "/api/health", "/api/services", "/api/services/*",
                    "/api/service-categories/*", "/api/auth/me", "/api/auth/csrf",
                    "/api/appointments/availability").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/auth/register", "/api/appointments/guest")
                    .permitAll()
                .requestMatchers("/api/profile/**").hasRole("CLIENT")
                .requestMatchers("/api/vehicles", "/api/vehicles/**").hasRole("CLIENT")
                .requestMatchers("/api/staff/appointments", "/api/staff/appointments/**")
                    .hasAnyRole("MECHANIC", "ADMIN")
                .requestMatchers("/api/appointments", "/api/appointments/**").hasRole("CLIENT")
                .requestMatchers("/api/services", "/api/services/**",
                    "/api/service-categories", "/api/service-categories/**")
                    .hasAnyRole("MECHANIC", "ADMIN")
                .anyRequest().denyAll())
            .formLogin(form -> form
                .loginPage("/api/auth/login")
                .loginProcessingUrl("/api/auth/login")
                .successHandler((request, response, authentication) -> response.setStatus(204))
                .failureHandler((request, response, exception) ->
                    writeError(response, 401, "Nieprawidłowy login lub hasło."))
                .permitAll())
            .logout(logout -> logout
                .logoutUrl("/api/auth/logout")
                .logoutSuccessHandler((request, response, authentication) -> response.setStatus(204))
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID")
                .permitAll())
            .exceptionHandling(exceptions -> exceptions
                .authenticationEntryPoint((request, response, exception) ->
                    writeError(response, 401, "Zaloguj się, aby wykonać tę operację."))
                .accessDeniedHandler((request, response, exception) ->
                    writeError(response, 403, "Brak uprawnień lub nieprawidłowy token formularza. Odśwież stronę.")))
            .build();
    }

    private static void writeError(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        // Messages passed here are fixed server strings, never user input.
        response.getWriter().write("{\"status\":" + status + ",\"message\":\"" + message + "\",\"fieldErrors\":{}}");
    }
}
