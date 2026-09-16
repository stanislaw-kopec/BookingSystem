package pl.autoserwis.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Component;

@Component
public class SessionAuthentication {
    private final SecurityContextRepository contexts;

    public SessionAuthentication(SecurityContextRepository contexts) {
        this.contexts = contexts;
    }

    public void save(AccountPrincipal principal, HttpServletRequest request, HttpServletResponse response) {
        // Replace the context rather than mutating an object shared by concurrent requests.
        var context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(UsernamePasswordAuthenticationToken.authenticated(
            principal, null, principal.getAuthorities()));
        SecurityContextHolder.setContext(context);
        contexts.saveContext(context, request, response);
    }
}
