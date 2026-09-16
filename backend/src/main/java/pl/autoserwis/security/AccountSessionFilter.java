package pl.autoserwis.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.web.filter.OncePerRequestFilter;
import pl.autoserwis.exception.ApiErrorCode;
import pl.autoserwis.user.AppUser;
import pl.autoserwis.user.UserRepository;

import java.io.IOException;

/** Checks stored sessions against current account state before CSRF and authorization. */
public class AccountSessionFilter extends OncePerRequestFilter {
    private final UserRepository users;
    private final SessionAuthentication sessions;
    private final SecurityContextLogoutHandler logout = new SecurityContextLogoutHandler();

    public AccountSessionFilter(UserRepository users, SessionAuthentication sessions) {
        this.users = users;
        this.sessions = sessions;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain chain) throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken)) {
            AccountPrincipal principal = authentication.getPrincipal() instanceof AccountPrincipal account
                ? account : null;
            AppUser user = principal == null ? null : users.findById(principal.getUserId()).orElse(null);
            if (user == null || !user.isEnabled()
                    || user.getSessionVersion() != principal.getSessionVersion()
                    || authentication.getAuthorities().stream()
                        .noneMatch(authority -> authority.getAuthority().equals("ROLE_" + user.getRole().name()))) {
                logout.logout(request, response, authentication);
                SecurityConfig.writeError(response, 401, ApiErrorCode.UNAUTHENTICATED,
                    "The session is no longer valid. Sign in again.");
                return;
            }
            if (!principal.getUsername().equals(user.getUsername())) {
                sessions.save(principal.withSessionDetails(user.getUsername(), user.getSessionVersion()),
                    request, response);
            }
        }
        chain.doFilter(request, response);
    }
}
