package streetwalker.userservice.mvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.*;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import streetwalker.userservice.config.TestSecurityConfig;
import streetwalker.userservice.controllers.UserController;
import streetwalker.userservice.models.Role;
import streetwalker.userservice.models.Status;
import streetwalker.userservice.dto.*;
import streetwalker.userservice.models.User;
import streetwalker.userservice.services.UserService;
import streetwalker.userservice.services.security.SecurityUtils;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

import java.util.Arrays;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

//@AutoConfigureMockMvc(addFilters = false)
@WebMvcTest(UserController.class)
@Import(TestSecurityConfig.class)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private SecurityUtils securityUtils;

    // =========================== getUserById ===========================
    @Test
    @WithMockUser(username = "test", roles = {"USER"})
    void testGetUserById() throws Exception {
        User user = new User();
        user.setId(1L);
        user.setUsername("test");
        user.setEmail("test@test.com");
        user.setStatus(new Status(0L, "test"));

        when(userService.findUserById(1L)).thenReturn(Optional.of(user));

        mockMvc.perform(get("/users/1")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("test"));
    }

    @Test
    @WithMockUser(username = "test", roles = {"USER"})
    void testGetUserById_NotFound() throws Exception {
        when(userService.findUserById(1L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/users/1")
                        .with(csrf()))
                .andExpect(status().isBadRequest());
    }

    // =========================== createUser ===========================
    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void testCreateUser() throws Exception {
        UserCreateDTO createDTO = new UserCreateDTO();
        createDTO.setUsername("test");
        createDTO.setEmail("test@example.com");
        createDTO.setFirstName("Test");
        createDTO.setLastName("Test");
        createDTO.setDescription("Test");

        UserDTO userDTO = new UserDTO();
        userDTO.setUsername("test");

        when(userService.create(any(UserCreateDTO.class))).thenReturn(userDTO);

        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDTO))
                        .with(csrf()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("test"));
    }

    // =========================== updateUser ===========================
    @Test
    @WithMockUser(username = "test", roles = {"USER"})
    void testUpdateUser_Success() throws Exception {
        UserUpdateDTO updateDTO = new UserUpdateDTO();
        updateDTO.setFirstName("Updated");
        updateDTO.setLastName("User");
        updateDTO.setDescription("Updated description");

        UserDTO userDTO = new UserDTO();
        userDTO.setUsername("test");
        userDTO.setFirstName("Updated");
        userDTO.setLastName("User");

        when(securityUtils.isCurrentUser(1L)).thenReturn(true);
        when(userService.update(any(UserUpdateDTO.class), eq(1L))).thenReturn(userDTO);

        mockMvc.perform(put("/users/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDTO))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("test"))
                .andExpect(jsonPath("$.firstName").value("Updated"));
    }

    @Test
    @WithMockUser(username = "other", roles = {"USER"})
    void testUpdateUser_Forbidden() throws Exception {
        UserUpdateDTO updateDTO = new UserUpdateDTO();
        updateDTO.setFirstName("Updated");

        when(securityUtils.isCurrentUser(1L)).thenReturn(false);

        mockMvc.perform(put("/users/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDTO))
                        .with(csrf()))
                .andExpect(status().isForbidden());
    }

    // =========================== searchUsersByUsername ===========================
    @Test
    @WithMockUser(username = "test", roles = {"USER"})
    void searchUsersByUsername_shouldReturnUsers() throws Exception {
        Pageable pageable = PageRequest.of(0, 2, Sort.by("username").ascending());

        User user1 = new User();
        user1.setUsername("AliceWonder");
        user1.setEmail("alice@test.com");
        user1.setStatus(new Status(1L, "ACTIVE"));
        user1.setRole(new Role(1L, "ROLE_ADMIN","ADMIN"));

        User user2 = new User();
        user2.setUsername("AnotherALICE");
        user2.setEmail("alice2@test.com");
        user2.setStatus(new Status(2L, "ACTIVE"));
        user2.setRole(new Role(2L, "ROLE_USER", "default"));

        Page<User> users = new PageImpl<>(Arrays.asList(user1, user2), pageable, 2);

        when(userService.findAllByUsernameSub(any(Pageable.class), eq("alice"))).thenReturn(users);

        mockMvc.perform(get("/users/search/username")
                        .param("username", "alice")
                        .param("page", "0")
                        .param("size", "2")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].username").value("AliceWonder"))
                .andExpect(jsonPath("$.content[1].username").value("AnotherALICE"))
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    @WithMockUser(username = "test", roles = {"USER"})
    void searchUsersByUsername_shouldReturnBadRequest_whenServiceThrows() throws Exception {
        when(userService.findAllByUsernameSub(any(Pageable.class), eq("alice")))
                .thenThrow(new RuntimeException("DB error"));

        mockMvc.perform(get("/users/search/username")
                        .param("username", "alice")
                        .param("page", "0")
                        .param("size", "2")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(content().string(containsString("DB error")));
    }

    // =========================== addFriend ===========================
    @Test
    @WithMockUser(username = "test", roles = {"USER"})
    void testAddFriend_Success() throws Exception {
        when(securityUtils.isCurrentUser(1L)).thenReturn(true);
        doNothing().when(userService).addFriend(1L, 2L);

        mockMvc.perform(post("/users/1/friends/2")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().string("Friend added successfully"));
    }

    @Test
    @WithMockUser(username = "other", roles = {"USER"})
    void testAddFriend_Forbidden() throws Exception {
        when(securityUtils.isCurrentUser(1L)).thenReturn(false);

        mockMvc.perform(post("/users/1/friends/2")
                        .with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "test", roles = {"USER"})
    void testAddFriend_BadRequest() throws Exception {
        when(securityUtils.isCurrentUser(1L)).thenReturn(true);
        doThrow(new RuntimeException("Cannot add friend")).when(userService).addFriend(1L, 2L);

        mockMvc.perform(post("/users/1/friends/2")
                        .with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Cannot add friend"));
    }

    // =========================== removeFriend ===========================
    @Test
    @WithMockUser(username = "test", roles = {"USER"})
    void testRemoveFriend_Success() throws Exception {
        when(securityUtils.isCurrentUser(1L)).thenReturn(true);
        doNothing().when(userService).removeFriend(1L, 2L);

        mockMvc.perform(delete("/users/1/friends/2")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().string("Friend removed successfully"));
    }

    @Test
    @WithMockUser(username = "other", roles = {"USER"})
    void testRemoveFriend_Forbidden() throws Exception {
        when(securityUtils.isCurrentUser(1L)).thenReturn(false);

        mockMvc.perform(delete("/users/1/friends/2")
                        .with(csrf()))
                .andExpect(status().isForbidden());
    }

    // =========================== passwordReset ===========================
    @Test
    @WithMockUser(username = "test", roles = {"USER"})
    void testCreatePasswordResetLink_Success() throws Exception {
        when(securityUtils.isCurrentUser(1L)).thenReturn(true);
        doNothing().when(userService).createNewPassLinkAndSendToMail(1L);

        mockMvc.perform(post("/users/1/password/reset")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().string("Password reset link sent to email"));
    }

    @Test
    @WithMockUser(username = "other", roles = {"USER"})
    void testCreatePasswordResetLink_Forbidden() throws Exception {
        when(securityUtils.isCurrentUser(1L)).thenReturn(false);

        mockMvc.perform(post("/users/1/password/reset")
                        .with(csrf()))
                .andExpect(status().isForbidden());
    }

    // =========================== getAllUsers ===========================
    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void testGetAllUsers_AdminAccess() throws Exception {
        Pageable pageable = PageRequest.of(0, 10);
        UserDTO user = new UserDTO();
        user.setUsername("test");
        user.setEmail("test@test.com");
        user.setStatus("test");
        UserDTO user2 = new UserDTO();
        user2.setUsername("test2");
        user2.setEmail("test2@test.com");
        user2.setStatus("down");

        Page<UserDTO> users = new PageImpl<>(Arrays.asList(user, user2), pageable, 2);
        when(userService.findAll(any(Pageable.class))).thenReturn(users);

        mockMvc.perform(get("/users")
                        .param("page", "0")
                        .param("size", "10")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    @WithMockUser(username = "user", roles = {"USER"})
    void testGetAllUsers_UserAccess() throws Exception {
        // Обычные пользователи также могут видеть список пользователей
        Pageable pageable = PageRequest.of(0, 10);
        UserDTO user = new UserDTO();
        user.setUsername("test");
        user.setEmail("test@test.com");
        user.setStatus("test");
        UserDTO user2 = new UserDTO();
        user2.setUsername("test2");
        user2.setEmail("test2@test.com");
        user2.setStatus("down");

        Page<UserDTO> users = new PageImpl<>(Arrays.asList(user, user2), pageable, 2);
        when(userService.findAll(any(Pageable.class))).thenReturn(users);

        mockMvc.perform(get("/users")
                        .param("page", "0")
                        .param("size", "10")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2));
    }
}