package streetwalker.userservice;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import streetwalker.userservice.mappers.UserMapper;
import streetwalker.userservice.models.User;
import streetwalker.userservice.models.dto.UserCreateDTO;
import streetwalker.userservice.models.dto.UserDTO;
import streetwalker.userservice.models.dto.UserUpdateDTO;
import streetwalker.userservice.repositories.UserRepository;
import streetwalker.userservice.services.RoleService;
import streetwalker.userservice.services.StatusService;
import streetwalker.userservice.services.UserService;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserMapper userMapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private RoleService roleService;

    @Mock
    private StatusService statusService;

    @InjectMocks
    private UserService userService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    // ===================== findUserById =====================
    @Test
    void findUserById_shouldReturnUser_whenExists() {
        User user = new User();
        user.setId(1L);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        Optional<User> result = userService.findUserById(1L);

        assertTrue(result.isPresent());
        assertEquals(1L, result.get().getId());
        verify(userRepository).findById(1L);
    }

    @Test
    void findUserById_shouldReturnEmpty_whenNotExists() {
        when(userRepository.findById(2L)).thenReturn(Optional.empty());

        Optional<User> result = userService.findUserById(2L);

        assertTrue(result.isEmpty());
        verify(userRepository).findById(2L);
    }

    // ===================== create =====================
    @Test
    void create_shouldSaveUser_whenDataIsValid() {
        UserCreateDTO dto = new UserCreateDTO();
        dto.setUsername("test");
        dto.setEmail("test@test.com");
        dto.setPhone("123456789");
        dto.setPassword("password");

        User user = new User();
        UserDTO userDTO = new UserDTO();

        when(userRepository.existsByUsername(dto.getUsername())).thenReturn(false);
        when(userRepository.existsByEmail(dto.getEmail())).thenReturn(false);
        when(userRepository.existsByPhone(dto.getPhone())).thenReturn(false);
        when(passwordEncoder.encode(dto.getPassword())).thenReturn("encodedPassword");
        when(userMapper.map(dto)).thenReturn(user);
        when(userMapper.map(user)).thenReturn(userDTO);

        UserDTO result = userService.create(dto);

        assertNotNull(result);
        verify(userRepository).save(user);
        verify(passwordEncoder).encode("password");
    }

    @Test
    void create_shouldThrowException_whenUsernameExists() {
        UserCreateDTO dto = new UserCreateDTO();
        dto.setUsername("test");

        when(userRepository.existsByUsername(dto.getUsername())).thenReturn(true);

        RuntimeException exception = assertThrows(RuntimeException.class, () -> userService.create(dto));
        assertTrue(exception.getMessage().contains("User already exists with username"));
        verify(userRepository, never()).save(any());
    }

    // ===================== update =====================
    @Test
    void update_shouldUpdateUser_whenExists() throws Exception {
        UserUpdateDTO dto = new UserUpdateDTO();
        User user = new User();
        UserDTO userDTO = new UserDTO();

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userMapper.map(user)).thenReturn(userDTO);

        UserDTO result = userService.update(dto, 1L);

        assertNotNull(result);
        verify(userRepository).save(user);
        verify(userMapper).update(dto, user);
    }

    @Test
    void update_shouldThrowException_whenUserNotFound() {
        UserUpdateDTO dto = new UserUpdateDTO();

        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(Exception.class, () -> userService.update(dto, 1L));
        verify(userRepository, never()).save(any());
    }
}

