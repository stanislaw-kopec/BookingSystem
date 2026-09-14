package pl.autoserwis.user;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pl.autoserwis.user.dto.AdminAccountResponse;
import pl.autoserwis.user.dto.AdminAccountStatusRequest;
import pl.autoserwis.user.dto.AdminAccountUpdateRequest;
import pl.autoserwis.user.dto.AdminPasswordResetRequest;
import pl.autoserwis.user.dto.ManagedAccountCreateRequest;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/admin/staff")
public class AdminStaffAccountController {
    private final AdminAccountService accounts;

    public AdminStaffAccountController(AdminAccountService accounts) {
        this.accounts = accounts;
    }

    @GetMapping("/mechanics")
    public List<AdminAccountResponse> mechanics() {
        return accounts.mechanics();
    }

    @PostMapping("/mechanics")
    public ResponseEntity<AdminAccountResponse> createMechanic(@Valid @RequestBody ManagedAccountCreateRequest request) {
        AdminAccountResponse account = accounts.createMechanic(request);
        return ResponseEntity.created(URI.create("/api/admin/staff/mechanics/" + account.id())).body(account);
    }

    @PutMapping("/mechanics/{mechanicId}")
    public AdminAccountResponse updateMechanic(@PathVariable Long mechanicId,
            @Valid @RequestBody AdminAccountUpdateRequest request) {
        return accounts.updateMechanic(mechanicId, request);
    }

    @PutMapping("/mechanics/{mechanicId}/password")
    public AdminAccountResponse resetMechanicPassword(@PathVariable Long mechanicId,
            @Valid @RequestBody AdminPasswordResetRequest request) {
        return accounts.resetMechanicPassword(mechanicId, request);
    }

    @PutMapping("/mechanics/{mechanicId}/status")
    public AdminAccountResponse updateMechanicStatus(@PathVariable Long mechanicId,
            @Valid @RequestBody AdminAccountStatusRequest request) {
        return accounts.updateMechanicStatus(mechanicId, request);
    }
}
