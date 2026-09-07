package pl.autoserwis.profile;

import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
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
    public ClientProfileResponse get(Authentication authentication) {
        return profiles.getCurrent(authentication.getName());
    }

    @PutMapping
    public ClientProfileResponse save(Authentication authentication,
            @Valid @RequestBody ClientProfileRequest request) {
        return profiles.saveCurrent(authentication.getName(), request);
    }
}
