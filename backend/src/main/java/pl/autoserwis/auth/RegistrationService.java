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
            conflicts.put("username", "Ten login jest już zajęty.");
        }
        if (users.existsByEmailIgnoreCase(email)) {
            conflicts.put("email", "Konto z tym adresem e-mail już istnieje.");
        }
        if (!conflicts.isEmpty()) {
            throw new RegistrationConflictException("Nie można utworzyć konta.", conflicts);
        }

        return users.saveAndFlush(new AppUser(
            username, email, passwords.encode(request.password()), UserRole.CLIENT));
    }

    private void validatePasswords(RegistrationRequest request) {
        Map<String, String> errors = new LinkedHashMap<>();
        if (!request.password().equals(request.passwordConfirmation())) {
            errors.put("passwordConfirmation", "Hasła nie są takie same.");
        }
        if (request.password().getBytes(StandardCharsets.UTF_8).length > BCRYPT_MAX_BYTES) {
            errors.put("password", "Hasło jest zbyt długie po zakodowaniu.");
        }
        if (!errors.isEmpty()) {
            throw new RegistrationValidationException("Popraw dane formularza.", errors);
        }
    }
}
