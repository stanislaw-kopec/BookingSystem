package pl.autoserwis.user;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import pl.autoserwis.user.dto.ManagedAccountCreateRequest;

import java.util.Comparator;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@Profile("production")
public class ProductionAdminBootstrap implements ApplicationRunner {
    private static final Logger LOGGER = LoggerFactory.getLogger(ProductionAdminBootstrap.class);

    private final UserRepository users;
    private final AdminAccountService accounts;
    private final Validator validator;
    private final String username;
    private final String email;
    private final String password;

    public ProductionAdminBootstrap(UserRepository users, AdminAccountService accounts, Validator validator,
            @Value("${app.bootstrap-admin.username:}") String username,
            @Value("${app.bootstrap-admin.email:}") String email,
            @Value("${app.bootstrap-admin.password:}") String password) {
        this.users = users;
        this.accounts = accounts;
        this.validator = validator;
        this.username = username;
        this.email = email;
        this.password = password;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (users.existsByRole(UserRole.ADMIN)) {
            return;
        }

        ManagedAccountCreateRequest request = new ManagedAccountCreateRequest(
            username, email, password, password);
        Set<ConstraintViolation<ManagedAccountCreateRequest>> violations = validator.validate(request);
        if (!violations.isEmpty()) {
            String invalidFields = violations.stream()
                .map(violation -> violation.getPropertyPath().toString())
                .sorted(Comparator.naturalOrder())
                .distinct()
                .collect(Collectors.joining(", "));
            throw new IllegalStateException(
                "Production administrator configuration is invalid. Check: " + invalidFields + ".");
        }

        accounts.createAdministrator(request);
        LOGGER.info("Created the initial production administrator account '{}'.", username);
    }
}
