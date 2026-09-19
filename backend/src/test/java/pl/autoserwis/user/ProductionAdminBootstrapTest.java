package pl.autoserwis.user;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pl.autoserwis.user.dto.ManagedAccountCreateRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductionAdminBootstrapTest {
    private static Validator validator;

    @Mock
    private UserRepository users;

    @Mock
    private AdminAccountService accounts;

    @BeforeAll
    static void createValidator() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Test
    void createsInitialAdministratorFromProductionConfiguration() {
        when(users.existsByRole(UserRole.ADMIN)).thenReturn(false);
        ProductionAdminBootstrap bootstrap = new ProductionAdminBootstrap(
            users, accounts, validator, "portfolio.admin", "admin@example.com", "StrongPassword123!");

        bootstrap.run(null);

        ArgumentCaptor<ManagedAccountCreateRequest> request =
            ArgumentCaptor.forClass(ManagedAccountCreateRequest.class);
        verify(accounts).createAdministrator(request.capture());
        assertThat(request.getValue().username()).isEqualTo("portfolio.admin");
        assertThat(request.getValue().email()).isEqualTo("admin@example.com");
        assertThat(request.getValue().password()).isEqualTo("StrongPassword123!");
        assertThat(request.getValue().passwordConfirmation()).isEqualTo("StrongPassword123!");
    }

    @Test
    void doesNotChangeAnExistingAdministrator() {
        when(users.existsByRole(UserRole.ADMIN)).thenReturn(true);
        ProductionAdminBootstrap bootstrap = new ProductionAdminBootstrap(
            users, accounts, validator, "", "", "");

        bootstrap.run(null);

        verify(accounts, never()).createAdministrator(any());
    }

    @Test
    void failsFirstStartWhenBootstrapConfigurationIsMissing() {
        when(users.existsByRole(UserRole.ADMIN)).thenReturn(false);
        ProductionAdminBootstrap bootstrap = new ProductionAdminBootstrap(
            users, accounts, validator, "", "", "");

        assertThatThrownBy(() -> bootstrap.run(null))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("email")
            .hasMessageContaining("password")
            .hasMessageContaining("username");

        verify(accounts, never()).createAdministrator(any());
    }
}
