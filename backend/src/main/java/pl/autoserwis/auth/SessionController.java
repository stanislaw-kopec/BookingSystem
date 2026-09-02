package pl.autoserwis.auth;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/auth")
public class SessionController {
    @GetMapping("/me")
    public SessionResponse session(Authentication authentication) {
        if (authentication == null || authentication instanceof AnonymousAuthenticationToken) {
            return new SessionResponse(null);
        }
        List<String> roles = authentication.getAuthorities().stream()
            .map(authority -> authority.getAuthority())
            .filter(authority -> authority.startsWith("ROLE_"))
            .map(authority -> authority.substring(5))
            .sorted()
            .toList();
        return new SessionResponse(new UserResponse(authentication.getName(), roles));
    }

    @GetMapping("/csrf")
    public CsrfResponse csrf(CsrfToken token) {
        return new CsrfResponse(token.getHeaderName(), token.getToken());
    }

    public record SessionResponse(UserResponse user) {}
    public record UserResponse(String username, List<String> roles) {}
    public record CsrfResponse(String headerName, String token) {}
}
