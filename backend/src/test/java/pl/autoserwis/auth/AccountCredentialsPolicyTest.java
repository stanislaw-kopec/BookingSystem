package pl.autoserwis.auth;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AccountCredentialsPolicyTest {
    private final AccountCredentialsPolicy policy = new AccountCredentialsPolicy();

    @Test
    void normalizesUsernameAndEmailForEveryAccountCreationPath() {
        AccountCredentialsPolicy.NormalizedCredentials normalized =
            policy.normalize("  Jan.Kowalski  ", "  JAN@example.COM  ");

        assertThat(normalized.username()).isEqualTo("Jan.Kowalski");
        assertThat(normalized.email()).isEqualTo("jan@example.com");
    }

    @Test
    void reportsConfirmationAndUtf8BcryptLimitWithCallerFieldNames() {
        String multibytePassword = "ą".repeat(37);

        Map<String, String> errors = policy.passwordValidationErrors(
            multibytePassword, "different", "newPassword", "newPasswordConfirmation");

        assertThat(errors).containsEntry("newPasswordConfirmation", "Passwords do not match.")
            .containsEntry("newPassword", "Password is too long after encoding.");
    }
}
