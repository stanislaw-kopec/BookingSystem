package pl.autoserwis.auth;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pl.autoserwis.auth.dto.ChangePasswordRequest;

@RestController
@RequestMapping("/api/auth/password")
public class AccountPasswordController {
    private final AccountPasswordService accountPasswordService;

    public AccountPasswordController(AccountPasswordService accountPasswordService) {
        this.accountPasswordService = accountPasswordService;
    }

    @PutMapping
    public ResponseEntity<Void> changePassword(Authentication authentication,
            @Valid @RequestBody ChangePasswordRequest request) {
        accountPasswordService.changePassword(authentication.getName(), request);
        return ResponseEntity.noContent().build();
    }
}
