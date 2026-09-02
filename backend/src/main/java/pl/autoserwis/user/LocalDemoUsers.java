package pl.autoserwis.user;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Profile("local")
public class LocalDemoUsers implements ApplicationRunner {
    private final UserRepository users;
    private final PasswordEncoder passwords;
    private final String adminPassword;
    private final String mechanicPassword;
    private final String clientPassword;

    public LocalDemoUsers(UserRepository users, PasswordEncoder passwords,
            @Value("${app.demo-users.admin-password}") String adminPassword,
            @Value("${app.demo-users.mechanic-password}") String mechanicPassword,
            @Value("${app.demo-users.client-password}") String clientPassword) {
        this.users = users;
        this.passwords = passwords;
        this.adminPassword = adminPassword;
        this.mechanicPassword = mechanicPassword;
        this.clientPassword = clientPassword;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        createIfMissing("admin", adminPassword, UserRole.ADMIN);
        createIfMissing("mechanic", mechanicPassword, UserRole.MECHANIC);
        createIfMissing("client", clientPassword, UserRole.CLIENT);
    }

    private void createIfMissing(String username, String password, UserRole role) {
        // Restarting the application must not reset an existing account.
        if (users.findByUsernameIgnoreCase(username).isEmpty()) {
            users.save(new AppUser(username, passwords.encode(password), role));
        }
    }
}
