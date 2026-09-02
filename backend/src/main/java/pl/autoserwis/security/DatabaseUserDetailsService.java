package pl.autoserwis.security;

import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;
import pl.autoserwis.user.UserRepository;

@Service
public class DatabaseUserDetailsService implements UserDetailsService {
    private final UserRepository users;

    public DatabaseUserDetailsService(UserRepository users) {
        this.users = users;
    }

    @Override
    public UserDetails loadUserByUsername(String username) {
        var user = users.findByUsernameIgnoreCase(username.strip())
            .orElseThrow(() -> new UsernameNotFoundException("Nieprawidłowy login lub hasło."));
        return User.withUsername(user.getUsername())
            .password(user.getPasswordHash())
            .roles(user.getRole().name())
            .build();
    }
}
