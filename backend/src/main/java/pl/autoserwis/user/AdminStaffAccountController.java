package pl.autoserwis.user;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pl.autoserwis.user.dto.StaffAccountRequest;
import pl.autoserwis.user.dto.StaffAccountResponse;
import pl.autoserwis.user.dto.StaffAccountStatusRequest;
import pl.autoserwis.user.dto.StaffAccountUpdateRequest;
import pl.autoserwis.user.dto.StaffPasswordResetRequest;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/admin/staff")
public class AdminStaffAccountController {
    private final StaffAccountService staffAccounts;

    public AdminStaffAccountController(StaffAccountService staffAccounts) {
        this.staffAccounts = staffAccounts;
    }

    @GetMapping("/mechanics")
    public List<StaffAccountResponse> mechanics() {
        return staffAccounts.mechanics();
    }

    @PostMapping("/mechanics")
    public ResponseEntity<StaffAccountResponse> createMechanic(@Valid @RequestBody StaffAccountRequest request) {
        StaffAccountResponse account = staffAccounts.createMechanic(request);
        return ResponseEntity.created(URI.create("/api/admin/staff/mechanics/" + account.id()))
            .body(account);
    }

    @PutMapping("/mechanics/{mechanicId}")
    public StaffAccountResponse updateMechanic(@PathVariable Long mechanicId,
            @Valid @RequestBody StaffAccountUpdateRequest request) {
        return staffAccounts.updateMechanic(mechanicId, request);
    }

    @PutMapping("/mechanics/{mechanicId}/password")
    public StaffAccountResponse resetMechanicPassword(@PathVariable Long mechanicId,
            @Valid @RequestBody StaffPasswordResetRequest request) {
        return staffAccounts.resetMechanicPassword(mechanicId, request);
    }

    @PutMapping("/mechanics/{mechanicId}/status")
    public StaffAccountResponse updateMechanicStatus(@PathVariable Long mechanicId,
            @Valid @RequestBody StaffAccountStatusRequest request) {
        return staffAccounts.updateMechanicStatus(mechanicId, request);
    }
}
