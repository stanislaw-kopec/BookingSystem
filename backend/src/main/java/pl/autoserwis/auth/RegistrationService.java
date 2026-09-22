package pl.autoserwis.auth;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.autoserwis.auth.dto.RegistrationRequest;
import pl.autoserwis.user.AppUser;
import pl.autoserwis.user.UserRepository;
import pl.autoserwis.user.UserRole;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class RegistrationService {
    private final UserRepository users;
    private final PasswordEncoder passwords;
    private final AccountCredentialsPolicy credentials;

    public RegistrationService(UserRepository users, PasswordEncoder passwords,
            AccountCredentialsPolicy credentials) {
        this.users = users;
        this.passwords = passwords;
        this.credentials = credentials;
    }

    @Transactional
    public AppUser register(RegistrationRequest request) {
        validatePasswords(request);

        AccountCredentialsPolicy.NormalizedCredentials normalized =
            credentials.normalize(request.username(), request.email());
        Map<String, String> conflicts = new LinkedHashMap<>();
        if (users.existsByUsernameIgnoreCase(normalized.username())) {
            conflicts.put("username", "This username is already taken.");
        }
        if (users.existsByEmailIgnoreCase(normalized.email())) {
            conflicts.put("email", "An account with this email already exists.");
        }
        if (!conflicts.isEmpty()) {
            throw new RegistrationConflictException("Account cannot be created.", conflicts);
        }

        return users.saveAndFlush(new AppUser(
            normalized.username(), normalized.email(), passwords.encode(request.password()), UserRole.CLIENT));
    }

    private void validatePasswords(RegistrationRequest request) {
        Map<String, String> errors = credentials.passwordValidationErrors(
            request.password(), request.passwordConfirmation());
        if (!errors.isEmpty()) {
            throw new RegistrationValidationException("Form data is invalid.", errors);
        }
    }
}
