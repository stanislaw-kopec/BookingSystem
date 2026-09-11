package pl.autoserwis.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {
    @Bean
    OpenAPI bookingSystemOpenApi() {
        return new OpenAPI()
            .info(new Info()
                .title("BookingSystem API")
                .version("0.0.1")
                .description("""
                    REST API for the Mietek Customs workshop booking system.
                    The application uses Spring Security sessions, CSRF protection and role-based access.
                    """)
                .license(new License().name("Portfolio project")))
            .components(new Components()
                .addSecuritySchemes("sessionCookie", new SecurityScheme()
                    .type(SecurityScheme.Type.APIKEY)
                    .in(SecurityScheme.In.COOKIE)
                    .name("JSESSIONID")
                    .description("Session cookie issued after logging in through /api/auth/login."))
                .addSecuritySchemes("csrfToken", new SecurityScheme()
                    .type(SecurityScheme.Type.APIKEY)
                    .in(SecurityScheme.In.HEADER)
                    .name("X-CSRF-TOKEN")
                    .description("CSRF token returned by /api/auth/csrf for state-changing requests.")));
    }

    @Bean
    GroupedOpenApi bookingSystemApiGroup() {
        return GroupedOpenApi.builder()
            .group("booking-system")
            .pathsToMatch("/api/**")
            .pathsToExclude("/api/health")
            .build();
    }
}
