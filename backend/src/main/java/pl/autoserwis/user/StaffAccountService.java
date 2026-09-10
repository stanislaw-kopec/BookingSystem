package pl.autoserwis.user;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.autoserwis.auth.RegistrationConflictException;
import pl.autoserwis.auth.RegistrationValidationException;
import pl.autoserwis.exception.ResourceNotFoundException;
import pl.autoserwis.user.dto.StaffAccountRequest;
import pl.autoserwis.user.dto.StaffAccountResponse;
import pl.autoserwis.user.dto.StaffAccountStatusRequest;
import pl.autoserwis.user.dto.StaffAccountUpdateRequest;
import pl.autoserwis.user.dto.StaffPasswordResetRequest;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@Transactional(readOnly = true)
public class StaffAccountService {
    private static final int BCRYPT_MAX_BYTES = 72;

    private final UserRepository users;
    private final PasswordEncoder passwords;

    public StaffAccountService(UserRepository users, PasswordEncoder passwords) {
        this.users = users;
        this.passwords = passwords;
    }

    public List<StaffAccountResponse> mechanics() {
        return users.findByRoleOrderByUsernameAsc(UserRole.MECHANIC).stream()
            .map(this::response)
            .toList();
    }

    @Transactional
    public StaffAccountResponse createMechanic(StaffAccountRequest request) {
        validatePasswords(request.password(), request.passwordConfirmation());

        String username = normalizedUsername(request.username());
        String email = normalizedEmail(request.email());
        validateUniqueAccount(username, email, null, "Nie można utworzyć konta mechanika.");

        AppUser mechanic = users.saveAndFlush(new AppUser(
            username, email, passwords.encode(request.password()), UserRole.MECHANIC));
        return response(mechanic);
    }

    @Transactional
    public StaffAccountResponse updateMechanic(Long mechanicId, StaffAccountUpdateRequest request) {
        AppUser mechanic = mechanic(mechanicId);
        String username = normalizedUsername(request.username());
        String email = normalizedEmail(request.email());
        validateUniqueAccount(username, email, mechanic.getId(), "Nie można zaktualizować konta mechanika.");
        mechanic.updateAccount(username, email);
        return response(mechanic);
    }

    @Transactional
    public StaffAccountResponse resetMechanicPassword(Long mechanicId, StaffPasswordResetRequest request) {
        AppUser mechanic = mechanic(mechanicId);
        validatePasswords(request.password(), request.passwordConfirmation());
        mechanic.changePassword(passwords.encode(request.password()));
        return response(mechanic);
    }

    @Transactional
    public StaffAccountResponse updateMechanicStatus(Long mechanicId, StaffAccountStatusRequest request) {
        AppUser mechanic = mechanic(mechanicId);
        mechanic.setEnabled(request.enabled());
        return response(mechanic);
    }

    private void validatePasswords(String password, String passwordConfirmation) {
        Map<String, String> errors = new LinkedHashMap<>();
        if (!password.equals(passwordConfirmation)) {
            errors.put("passwordConfirmation", "Hasła nie są takie same.");
        }
        if (password.getBytes(StandardCharsets.UTF_8).length > BCRYPT_MAX_BYTES) {
            errors.put("password", "Hasło jest zbyt długie po zakodowaniu.");
        }
        if (!errors.isEmpty()) {
            throw new RegistrationValidationException("Popraw dane formularza.", errors);
        }
    }

    private void validateUniqueAccount(String username, String email, Long currentUserId, String message) {
        Map<String, String> conflicts = new LinkedHashMap<>();
        users.findByUsernameIgnoreCase(username)
            .filter(user -> !user.getId().equals(currentUserId))
            .ifPresent(user -> conflicts.put("username", "Ten login jest już zajęty."));
        users.findByEmailIgnoreCase(email)
            .filter(user -> !user.getId().equals(currentUserId))
            .ifPresent(user -> conflicts.put("email", "Konto z tym adresem e-mail już istnieje."));
        if (!conflicts.isEmpty()) {
            throw new RegistrationConflictException(message, conflicts);
        }
    }

    private AppUser mechanic(Long mechanicId) {
        return users.findById(mechanicId)
            .filter(user -> user.getRole() == UserRole.MECHANIC)
            .orElseThrow(() -> new ResourceNotFoundException("Nie znaleziono konta mechanika."));
    }

    private String normalizedUsername(String username) {
        return username.strip();
    }

    private String normalizedEmail(String email) {
        return email.strip().toLowerCase(Locale.ROOT);
    }

    private StaffAccountResponse response(AppUser user) {
        return new StaffAccountResponse(user.getId(), user.getUsername(), user.getEmail(), user.getRole(), user.isEnabled());
    }
}
