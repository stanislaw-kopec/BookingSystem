package pl.autoserwis.auth;

import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

@Component
public class AccountCredentialsPolicy {
    private static final int BCRYPT_MAX_BYTES = 72;

    public NormalizedCredentials normalize(String username, String email) {
        return new NormalizedCredentials(
            username.strip(),
            email.strip().toLowerCase(Locale.ROOT)
        );
    }

    public Map<String, String> passwordValidationErrors(String password, String confirmation) {
        return passwordValidationErrors(password, confirmation, "password", "passwordConfirmation");
    }

    public Map<String, String> passwordValidationErrors(String password, String confirmation,
            String passwordField, String confirmationField) {
        Map<String, String> errors = new LinkedHashMap<>();
        if (!password.equals(confirmation)) {
            errors.put(confirmationField, "Passwords do not match.");
        }
        if (!isWithinBcryptLimit(password)) {
            errors.put(passwordField, "Password is too long after encoding.");
        }
        return errors;
    }

    public boolean isWithinBcryptLimit(String password) {
        return password.getBytes(StandardCharsets.UTF_8).length <= BCRYPT_MAX_BYTES;
    }

    public record NormalizedCredentials(String username, String email) {}
}
