package streetwalker.userservice.mvc;


import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.*;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import streetwalker.userservice.controllers.UserController;
import streetwalker.userservice.models.Role;
import streetwalker.userservice.models.Status;
import streetwalker.userservice.dto.UserCreateDTO;
import streetwalker.userservice.dto.UserDTO;
import streetwalker.userservice.models.User;
import streetwalker.userservice.services.UserService;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;


import java.util.Arrays;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
@AutoConfigureMockMvc(addFilters = false)
@WebMvcTest(UserController.class)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;
    @MockitoBean
    private UserService userService;


    @Test
    void testGetUserById() throws Exception {
        User user = new User();
        user.setId(1L);
        user.setUsername("test");
        user.setEmail("test@test.com");
        user.setStatus(new Status(0L, "test"));

        when(userService.findUserById(1L)).thenReturn(Optional.of(user));

        mockMvc.perform(get("/users/1")
                        .with(csrf())
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("test"));
    }

    @Test
    void testCreateUser() throws Exception {
        UserCreateDTO createDTO = new UserCreateDTO();
        createDTO.setUsername("test");
        createDTO.setEmail("test@example.com");
        createDTO.setFirstName("Test");
        createDTO.setLastName("Test");
        createDTO.setBio("Test");



        User user = new User();
        user.setId(1L);
        user.setUsername("test");

        UserDTO userDTO = new UserDTO();
        userDTO.setUsername("test");

        when(userService.create(any(UserCreateDTO.class))).thenReturn(userDTO);

        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDTO)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("test"));
    }
    // =========================== searchUsersByUsername ===========================
        @Test
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
        void searchUsersByUsername_shouldReturnBadRequest_whenServiceThrows() throws Exception {
            when(userService.findAllByUsernameSub(any(Pageable.class), eq("alice")))
                    .thenThrow(new RuntimeException("DB error"));

            mockMvc.perform(get("/users/search/username")
                            .param("username", "alice")
                            .param("page", "0")
                            .param("size", "2")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest())
                    .andExpect(content().string(containsString("DB error")));
        }


}
