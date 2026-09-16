package pl.autoserwis.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import pl.autoserwis.user.AppUser;

import java.io.Serial;
import java.util.Collection;
import java.util.List;

/** Stable account identity; the username is only a login/display value. */
public final class AccountPrincipal extends User {
    @Serial
    private static final long serialVersionUID = 1L;

    private final Long userId;
    private final long sessionVersion;

    public AccountPrincipal(AppUser user) {
        super(user.getUsername(), user.getPasswordHash(), user.isEnabled(), true, true, true,
            List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name())));
        this.userId = user.getId();
        this.sessionVersion = user.getSessionVersion();
    }

    private AccountPrincipal(Long userId, String username, long sessionVersion,
            Collection<? extends GrantedAuthority> authorities) {
        super(username, "", authorities);
        this.userId = userId;
        this.sessionVersion = sessionVersion;
    }

    public Long getUserId() {
        return userId;
    }

    public long getSessionVersion() {
        return sessionVersion;
    }

    public AccountPrincipal withSessionDetails(String username, long version) {
        return new AccountPrincipal(userId, username, version, getAuthorities());
    }
}
