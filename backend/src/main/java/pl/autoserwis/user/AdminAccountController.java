package pl.autoserwis.user;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pl.autoserwis.user.dto.AdminAccountPageResponse;
import pl.autoserwis.user.dto.AdminAccountResponse;
import pl.autoserwis.user.dto.AdminAccountStatusRequest;
import pl.autoserwis.user.dto.AdminAccountUpdateRequest;
import pl.autoserwis.user.dto.AdminPasswordResetRequest;
import pl.autoserwis.user.dto.ManagedAccountCreateRequest;

import java.net.URI;

@RestController
@RequestMapping("/api/admin/accounts")
public class AdminAccountController {
    private final AdminAccountService accounts;

    public AdminAccountController(AdminAccountService accounts) {
        this.accounts = accounts;
    }

    @GetMapping
    public AdminAccountPageResponse accounts(
            @RequestParam(required = false) UserRole role,
            @RequestParam(required = false) Boolean enabled,
            @RequestParam(required = false, defaultValue = "") String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return accounts.accounts(role, enabled, query, page, size);
    }

    @PostMapping("/mechanics")
    public ResponseEntity<AdminAccountResponse> createMechanic(@Valid @RequestBody ManagedAccountCreateRequest request) {
        AdminAccountResponse account = accounts.createMechanic(request);
        return ResponseEntity.created(URI.create("/api/admin/accounts/" + account.id())).body(account);
    }

    @PostMapping("/administrators")
    public ResponseEntity<AdminAccountResponse> createAdministrator(
            @Valid @RequestBody ManagedAccountCreateRequest request) {
        AdminAccountResponse account = accounts.createAdministrator(request);
        return ResponseEntity.created(URI.create("/api/admin/accounts/" + account.id())).body(account);
    }

    @PutMapping("/{accountId}")
    public AdminAccountResponse updateAccount(@PathVariable Long accountId,
            @Valid @RequestBody AdminAccountUpdateRequest request) {
        return accounts.updateAccount(accountId, request);
    }

    @PutMapping("/{accountId}/password")
    public AdminAccountResponse resetPassword(@PathVariable Long accountId,
            @Valid @RequestBody AdminPasswordResetRequest request) {
        return accounts.resetPassword(accountId, request);
    }

    @PutMapping("/{accountId}/status")
    public AdminAccountResponse updateStatus(@PathVariable Long accountId,
            @Valid @RequestBody AdminAccountStatusRequest request) {
        return accounts.updateStatus(accountId, request);
    }
}
