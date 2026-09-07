package pl.autoserwis.auth;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pl.autoserwis.auth.dto.RegistrationRequest;
import pl.autoserwis.auth.dto.RegistrationResponse;
import pl.autoserwis.user.AppUser;

import java.net.URI;

@RestController
@RequestMapping("/api/auth")
public class RegistrationController {
    private final RegistrationService registration;

    public RegistrationController(RegistrationService registration) {
        this.registration = registration;
    }

    @PostMapping("/register")
    public ResponseEntity<RegistrationResponse> register(@Valid @RequestBody RegistrationRequest request) {
        AppUser user = registration.register(request);
        return ResponseEntity.created(URI.create("/api/auth/me"))
            .body(new RegistrationResponse(user.getUsername()));
    }
}
