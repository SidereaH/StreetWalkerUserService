package streetwalker.userservice.services;

import org.apache.coyote.BadRequestException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.data.domain.*;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import streetwalker.userservice.dto.*;
import streetwalker.userservice.mappers.UserMapper;
import streetwalker.userservice.models.RefreshToken;
import streetwalker.userservice.models.Status;
import streetwalker.userservice.models.User;
import streetwalker.userservice.repositories.UserRepository;
import streetwalker.userservice.security.JwtCore;
import streetwalker.userservice.services.security.UserActivityLogService;
import streetwalker.userservice.services.util.RequestContextHelper;

import java.util.*;

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
    @Mock private UserActivityLogService activityLogService;
    @InjectMocks private UserService userService;
    @Mock private RequestContextHelper requestContextHelper;

    private User user1;
    private User user2;
    private User user3;
    private User user4;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        user1 = new User();
        user1.setId(1L);
        user1.setUsername("user1");
        user1.setFriends(new HashSet<>());

        user2 = new User();
        user2.setId(2L);
        user2.setUsername("user2");
        user2.setFriends(new HashSet<>());

        user3 = new User();
        user3.setId(3L);
        user3.setUsername("user3");
        user3.setFriends(new HashSet<>());

        user4 = new User();
        user4.setId(4L);
        user4.setUsername("user4");
        user4.setFriends(new HashSet<>());

        when(requestContextHelper.getCurrentRequestIpAddress()).thenReturn("127.0.0.1");
        when(requestContextHelper.getCurrentRequestUserAgent()).thenReturn("test-agent");
    }
    // ===================== findAll =====================
    @Test
    void findAll_shouldReturnPage() {
        Pageable pageable = PageRequest.of(0, 10);
        User user = new User();
        user.setUsername("test");
        user.setEmail("test@test.com");
        user.setStatus(new Status());
        UserDTO userDTO = new UserDTO();
        userDTO.setUsername("test");
        userDTO.setEmail("test@test.com");

        Page<User> users = new PageImpl<>(List.of(user), pageable, 1);

        when(userRepository.findAll(pageable)).thenReturn(users);

        Page<UserDTO> result = userService.findAll(pageable);
        assertEquals(userDTO.getEmail(), users.getContent().get(0).getEmail()) ;
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
        dto.setEmail("new email");
        User user = new User();
        user.setId(1L);
        user.setUsername("siderea");
        user.setEmail("mail");
        user.setPhone("79882578790");
        UserDTO mapped = new UserDTO();
        mapped.setEmail("new email");
        mapped.setPhone("79882578790");
        mapped.setUsername("siderea");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(user);
        when(userMapper.map(any(User.class))).thenReturn(mapped);

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
        User user = new User();
        user.setUsername("u");
        user.setId(1L);

        Authentication auth = mock(Authentication.class);
        RefreshToken refresh = new RefreshToken();
        refresh.setToken("refresh");

        when(userRepository.findByPhone("123")).thenReturn(Optional.of(user));
        when(authenticationManager.authenticate(any())).thenReturn(auth);
        when(jwtCore.generateToken(auth)).thenReturn("jwt");
        when(refreshTokenService.create(user)).thenReturn(refresh);

        // Mock the RequestContextHelper methods
        when(requestContextHelper.getCurrentRequestIpAddress()).thenReturn("192.168.1.1");
        when(requestContextHelper.getCurrentRequestUserAgent()).thenReturn("TestAgent");

        AuthResponse res = userService.signin(req);

        assertEquals("jwt", res.getAccessToken());
        assertEquals("refresh", res.getRefreshToken());

        // Verify activity logging was called
        verify(activityLogService).logLoginSuccess(eq(user), any(UUID.class));
    }

    @Test
    void signin_shouldThrow_whenUserNotFound() {
        SigninRequest req = new SigninRequest("123","pwd");
        when(userRepository.findByPhone("123")).thenReturn(Optional.empty());

        // Mock the RequestContextHelper methods
        when(requestContextHelper.getCurrentRequestIpAddress()).thenReturn("192.168.1.1");
        when(requestContextHelper.getCurrentRequestUserAgent()).thenReturn("TestAgent");

        assertThrows(RuntimeException.class, () -> userService.signin(req));

        // Verify activity logging was called for failure
        verify(activityLogService).logLoginFailure(eq("Phone: 123"), eq("192.168.1.1"), eq("TestAgent"), any(), any());
    }

    @Test
    void signin_shouldThrow_whenBadCredentials() {
        SigninRequest req = new SigninRequest("123","pwd");
        User user = new User(); user.setUsername("u");
        when(requestContextHelper.getCurrentRequestIpAddress()).thenReturn("192.168.1.1");
        when(requestContextHelper.getCurrentRequestUserAgent()).thenReturn("TestAgent");
        when(userRepository.findByPhone("123")).thenReturn(Optional.of(user));
        when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("bad"));

        assertThrows(BadCredentialsException.class, () -> userService.signin(req));
    }

    // ===================== refresh =====================
    @Test
    void refresh_shouldReturnNewTokens_whenValid() {
        User user = new User();
        user.setUsername("u");
        user.setId(1L);

        when(refreshTokenService.check("r")).thenReturn("u");
        when(userRepository.findByUsername("u")).thenReturn(Optional.of(user));
        when(jwtCore.generateToken(user)).thenReturn("newJwt");
        when(refreshTokenService.create(user)).thenReturn(new RefreshToken(0L, "refreshNew", "test_token", new Date()));

        // Mock the RequestContextHelper methods
        when(requestContextHelper.getCurrentRequestIpAddress()).thenReturn("192.168.1.1");
        when(requestContextHelper.getCurrentRequestUserAgent()).thenReturn("TestAgent");

        AuthResponse res = userService.refresh("r");

        assertEquals("newJwt", res.getAccessToken());
        assertEquals("test_token", res.getRefreshToken());

        // Verify activity logging was called
        verify(activityLogService).logUserActivity(any(), any(), eq(user), eq(true), any(), any(), any(), any(), any(), any(), any(), any(), eq("192.168.1.1"), eq("TestAgent"));
    }

    @Test
    void refresh_shouldThrow_whenUserNotFound() {
        when(refreshTokenService.check("r")).thenReturn("u");
        when(userRepository.findByUsername("u")).thenReturn(Optional.empty());

        // Mock the RequestContextHelper methods
        when(requestContextHelper.getCurrentRequestIpAddress()).thenReturn("192.168.1.1");
        when(requestContextHelper.getCurrentRequestUserAgent()).thenReturn("TestAgent");

        assertThrows(RuntimeException.class, () -> userService.refresh("r"));

        // Verify activity logging was called for failure
        verify(activityLogService).logUserActivity(any(), any(), isNull(), eq(false), any(), any(), any(), any(), any(), any(), any(), any(), eq("192.168.1.1"), eq("TestAgent"));
    }

    // ===================== logout =====================
    @Test
    void logout_shouldCallDelete() {
        // Mock the RequestContextHelper methods
        when(requestContextHelper.getCurrentRequestIpAddress()).thenReturn("192.168.1.1");
        when(requestContextHelper.getCurrentRequestUserAgent()).thenReturn("TestAgent");
        when(refreshTokenService.getUsernameFromToken("r")).thenReturn("testuser");

        User user = new User();
        user.setId(1L);
        user.setUsername("testuser");
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));

        userService.logout("r");

        verify(refreshTokenService).delete("r");
        verify(activityLogService).logUserActivity(any(), any(), eq(user), eq(true), any(), any(), any(), any(), any(), any(), any(), any(), eq("192.168.1.1"), eq("TestAgent"));
    }
    // ======================= findAllByUsernameSub =======================

    @Test
    void findAllByUsernameSub_shouldReturnPage_whenFound() {
        Pageable pageable = PageRequest.of(0, 2, Sort.by("username").ascending());
        User user1 = new User();
        user1.setUsername("AliceWonder");
        user1.setEmail("alice@test.com");

        User user3 = new User();
        user3.setUsername("AnotherALICE");
        user3.setEmail("alice2@test.com");

        Page<User> page = new PageImpl<>(List.of(user1, user3));

        when(userRepository.findUserByUsernameContainingIgnoreCase(pageable, "alice")).thenReturn(page);

        // Mock the RequestContextHelper methods
        when(requestContextHelper.getCurrentRequestIpAddress()).thenReturn("192.168.1.1");
        when(requestContextHelper.getCurrentRequestUserAgent()).thenReturn("TestAgent");

        Page<User> result = userService.findAllByUsernameSub(pageable, "alice");

        assertEquals(page, result);
        verify(activityLogService).logUserActivity(any(), any(), isNull(), eq(true), any(), any(), any(), any(), any(), any(), any(), any(), eq("192.168.1.1"), eq("TestAgent"));
    }

    @Test
    void findAllByUsernameSub_shouldThrow_whenNotFound() {
        Pageable pageable = PageRequest.of(0, 2, Sort.by("username").ascending());
        Page<User> emptyPage = new PageImpl<>(List.of());

        when(userRepository.findUserByUsernameContainingIgnoreCase(pageable, "t")).thenReturn(emptyPage);

        // Mock the RequestContextHelper methods
        when(requestContextHelper.getCurrentRequestIpAddress()).thenReturn("192.168.1.1");
        when(requestContextHelper.getCurrentRequestUserAgent()).thenReturn("TestAgent");

        assertThrows(RuntimeException.class, () -> userService.findAllByUsernameSub(pageable, "t"));

        // Verify activity logging was called for failure
        verify(activityLogService).logUserActivity(any(), any(), isNull(), eq(false), any(), any(), any(), any(), any(), any(), any(), any(), eq("192.168.1.1"), eq("TestAgent"));
    }



        @Test
        void addFriend_ShouldAddFriendSuccessfully() {
            // Arrange
            when(userRepository.findById(1L)).thenReturn(Optional.of(user1));
            when(userRepository.findById(2L)).thenReturn(Optional.of(user2));
            when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // Act
            assertDoesNotThrow(() -> userService.addFriend(1L, 2L));

            // Assert
            assertTrue(user1.isFriend(user2));
            assertTrue(user2.isFriend(user1));
            verify(userRepository, times(2)).save(any(User.class));
            verify(activityLogService).logUserActivity(any(), any(), any(), eq(true), any(), any(), any(), any(), any(), any(), any(), any(), any(), any());
        }

        @Test
        void addFriend_ShouldThrowException_WhenUserNotFound() {
            // Arrange
            when(userRepository.findById(1L)).thenReturn(Optional.empty());

            // Act & Assert
            RuntimeException exception = assertThrows(RuntimeException.class,
                    () -> userService.addFriend(1L, 2L));
            assertEquals("User not found with id: 1", exception.getMessage());
        }

        @Test
        void addFriend_ShouldThrowException_WhenFriendNotFound() {
            // Arrange
            when(userRepository.findById(1L)).thenReturn(Optional.of(user1));
            when(userRepository.findById(2L)).thenReturn(Optional.empty());

            // Act & Assert
            RuntimeException exception = assertThrows(RuntimeException.class,
                    () -> userService.addFriend(1L, 2L));
            assertEquals("Friend not found with id: 2", exception.getMessage());
        }

        @Test
        void addFriend_ShouldThrowException_WhenAddingSelf() {
            // Arrange
            when(userRepository.findById(1L)).thenReturn(Optional.of(user1));

            // Act & Assert
            RuntimeException exception = assertThrows(RuntimeException.class,
                    () -> userService.addFriend(1L, 1L));
            assertEquals("Cannot add yourself as a friend", exception.getMessage());
        }

        @Test
        void addFriend_ShouldThrowException_WhenAlreadyFriends() {
            // Arrange
            user1.addFriend(user2);
            when(userRepository.findById(1L)).thenReturn(Optional.of(user1));
            when(userRepository.findById(2L)).thenReturn(Optional.of(user2));

            // Act & Assert
            RuntimeException exception = assertThrows(RuntimeException.class,
                    () -> userService.addFriend(1L, 2L));
            assertEquals("Users are already friends", exception.getMessage());
        }

        @Test
        void removeFriend_ShouldRemoveFriendSuccessfully() {
            // Arrange
            user1.addFriend(user2);
            when(userRepository.findById(1L)).thenReturn(Optional.of(user1));
            when(userRepository.findById(2L)).thenReturn(Optional.of(user2));
            when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // Act
            assertDoesNotThrow(() -> userService.removeFriend(1L, 2L));

            // Assert
            assertFalse(user1.isFriend(user2));
            assertFalse(user2.isFriend(user1));
            verify(userRepository, times(2)).save(any(User.class));
            verify(activityLogService).logUserActivity(any(), any(), any(), eq(true), any(), any(), any(), any(), any(), any(), any(), any(), any(), any());
        }

        @Test
        void removeFriend_ShouldThrowException_WhenNotFriends() {
            // Arrange
            when(userRepository.findById(1L)).thenReturn(Optional.of(user1));
            when(userRepository.findById(2L)).thenReturn(Optional.of(user2));

            // Act & Assert
            RuntimeException exception = assertThrows(RuntimeException.class,
                    () -> userService.removeFriend(1L, 2L));
            assertEquals("Users are not friends", exception.getMessage());
        }

        @Test
        void areFriends_ShouldReturnTrue_WhenUsersAreFriends() {
            // Arrange
            user1.addFriend(user2);
            when(userRepository.findById(1L)).thenReturn(Optional.of(user1));
            when(userRepository.findById(2L)).thenReturn(Optional.of(user2));

            // Act
            boolean result = userService.areFriends(1L, 2L);

            // Assert
            assertTrue(result);
        }

        @Test
        void areFriends_ShouldReturnFalse_WhenUsersAreNotFriends() {
            // Arrange
            when(userRepository.findById(1L)).thenReturn(Optional.of(user1));
            when(userRepository.findById(2L)).thenReturn(Optional.of(user2));

            // Act
            boolean result = userService.areFriends(1L, 2L);

            // Assert
            assertFalse(result);
        }

        @Test
        void areFriends_ShouldThrowException_WhenUserNotFound() {
            // Arrange
            when(userRepository.findById(1L)).thenReturn(Optional.empty());

            // Act & Assert
            RuntimeException exception = assertThrows(RuntimeException.class,
                    () -> userService.areFriends(1L, 2L));
            assertEquals("User not found with id: 1", exception.getMessage());
        }

        @Test
        void getFriends_ShouldReturnEmptyPage_WhenNoFriends() {
            // Arrange
            Pageable pageable = PageRequest.of(0, 10);
            when(userRepository.findById(1L)).thenReturn(Optional.of(user1));
            when(userRepository.findAllByIdIn(anySet(), eq(pageable))).thenReturn(Page.empty());

            // Act
            Page<UserDTO> result = userService.getFriends(1L, pageable);

            // Assert
            assertTrue(result.isEmpty());
//            verify(activityLogService).logUserActivity(any(), any(), any(), eq(true), any(), any(), any(), any(), any(), any(), any(), any(), any(), any());
        }

        @Test
        void getFriends_ShouldReturnFriendsPage() {
            // Arrange
            user1.addFriend(user2);
            user1.addFriend(user3);

            Pageable pageable = PageRequest.of(0, 10);
            List<User> friendsList = Arrays.asList(user2, user3);
            Page<User> friendsPage = new PageImpl<>(friendsList, pageable, 2);

            when(userRepository.findById(1L)).thenReturn(Optional.of(user1));
            when(userRepository.findAllByIdIn(anySet(), eq(pageable))).thenReturn(friendsPage);
            when(userMapper.map(user2)).thenReturn(new UserDTO());
            when(userMapper.map(user3)).thenReturn(new UserDTO());

            // Act
            Page<UserDTO> result = userService.getFriends(1L, pageable);

            // Assert
            assertEquals(2, result.getTotalElements());
            verify(activityLogService).logUserActivity(any(), any(), any(), eq(true), any(), any(), any(), any(), any(), any(), any(), any(), any(), any());
        }

        @Test
        void getMutualFriends_ShouldReturnEmptyPage_WhenNoMutualFriends() {
            // Arrange
            Pageable pageable = PageRequest.of(0, 10);
            when(userRepository.findById(1L)).thenReturn(Optional.of(user1));
            when(userRepository.findById(2L)).thenReturn(Optional.of(user2));
            when(userRepository.findAllByIdIn(anySet(), eq(pageable))).thenReturn(Page.empty());

            // Act
            Page<UserDTO> result = userService.getMutualFriends(1L, 2L, pageable);

            // Assert
            assertTrue(result.isEmpty());
//            verify(activityLogService).logUserActivity(any(), any(), any(), eq(true), any(), any(), any(), any(), any(), any(), any(), any(), any(), any());
        }

        @Test
        void getMutualFriends_ShouldReturnMutualFriendsPage() {
            // Arrange
            user1.addFriend(user3);
            user1.addFriend(user4);
            user2.addFriend(user3);
            user2.addFriend(user4);

            Pageable pageable = PageRequest.of(0, 10);
            List<User> mutualFriendsList = Arrays.asList(user3, user4);
            Page<User> mutualFriendsPage = new PageImpl<>(mutualFriendsList, pageable, 2);

            when(userRepository.findById(1L)).thenReturn(Optional.of(user1));
            when(userRepository.findById(2L)).thenReturn(Optional.of(user2));
            when(userRepository.findAllByIdIn(anySet(), eq(pageable))).thenReturn(mutualFriendsPage);
            when(userMapper.map(user3)).thenReturn(new UserDTO());
            when(userMapper.map(user4)).thenReturn(new UserDTO());

            // Act
            Page<UserDTO> result = userService.getMutualFriends(1L, 2L, pageable);

            // Assert
            assertEquals(2, result.getTotalElements());
            verify(activityLogService).logUserActivity(any(), any(), any(), eq(true), any(), any(), any(), any(), any(), any(), any(), any(), any(), any());
        }

        @Test
        void searchFriends_ShouldReturnEmptyPage_WhenNoFriendsMatchQuery() {
            // Arrange
            user1.addFriend(user2);
            Pageable pageable = PageRequest.of(0, 10);

            when(userRepository.findById(1L)).thenReturn(Optional.of(user1));
            when(userRepository.findByIdInAndUsernameContainingIgnoreCase(anySet(), eq("nonexistent"), eq(pageable)))
                    .thenReturn(Page.empty());

            // Act
            Page<UserDTO> result = userService.searchFriends(1L, "nonexistent", pageable);

            // Assert
            assertTrue(result.isEmpty());
            verify(activityLogService).logUserActivity(any(), any(), any(), eq(true), any(), any(), any(), any(), any(), any(), any(), any(), any(), any());
        }

        @Test
        void searchFriends_ShouldReturnMatchingFriends() {
            // Arrange
            user1.addFriend(user2);
            user1.addFriend(user3);

            Pageable pageable = PageRequest.of(0, 10);
            List<User> matchingFriends = List.of(user2);
            Page<User> friendsPage = new PageImpl<>(matchingFriends, pageable, 1);

            when(userRepository.findById(1L)).thenReturn(Optional.of(user1));
            when(userRepository.findByIdInAndUsernameContainingIgnoreCase(anySet(), eq("user2"), eq(pageable)))
                    .thenReturn(friendsPage);
            when(userMapper.map(user2)).thenReturn(new UserDTO());

            // Act
            Page<UserDTO> result = userService.searchFriends(1L, "user2", pageable);

            // Assert
            assertEquals(1, result.getTotalElements());
            verify(activityLogService).logUserActivity(any(), any(), any(), eq(true), any(), any(), any(), any(), any(), any(), any(), any(), any(), any());
        }

        @Test
        void searchFriends_ShouldReturnEmptyPage_WhenUserHasNoFriends() {
            // Arrange
            Pageable pageable = PageRequest.of(0, 10);

            when(userRepository.findById(1L)).thenReturn(Optional.of(user1));
            when(userRepository.findByIdInAndUsernameContainingIgnoreCase(anySet(), eq("query"), eq(pageable)))
                    .thenReturn(Page.empty());

            // Act
            Page<UserDTO> result = userService.searchFriends(1L, "query", pageable);

            // Assert
            assertTrue(result.isEmpty());
//            verify(activityLogService).logUserActivity(any(), any(), any(), eq(true), any(), any(), any(), any(), any(), any(), any(), any(), any(), any());
        }

        @Test
        void getFriendsCount_ShouldReturnZero_WhenNoFriends() {
            // Arrange
            when(userRepository.findById(1L)).thenReturn(Optional.of(user1));

            // Act
            int result = userService.getFriendsCount(1L);

            // Assert
            assertEquals(0, result);
        }

        @Test
        void getFriendsCount_ShouldReturnCorrectCount() {
            // Arrange
            user1.addFriend(user2);
            user1.addFriend(user3);
            when(userRepository.findById(1L)).thenReturn(Optional.of(user1));

            // Act
            int result = userService.getFriendsCount(1L);

            // Assert
            assertEquals(2, result);
        }

        @Test
        void getFriendSuggestions_ShouldReturnSuggestions() {
            // Arrange
            user1.addFriend(user2); // user2 is already a friend, should be excluded

            Pageable pageable = PageRequest.of(0, 10);
            List<User> suggestions = Arrays.asList(user3, user4);
            Page<User> suggestionsPage = new PageImpl<>(suggestions, pageable, 2);

            when(userRepository.findById(1L)).thenReturn(Optional.of(user1));
            when(userRepository.findByIdNotIn(anySet(), eq(pageable))).thenReturn(suggestionsPage);
            when(userMapper.map(user3)).thenReturn(new UserDTO());
            when(userMapper.map(user4)).thenReturn(new UserDTO());

            // Act
            Page<UserDTO> result = userService.getFriendSuggestions(1L, pageable);

            // Assert
            assertEquals(2, result.getTotalElements());
            verify(userRepository).findByIdNotIn(argThat(set ->
                    set.contains(1L) && set.contains(2L) && set.size() == 2), eq(pageable));
            verify(activityLogService).logUserActivity(any(), any(), any(), eq(true), any(), any(), any(), any(), any(), any(), any(), any(), any(), any());
        }

        @Test
        void getFriendSuggestions_ShouldReturnEmptyPage_WhenNoSuggestions() {
            // Arrange
            Pageable pageable = PageRequest.of(0, 10);

            when(userRepository.findById(1L)).thenReturn(Optional.of(user1));
            when(userRepository.findByIdNotIn(anySet(), eq(pageable))).thenReturn(Page.empty());

            // Act
            Page<UserDTO> result = userService.getFriendSuggestions(1L, pageable);

            // Assert
            assertTrue(result.isEmpty());
            verify(activityLogService).logUserActivity(any(), any(), any(), eq(true), any(), any(), any(), any(), any(), any(), any(), any(), any(), any());
        }

        @Test
        void getFriendSuggestions_ShouldExcludeSelfAndFriends() {
            // Arrange
            user1.addFriend(user2);
            user1.addFriend(user3);

            Pageable pageable = PageRequest.of(0, 10);
            when(userRepository.findById(1L)).thenReturn(Optional.of(user1));
            when(userRepository.findByIdNotIn(anySet(), eq(pageable))).thenReturn(Page.empty());

            // Act
            userService.getFriendSuggestions(1L, pageable);

            // Assert
            verify(userRepository).findByIdNotIn(argThat(excludedIds ->
                    excludedIds.contains(1L) && // self
                            excludedIds.contains(2L) && // friend
                            excludedIds.contains(3L) && // friend
                            excludedIds.size() == 3), eq(pageable));
        }

        @Test
        void getAllFriends_ShouldReturnAllFriends() {
            // Arrange
            user1.addFriend(user2);
            user1.addFriend(user3);

            // This method is tested indirectly through getFriends(), but let's test the User entity method
            Set<User> friends = user1.getAllFriends();

            // Assert
            assertEquals(2, friends.size());
            assertTrue(friends.contains(user2));
            assertTrue(friends.contains(user3));
        }

        @Test
        void getMutualFriends_EntityMethod_ShouldReturnCorrectMutualFriends() {
            // Arrange
            user1.addFriend(user3);
            user1.addFriend(user4);
            user2.addFriend(user3);
            user2.addFriend(user4);
            user2.addFriend(user1); // user1 is friend but not mutual with others

            // Act
            Set<User> mutualFriends = user1.getMutualFriends(user2);

            // Assert
            assertEquals(2, mutualFriends.size());
            assertTrue(mutualFriends.contains(user3));
            assertTrue(mutualFriends.contains(user4));
            assertFalse(mutualFriends.contains(user1));
        }

        @Test
        void isFriend_EntityMethod_ShouldReturnCorrectStatus() {
            // Arrange
            user1.addFriend(user2);

            // Act & Assert
            assertTrue(user1.isFriend(user2));
            assertFalse(user1.isFriend(user3));
        }
    @Test
    void addFriend_ShouldLogActivity() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.of(user1));
        when(userRepository.findById(2L)).thenReturn(Optional.of(user2));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        userService.addFriend(1L, 2L);

        // Assert
        verify(activityLogService).logUserActivity(
                eq(streetwalker.userservice.models.security.ActionType.ADD_FRIEND),
                eq("Add Friend"),
                eq(user1),
                eq(true),
                contains("added user2 as friend"),
                isNull(),
                contains("\"friendId\": 2"),
                eq("User"),
                eq("2"),
                eq(2L),
                eq("USER"),
                any(UUID.class),
                eq("127.0.0.1"),
                eq("test-agent")
        );
    }

    @Test
    void removeFriend_ShouldLogActivity() {
        // Arrange
        user1.addFriend(user2);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user1));
        when(userRepository.findById(2L)).thenReturn(Optional.of(user2));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        userService.removeFriend(1L, 2L);

        // Assert
        verify(activityLogService).logUserActivity(
                eq(streetwalker.userservice.models.security.ActionType.REMOVE_FRIEND),
                eq("Remove Friend"),
                eq(user1),
                eq(true),
                contains("removed user2 from friends"),
                isNull(),
                contains("\"friendId\": 2"),
                eq("User"),
                eq("2"),
                eq(2L),
                eq("USER"),
                any(UUID.class),
                eq("127.0.0.1"),
                eq("test-agent")
        );
    }

}
