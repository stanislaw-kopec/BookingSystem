package pl.autoserwis.profile;

import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import pl.autoserwis.security.AccountPrincipal;
import org.springframework.web.bind.annotation.*;
import pl.autoserwis.profile.dto.ClientProfileRequest;
import pl.autoserwis.profile.dto.ClientProfileResponse;

@RestController
@RequestMapping("/api/profile/me")
public class ClientProfileController {
    private final ClientProfileService profiles;

    public ClientProfileController(ClientProfileService profiles) {
        this.profiles = profiles;
    }

    @GetMapping
    public ClientProfileResponse get(@AuthenticationPrincipal AccountPrincipal principal) {
        return profiles.getCurrent(principal.getUserId());
    }

    @PutMapping
    public ClientProfileResponse save(@AuthenticationPrincipal AccountPrincipal principal,
            @Valid @RequestBody ClientProfileRequest request) {
        return profiles.saveCurrent(principal.getUserId(), request);
    }
}
