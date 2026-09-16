package pl.autoserwis;

import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.web.context.support.WebApplicationContextUtils;
import pl.autoserwis.security.AccountPrincipal;
import pl.autoserwis.user.AppUser;
import pl.autoserwis.user.UserRepository;
import pl.autoserwis.user.UserRole;

/** Database-backed principals for authorization tests; session tests use the login endpoint. */
public final class DatabaseTestUsers {
    private DatabaseTestUsers() {}

    public static UserFixture databaseUser(String username) {
        return new UserFixture(username);
    }

    public record UserFixture(String username) {
        public RequestPostProcessor roles(String role) {
            return request -> {
                var context = WebApplicationContextUtils.getRequiredWebApplicationContext(request.getServletContext());
                UserRepository users = context.getBean(UserRepository.class);
                // Role-only tests previously used users without a database account.
                AppUser account = users.findByUsernameIgnoreCase(username)
                    .orElseGet(() -> users.saveAndFlush(new AppUser(username,
                        username + "@security-fixture.example", "unused-test-password", UserRole.valueOf(role))));
                if (account.getRole() != UserRole.valueOf(role)) {
                    throw new IllegalArgumentException("Test principal role must match the database account.");
                }
                AccountPrincipal principal = new AccountPrincipal(account);
                principal.eraseCredentials();
                return SecurityMockMvcRequestPostProcessors.user(principal).postProcessRequest(request);
            };
        }
    }
}
