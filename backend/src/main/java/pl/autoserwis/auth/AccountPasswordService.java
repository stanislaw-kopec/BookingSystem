package pl.autoserwis.auth;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.autoserwis.auth.dto.ChangePasswordRequest;
import pl.autoserwis.exception.ResourceNotFoundException;
import pl.autoserwis.user.AppUser;
import pl.autoserwis.user.UserRepository;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class AccountPasswordService {
    private final UserRepository users;
    private final PasswordEncoder passwords;
    private final AccountCredentialsPolicy credentials;

    public AccountPasswordService(UserRepository users, PasswordEncoder passwords,
            AccountCredentialsPolicy credentials) {
        this.users = users;
        this.passwords = passwords;
        this.credentials = credentials;
    }

    @Transactional
    public long changePassword(Long userId, ChangePasswordRequest request) {
        AppUser user = users.findByIdForUpdate(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found."));
        Map<String, String> errors = new LinkedHashMap<>();
        if (!credentials.isWithinBcryptLimit(request.currentPassword())
                || !passwords.matches(request.currentPassword(), user.getPasswordHash())) {
            errors.put("currentPassword", "Current password is incorrect.");
        }
        errors.putAll(credentials.passwordValidationErrors(
            request.newPassword(), request.newPasswordConfirmation(),
            "newPassword", "newPasswordConfirmation"));
        if (!errors.isEmpty()) {
            throw new AccountPasswordValidationException(errors);
        }
        user.changePassword(passwords.encode(request.newPassword()));
        return user.getSessionVersion();
    }
}
