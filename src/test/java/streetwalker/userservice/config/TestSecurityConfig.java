package streetwalker.userservice.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import streetwalker.userservice.models.User;
import streetwalker.userservice.services.security.SecurityUtils;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@TestConfiguration
public class TestSecurityConfig {

    @Bean
    public SecurityUtils securityUtils() {
        return new SecurityUtils();
    }

    public static void setupSecurityContext(User user) {
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                user, null, user.getAuthorities()
        );
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
    }

    public static void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }
}