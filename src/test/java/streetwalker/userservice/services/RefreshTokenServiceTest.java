package streetwalker.userservice.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import streetwalker.userservice.models.RefreshToken;
import streetwalker.userservice.models.User;
import streetwalker.userservice.repositories.RefreshTokenRepository;
import streetwalker.userservice.security.JwtCore;

import java.util.Date;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RefreshTokenServiceTest {

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private JwtCore jwtCore;

    @InjectMocks
    private RefreshTokenService refreshTokenService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    // ========== create ==========
    @Test
    void create_shouldSaveAndReturnToken() {
        User user = new User();
        user.setUsername("john");

        RefreshToken token = new RefreshToken();
        token.setUsername("john");
        token.setToken("refresh123");
        token.setExpiryDate(new Date(System.currentTimeMillis() + 10000));

        when(jwtCore.generateRefreshToken("john")).thenReturn("refresh123");
        when(jwtCore.getRefreshTokenLifetime()).thenReturn(10000L);
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenReturn(token);

        RefreshToken result = refreshTokenService.create(user);

        assertEquals("refresh123", result.getToken());
        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    // ========== check ==========
    @Test
    void check_shouldReturnUsername_whenTokenValid() {
        RefreshToken token = new RefreshToken();
        token.setToken("validToken");
        token.setExpiryDate(new Date(System.currentTimeMillis() + 10000));

        when(refreshTokenRepository.findByToken("validToken")).thenReturn(Optional.of(token));
        when(jwtCore.getUserNameFromJwt("validToken")).thenReturn("john");

        String username = refreshTokenService.check("validToken");

        assertEquals("john", username);
    }

    @Test
    void check_shouldThrow_whenTokenNotFound() {
        when(refreshTokenRepository.findByToken("badToken")).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> refreshTokenService.check("badToken"));
    }

    @Test
    void check_shouldThrow_whenTokenExpired() {
        RefreshToken token = new RefreshToken();
        token.setToken("expired");
        token.setExpiryDate(new Date(System.currentTimeMillis() - 1000));

        when(refreshTokenRepository.findByToken("expired")).thenReturn(Optional.of(token));

        assertThrows(RuntimeException.class, () -> refreshTokenService.check("expired"));
        verify(refreshTokenRepository).delete(token);
    }

    // ========== delete ==========
    @Test
    void delete_shouldRemoveByUsername_whenTokenNotNull() {
        when(jwtCore.getUserNameFromJwt("refresh123")).thenReturn("john");

        refreshTokenService.delete("refresh123");

        verify(refreshTokenRepository).deleteByUsername("john");
    }

    @Test
    void delete_shouldDoNothing_whenTokenIsNull() {
        refreshTokenService.delete(null);

        verify(refreshTokenRepository, never()).deleteByUsername(anyString());
    }
}

