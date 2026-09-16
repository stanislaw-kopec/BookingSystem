package pl.autoserwis.user;

import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.autoserwis.auth.RegistrationConflictException;
import pl.autoserwis.auth.RegistrationValidationException;
import pl.autoserwis.exception.ResourceNotFoundException;
import pl.autoserwis.user.dto.AdminAccountPageResponse;
import pl.autoserwis.user.dto.AdminAccountResponse;
import pl.autoserwis.user.dto.AdminAccountStatusRequest;
import pl.autoserwis.user.dto.AdminAccountUpdateRequest;
import pl.autoserwis.user.dto.AdminPasswordResetRequest;
import pl.autoserwis.user.dto.ManagedAccountCreateRequest;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@Transactional(readOnly = true)
public class AdminAccountService {
    private static final int BCRYPT_MAX_BYTES = 72;
    private static final int MAX_PAGE_SIZE = 50;

    private final UserRepository users;
    private final PasswordEncoder passwords;

    public AdminAccountService(UserRepository users, PasswordEncoder passwords) {
        this.users = users;
        this.passwords = passwords;
    }

    public AdminAccountPageResponse accounts(UserRole role, Boolean enabled, String query, int page, int size) {
        if (page < 0) {
            throw new AccountManagementValidationException(Map.of("page", "Page number cannot be negative."));
        }
        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new AccountManagementValidationException(Map.of("size", "Page size must be between 1 and 50."));
        }

        String normalizedQuery = query == null ? "" : query.strip().toLowerCase(Locale.ROOT);
        Specification<AppUser> specification = (root, criteriaQuery, builder) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (role != null) {
                predicates.add(builder.equal(root.get("role"), role));
            }
            if (enabled != null) {
                predicates.add(builder.equal(root.get("enabled"), enabled));
            }
            if (!normalizedQuery.isEmpty()) {
                String pattern = "%" + escapeLike(normalizedQuery) + "%";
                predicates.add(builder.or(
                    builder.like(builder.lower(root.get("username")), pattern, '\\'),
                    builder.like(builder.lower(root.get("email")), pattern, '\\')
                ));
            }
            return builder.and(predicates.toArray(Predicate[]::new));
        };
        Page<AppUser> result = users.findAll(specification, PageRequest.of(page, size,
            Sort.by(Sort.Order.asc("username").ignoreCase(), Sort.Order.asc("id"))));
        return new AdminAccountPageResponse(result.getContent().stream().map(this::response).toList(),
            result.getNumber(), result.getSize(), result.getTotalElements(), result.getTotalPages());
    }

    public List<AdminAccountResponse> mechanics() {
        return users.findByRoleOrderByUsernameAsc(UserRole.MECHANIC).stream()
            .map(this::response)
            .toList();
    }

    @Transactional
    public AdminAccountResponse createMechanic(ManagedAccountCreateRequest request) {
        return createAccount(request, UserRole.MECHANIC, "Mechanic account cannot be created.");
    }

    @Transactional
    public AdminAccountResponse createAdministrator(ManagedAccountCreateRequest request) {
        return createAccount(request, UserRole.ADMIN, "Administrator account cannot be created.");
    }

    private AdminAccountResponse createAccount(ManagedAccountCreateRequest request, UserRole role, String conflictMessage) {
        validatePasswords(request.password(), request.passwordConfirmation());
        String username = normalizedUsername(request.username());
        String email = normalizedEmail(request.email());
        validateUniqueAccount(username, email, null, conflictMessage);
        return response(users.saveAndFlush(new AppUser(
            username, email, passwords.encode(request.password()), role)));
    }

    @Transactional
    public AdminAccountResponse updateAccount(Long accountId, AdminAccountUpdateRequest request) {
        AppUser account = manageableAccount(accountId);
        String username = normalizedUsername(request.username());
        String email = normalizedEmail(request.email());
        validateUniqueAccount(username, email, account.getId(), "Account cannot be updated.");
        account.updateAccount(username, email);
        return response(account);
    }

    @Transactional
    public AdminAccountResponse updateMechanic(Long mechanicId, AdminAccountUpdateRequest request) {
        mechanicAccount(mechanicId);
        return updateAccount(mechanicId, request);
    }

    @Transactional
    public AdminAccountResponse resetPassword(Long accountId, AdminPasswordResetRequest request) {
        AppUser account = manageableAccount(accountId);
        validatePasswords(request.password(), request.passwordConfirmation());
        account.changePassword(passwords.encode(request.password()));
        return response(account);
    }

    @Transactional
    public AdminAccountResponse resetMechanicPassword(Long mechanicId, AdminPasswordResetRequest request) {
        mechanicAccount(mechanicId);
        return resetPassword(mechanicId, request);
    }

    @Transactional
    public AdminAccountResponse updateStatus(Long accountId, AdminAccountStatusRequest request) {
        AppUser account = manageableAccount(accountId);
        account.setEnabled(request.enabled());
        return response(account);
    }

    @Transactional
    public AdminAccountResponse updateMechanicStatus(Long mechanicId, AdminAccountStatusRequest request) {
        mechanicAccount(mechanicId);
        return updateStatus(mechanicId, request);
    }

    private AppUser mechanicAccount(Long mechanicId) {
        return users.findByIdForUpdate(mechanicId)
            .filter(user -> user.getRole() == UserRole.MECHANIC)
            .orElseThrow(() -> new ResourceNotFoundException("Mechanic account not found."));
    }

    private AppUser manageableAccount(Long accountId) {
        return users.findByIdForUpdate(accountId)
            .filter(user -> user.getRole() == UserRole.CLIENT || user.getRole() == UserRole.MECHANIC)
            .orElseThrow(() -> new ResourceNotFoundException("Managed account not found."));
    }

    private void validatePasswords(String password, String passwordConfirmation) {
        Map<String, String> errors = new LinkedHashMap<>();
        if (!password.equals(passwordConfirmation)) {
            errors.put("passwordConfirmation", "Passwords do not match.");
        }
        if (password.getBytes(StandardCharsets.UTF_8).length > BCRYPT_MAX_BYTES) {
            errors.put("password", "Password is too long after encoding.");
        }
        if (!errors.isEmpty()) {
            throw new RegistrationValidationException("Form data is invalid.", errors);
        }
    }

    private void validateUniqueAccount(String username, String email, Long currentUserId, String message) {
        Map<String, String> conflicts = new LinkedHashMap<>();
        users.findByUsernameIgnoreCase(username)
            .filter(user -> !user.getId().equals(currentUserId))
            .ifPresent(user -> conflicts.put("username", "This username is already taken."));
        users.findByEmailIgnoreCase(email)
            .filter(user -> !user.getId().equals(currentUserId))
            .ifPresent(user -> conflicts.put("email", "An account with this email already exists."));
        if (!conflicts.isEmpty()) {
            throw new RegistrationConflictException(message, conflicts);
        }
    }

    private String normalizedUsername(String username) {
        return username.strip();
    }

    private String normalizedEmail(String email) {
        return email.strip().toLowerCase(Locale.ROOT);
    }

    private String escapeLike(String value) {
        return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }

    private AdminAccountResponse response(AppUser user) {
        return new AdminAccountResponse(user.getId(), user.getUsername(), user.getEmail(),
            user.getRole(), user.isEnabled());
    }
}
