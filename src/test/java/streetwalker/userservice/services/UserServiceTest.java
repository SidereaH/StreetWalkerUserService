package streetwalker.userservice.services;

import org.apache.coyote.BadRequestException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import streetwalker.userservice.mappers.UserMapper;
import streetwalker.userservice.models.RefreshToken;
import streetwalker.userservice.models.User;
import streetwalker.userservice.models.dto.*;
import streetwalker.userservice.repositories.UserRepository;
import streetwalker.userservice.security.JwtCore;

import java.util.Date;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class UserServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private UserMapper userMapper;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private RoleService roleService;
    @Mock private StatusService statusService;
    @Mock private AuthenticationManager authenticationManager;
    @Mock private RefreshTokenService refreshTokenService;
    @Mock private JwtCore jwtCore;

    @InjectMocks private UserService userService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    // ===================== findAll =====================
    @Test
    void findAll_shouldReturnPage() {
        Pageable pageable = mock(Pageable.class);
        Page<User> page = mock(Page.class);

        when(userRepository.findAll(pageable)).thenReturn(page);

        Page<User> result = userService.findAll(pageable);

        assertEquals(page, result);
        verify(userRepository).findAll(pageable);
    }

    // ===================== loadUserByUsername =====================
    @Test
    void loadUserByUsername_shouldReturnUser_whenExists() {
        User user = new User();
        when(userRepository.findByUsername("test")).thenReturn(Optional.of(user));

        assertEquals(user, userService.loadUserByUsername("test"));
    }

    @Test
    void loadUserByUsername_shouldThrow_whenNotFound() {
        when(userRepository.findByUsername("missing")).thenReturn(Optional.empty());

        assertThrows(UsernameNotFoundException.class, () -> userService.loadUserByUsername("missing"));
    }

    // ===================== create(SignupRequest) =====================
    @Test
    void create_shouldSaveUser_whenValid() {
        SignupRequest dto = new SignupRequest("test","test@test.com","123","password");
        User user = new User();
        UserDTO mapped = new UserDTO();

        when(userRepository.existsByUsername(dto.getUsername())).thenReturn(false);
        when(userRepository.existsByEmail(dto.getEmail())).thenReturn(false);
        when(userRepository.existsByPhone(dto.getPhone())).thenReturn(false);
        when(passwordEncoder.encode(dto.getPassword())).thenReturn("encoded");
        when(userMapper.map(dto)).thenReturn(user);
        when(userMapper.map(user)).thenReturn(mapped);

        UserDTO result = userService.create(dto);

        assertEquals(mapped, result);
        verify(userRepository).save(user);
    }

    @Test
    void create_shouldThrow_whenUsernameExists() {
        SignupRequest dto = new SignupRequest("test","e","p","x");
        when(userRepository.existsByUsername("test")).thenReturn(true);

        assertThrows(RuntimeException.class, () -> userService.create(dto));
    }

    @Test
    void create_shouldThrow_whenEmailExists() {
        SignupRequest dto = new SignupRequest("test","e","p","x");
        when(userRepository.existsByUsername("test")).thenReturn(false);
        when(userRepository.existsByEmail("e")).thenReturn(true);

        assertThrows(RuntimeException.class, () -> userService.create(dto));
    }

    @Test
    void create_shouldThrow_whenPhoneExists() {
        SignupRequest dto = new SignupRequest("test","e","p","x");
        when(userRepository.existsByUsername("test")).thenReturn(false);
        when(userRepository.existsByEmail("e")).thenReturn(false);
        when(userRepository.existsByPhone("p")).thenReturn(true);

        assertThrows(RuntimeException.class, () -> userService.create(dto));
    }

    // ===================== update =====================
    @Test
    void update_shouldUpdateUser_whenExists() throws Exception {
        UserUpdateDTO dto = new UserUpdateDTO();
        User user = new User();
        UserDTO mapped = new UserDTO();

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userMapper.map(user)).thenReturn(mapped);

        UserDTO result = userService.update(dto, 1L);

        assertEquals(mapped, result);
        verify(userRepository).save(user);
        verify(userMapper).update(dto, user);
    }

    @Test
    void update_shouldThrow_whenNotFound() {
        UserUpdateDTO dto = new UserUpdateDTO();
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(BadRequestException.class, () -> userService.update(dto, 1L));
    }

    // ===================== signin =====================
    @Test
    void signin_shouldReturnTokens_whenValid() {
        SigninRequest req = new SigninRequest("123","pwd");
        User user = new User(); user.setUsername("u");
        Authentication auth = mock(Authentication.class);
        RefreshToken refresh = new RefreshToken(); refresh.setToken("refresh");
        when(userRepository.findByPhone("123")).thenReturn(Optional.of(user));
        when(authenticationManager.authenticate(any())).thenReturn(auth);
        when(jwtCore.generateToken(auth)).thenReturn("jwt");
        when(refreshTokenService.create(user)).thenReturn(refresh);

        AuthResponse res = userService.signin(req);

        assertEquals("jwt", res.getAccessToken());
        assertEquals("refresh", res.getRefreshToken());
    }

    @Test
    void signin_shouldThrow_whenUserNotFound() {
        SigninRequest req = new SigninRequest("123","pwd");
        when(userRepository.findByPhone("123")).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> userService.signin(req));
    }

    @Test
    void signin_shouldThrow_whenBadCredentials() {
        SigninRequest req = new SigninRequest("123","pwd");
        User user = new User(); user.setUsername("u");
        when(userRepository.findByPhone("123")).thenReturn(Optional.of(user));
        when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("bad"));

        assertThrows(BadCredentialsException.class, () -> userService.signin(req));
    }

    // ===================== refresh =====================
    @Test
    void refresh_shouldReturnNewTokens_whenValid() {
        User user = new User(); user.setUsername("u");
        when(refreshTokenService.check("r")).thenReturn("u");
        when(userRepository.findByUsername("u")).thenReturn(Optional.of(user));
        when(jwtCore.generateToken(user)).thenReturn("newJwt");
        when(refreshTokenService.create(user)).thenReturn(new RefreshToken(0L, "refreshNew", "test_token", new Date()));

        AuthResponse res = userService.refresh("r");

        assertEquals("newJwt", res.getAccessToken());
        assertEquals("test_token", res.getRefreshToken());
    }

    @Test
    void refresh_shouldThrow_whenUserNotFound() {
        when(refreshTokenService.check("r")).thenReturn("u");
        when(userRepository.findByUsername("u")).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> userService.refresh("r"));
    }

    // ===================== logout =====================
    @Test
    void logout_shouldCallDelete() {
        userService.logout("r");
        verify(refreshTokenService).delete("r");
    }
}
