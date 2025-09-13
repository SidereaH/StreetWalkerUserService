package streetwalker.userservice.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import streetwalker.userservice.models.User;
import streetwalker.userservice.services.security.SecurityUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SecurityUtilsTest {

    @InjectMocks
    private SecurityUtils securityUtils;

    @Mock
    private Authentication authentication;

    @Mock
    private SecurityContext securityContext;

    @Test
    void testGetCurrentUser_Authenticated() {
        User user = new User();
        user.setId(1L);
        user.setUsername("test");

        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(user);
        SecurityContextHolder.setContext(securityContext);

        User result = securityUtils.getCurrentUser();

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("test", result.getUsername());
    }

    @Test
    void testGetCurrentUser_NotAuthenticated() {
        when(securityContext.getAuthentication()).thenReturn(null);
        SecurityContextHolder.setContext(securityContext);

        User result = securityUtils.getCurrentUser();

        assertNull(result);
    }

    @Test
    void testIsCurrentUser_True() {
        User user = new User();
        user.setId(1L);

        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(user);
        SecurityContextHolder.setContext(securityContext);

        boolean result = securityUtils.isCurrentUser(1L);

        assertTrue(result);
    }

    @Test
    void testIsCurrentUser_False() {
        User user = new User();
        user.setId(1L);

        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(user);
        SecurityContextHolder.setContext(securityContext);

        boolean result = securityUtils.isCurrentUser(2L);

        assertFalse(result);
    }

    @Test
    void testIsCurrentUser_NotAuthenticated() {
        when(securityContext.getAuthentication()).thenReturn(null);
        SecurityContextHolder.setContext(securityContext);

        boolean result = securityUtils.isCurrentUser(1L);

        assertFalse(result);
    }
}