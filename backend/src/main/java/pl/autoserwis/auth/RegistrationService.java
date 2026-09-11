package pl.autoserwis.auth;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.autoserwis.auth.dto.RegistrationRequest;
import pl.autoserwis.user.AppUser;
import pl.autoserwis.user.UserRepository;
import pl.autoserwis.user.UserRole;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

@Service
public class RegistrationService {
    private static final int BCRYPT_MAX_BYTES = 72;

    private final UserRepository users;
    private final PasswordEncoder passwords;

    public RegistrationService(UserRepository users, PasswordEncoder passwords) {
        this.users = users;
        this.passwords = passwords;
    }

    @Transactional
    public AppUser register(RegistrationRequest request) {
        validatePasswords(request);

        String username = request.username().strip();
        String email = request.email().strip().toLowerCase(Locale.ROOT);
        Map<String, String> conflicts = new LinkedHashMap<>();
        if (users.existsByUsernameIgnoreCase(username)) {
            conflicts.put("username", "This username is already taken.");
        }
        if (users.existsByEmailIgnoreCase(email)) {
            conflicts.put("email", "An account with this email already exists.");
        }
        if (!conflicts.isEmpty()) {
            throw new RegistrationConflictException("Account cannot be created.", conflicts);
        }

        return users.saveAndFlush(new AppUser(
            username, email, passwords.encode(request.password()), UserRole.CLIENT));
    }

    private void validatePasswords(RegistrationRequest request) {
        Map<String, String> errors = new LinkedHashMap<>();
        if (!request.password().equals(request.passwordConfirmation())) {
            errors.put("passwordConfirmation", "Passwords do not match.");
        }
        if (request.password().getBytes(StandardCharsets.UTF_8).length > BCRYPT_MAX_BYTES) {
            errors.put("password", "Password is too long after encoding.");
        }
        if (!errors.isEmpty()) {
            throw new RegistrationValidationException("Form data is invalid.", errors);
        }
    }
}
