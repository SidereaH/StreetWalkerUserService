package streetwalker.userservice;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import streetwalker.userservice.controllers.SecurityController;
import streetwalker.userservice.models.RefreshToken;
import streetwalker.userservice.models.Role;
import streetwalker.userservice.models.Status;
import streetwalker.userservice.models.User;
import streetwalker.userservice.models.dto.SignupRequest;
import streetwalker.userservice.repositories.RefreshTokenRepository;
import streetwalker.userservice.repositories.RoleRepository;
import streetwalker.userservice.repositories.StatusRepository;
import streetwalker.userservice.repositories.UserRepository;
import streetwalker.userservice.security.JwtCore;
import streetwalker.userservice.services.RoleService;
import streetwalker.userservice.services.StatusService;

import java.util.Date;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringBootTest
@AutoConfigureMockMvc
class SecurityControllerTest {

    @MockitoBean
    private UserRepository userRepository;


    @MockitoBean
    private RoleService roleService;

    @MockitoBean
    private RoleRepository roleRepository;


    @MockitoBean
    private StatusRepository statusRepository;

    @MockitoBean
    private RefreshTokenRepository refreshTokenRepository;

    @MockitoBean
    private PasswordEncoder passwordEncoder;

    @MockitoBean
    private AuthenticationManager authenticationManager;

    @MockitoBean
    private JwtCore jwtCore;

    @Autowired
    private SecurityController securityController;
    @Autowired
    private StatusService statusService;

    @Test
    void testSignup_Success() {
        SignupRequest signupRequest = new SignupRequest();
        signupRequest.setUsername("testuser");
        signupRequest.setEmail("test@example.com");
        signupRequest.setPhone("79882578790");
        signupRequest.setPassword("password");

        when(userRepository.existsByUsername("testuser")).thenReturn(false);
        when(userRepository.existsByEmail("test@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password")).thenReturn("encodedPassword");
        when(roleService.getDefaultRole()).thenReturn(new Role(1L, "USER", "oh yes"));
        when(statusRepository.findByStatusName("Active")).thenReturn(Optional.of(new Status(1L,"ACTIVE")));
        User savedUser = new User();
        savedUser.setUsername("testuser");
        savedUser.setEmail("test@example.com");
        savedUser.setPhone("79882578790");
        savedUser.setPassword("encodedPassword");
        savedUser.setRole(roleService.getDefaultRole());
        savedUser.setStatus(statusService.getDefaultStatus());
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        ResponseEntity<User> response = (ResponseEntity<User>) securityController.signup(signupRequest);
        System.out.println(response.getBody().getPassword());
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals(savedUser, response.getBody());
    }

    @Test
    void testSignup_UsernameAlreadyExists() {
        SignupRequest signupRequest = new SignupRequest();
        signupRequest.setUsername("existinguser");
        signupRequest.setEmail("test@example.com");
        signupRequest.setPassword("password");

        when(userRepository.existsByUsername("existinguser")).thenReturn(true);

        ResponseEntity<?> response = securityController.signup(signupRequest);
        System.out.println("Response: {}" + response);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("User already exists with username: existinguser", response.getBody());
    }
    @Test
    void testSignup_EmailAlreadyExists() {
        SignupRequest signupRequest = new SignupRequest();
        signupRequest.setUsername("existinguser");
        signupRequest.setEmail("test@example.com");
        signupRequest.setPassword("password");

        when(userRepository.existsByEmail("test@example.com")).thenReturn(true);

        ResponseEntity<?> response = securityController.signup(signupRequest);
        System.out.println("Response: {}" + response);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("User already exists with email: test@example.com", response.getBody());
    }

    @Test
    void testSignup_PhoneAlreadyExists() {
        SignupRequest signupRequest = new SignupRequest();
        signupRequest.setUsername("existinguser");
        signupRequest.setEmail("test@example.com");
        signupRequest.setPhone("79882578790");
        signupRequest.setPassword("password");

        when(userRepository.existsByPhone("79882578790")).thenReturn(true);

        ResponseEntity<?> response = securityController.signup(signupRequest);
        System.out.println("Response: {}" + response);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("User already exists with phone: 79882578790", response.getBody());
    }

    @Test
    void testRefreshToken_InvalidToken() {
        String refreshToken = "invalid-token";
        when(refreshTokenRepository.findByToken(refreshToken)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            securityController.refreshToken(refreshToken);
        });
        assertEquals("Invalid refresh token", exception.getMessage());

        verify(userRepository, never()).findByUsername(any());
        verify(jwtCore, never()).generateToken(any(Authentication.class));
    }

    @Test
    void testRefreshToken_ExpiredToken() {
        String refreshToken = "expired-token";
        RefreshToken storedToken = new RefreshToken();
        storedToken.setToken(refreshToken);
        storedToken.setUsername("testuser");
        storedToken.setExpiryDate(new Date(System.currentTimeMillis() - 1000));

        when(refreshTokenRepository.findByToken(refreshToken)).thenReturn(Optional.of(storedToken));

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            securityController.refreshToken(refreshToken);
        });
        assertEquals("Refresh token expired", exception.getMessage());

        verify(refreshTokenRepository, times(1)).delete(storedToken);
    }

    @Test
    void testRefreshToken_UserNotFound() {
        String refreshToken = "valid-refresh-token";
        RefreshToken storedToken = new RefreshToken();
        storedToken.setToken(refreshToken);
        storedToken.setUsername("testuser");
        storedToken.setExpiryDate(new Date(System.currentTimeMillis() + 100000));

        when(refreshTokenRepository.findByToken(refreshToken)).thenReturn(Optional.of(storedToken));
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            securityController.refreshToken(refreshToken);
        });
        assertEquals("User not found exception", exception.getMessage());
    }
}
