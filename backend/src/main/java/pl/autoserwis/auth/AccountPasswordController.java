package pl.autoserwis.auth;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import pl.autoserwis.security.AccountPrincipal;
import pl.autoserwis.security.SessionAuthentication;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pl.autoserwis.auth.dto.ChangePasswordRequest;

@RestController
@RequestMapping("/api/auth/password")
public class AccountPasswordController {
    private final AccountPasswordService accountPasswordService;
    private final SessionAuthentication sessions;

    public AccountPasswordController(AccountPasswordService accountPasswordService, SessionAuthentication sessions) {
        this.accountPasswordService = accountPasswordService;
        this.sessions = sessions;
    }

    @PutMapping
    public ResponseEntity<Void> changePassword(@AuthenticationPrincipal AccountPrincipal principal,
            @Valid @RequestBody ChangePasswordRequest request,
            HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        long version = accountPasswordService.changePassword(principal.getUserId(), request);
        sessions.save(principal.withSessionDetails(principal.getUsername(), version), httpRequest, httpResponse);
        return ResponseEntity.noContent().build();
    }
}
