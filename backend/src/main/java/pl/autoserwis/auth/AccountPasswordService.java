package pl.autoserwis.auth;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.autoserwis.auth.dto.ChangePasswordRequest;
import pl.autoserwis.exception.ResourceNotFoundException;
import pl.autoserwis.user.AppUser;
import pl.autoserwis.user.UserRepository;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class AccountPasswordService {
    private static final int BCRYPT_MAX_BYTES = 72;

    private final UserRepository users;
    private final PasswordEncoder passwords;

    public AccountPasswordService(UserRepository users, PasswordEncoder passwords) {
        this.users = users;
        this.passwords = passwords;
    }

    @Transactional
    public void changePassword(String username, ChangePasswordRequest request) {
        AppUser user = users.findByUsernameIgnoreCase(username)
            .orElseThrow(() -> new ResourceNotFoundException("User not found."));
        Map<String, String> errors = new LinkedHashMap<>();
        if (request.currentPassword().getBytes(StandardCharsets.UTF_8).length > BCRYPT_MAX_BYTES
                || !passwords.matches(request.currentPassword(), user.getPasswordHash())) {
            errors.put("currentPassword", "Current password is incorrect.");
        }
        if (!request.newPassword().equals(request.newPasswordConfirmation())) {
            errors.put("newPasswordConfirmation", "Passwords do not match.");
        }
        if (request.newPassword().getBytes(StandardCharsets.UTF_8).length > BCRYPT_MAX_BYTES) {
            errors.put("newPassword", "Password is too long after encoding.");
        }
        if (!errors.isEmpty()) {
            throw new AccountPasswordValidationException(errors);
        }
        user.changePassword(passwords.encode(request.newPassword()));
    }
}
