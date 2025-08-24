package streetwalker.userservice.mvc;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.dao.DataAccessException;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import streetwalker.userservice.controllers.SecurityController;
import streetwalker.userservice.dto.AuthResponse;
import streetwalker.userservice.dto.SigninRequest;
import streetwalker.userservice.dto.SignupRequest;
import streetwalker.userservice.dto.UserDTO;
import streetwalker.userservice.services.UserService;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(SecurityController.class)
@AutoConfigureMockMvc(addFilters = false)
class SecurityControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    // ---------- signup ----------
    @Test
    void signup_Success() throws Exception {
        SignupRequest signupRequest = new SignupRequest("testuser", "test@example.com", "79998887766", "password");

        UserDTO savedUser = new UserDTO();
        savedUser.setUsername("testuser");
        savedUser.setEmail("test@example.com");
        savedUser.setPhone("79998887766");
        savedUser.setRole("USER");
        savedUser.setStatus("Active");

        when(userService.create(any(SignupRequest.class))).thenReturn(savedUser);

        mockMvc.perform(post("/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "testuser",
                                  "email": "test@example.com",
                                  "phone": "79998887766",
                                  "password": "password"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("testuser"))
                .andExpect(jsonPath("$.email").value("test@example.com"))
                .andExpect(jsonPath("$.phone").value("79998887766"))
                .andExpect(jsonPath("$.role").value("USER"))
                .andExpect(jsonPath("$.status").value("Active"));
    }

    @Test
    void signup_Failure() throws Exception {
        when(userService.create(any(SignupRequest.class)))
                .thenThrow(new RuntimeException("User already exists with username: testuser"));

        mockMvc.perform(post("/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"testuser","email":"test@example.com","phone":"79998887766","password":"password"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("User already exists with username: testuser"));
    }

    // ---------- signin ----------
    @Test
    void signin_Success() throws Exception {
        AuthResponse authResponse = new AuthResponse("jwt-token", "refresh-token");
        when(userService.signin(any(SigninRequest.class))).thenReturn(authResponse);

        mockMvc.perform(post("/auth/signin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"phone":"79998887766","password":"password"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("jwt-token"))
                .andExpect(jsonPath("$.refreshToken").value("refresh-token"));
    }

    @Test
    void signin_Failure() throws Exception {
        when(userService.signin(any(SigninRequest.class))).thenThrow(new RuntimeException("Invalid credentials"));

        mockMvc.perform(post("/auth/signin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"phone":"79998887766","password":"wrong"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Invalid credentials"));
    }

    // ---------- refresh ----------
    @Test
    void refresh_Success() throws Exception {
        AuthResponse authResponse = new AuthResponse("new-jwt", "new-refresh");
        when(userService.refresh("valid-token")).thenReturn(authResponse);

        mockMvc.perform(post("/auth/refresh")
                        .param("refreshToken", "valid-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("new-jwt"))
                .andExpect(jsonPath("$.refreshToken").value("new-refresh"));
    }

    @Test
    void refresh_Failure() throws Exception {
        when(userService.refresh("bad-token")).thenThrow(new RuntimeException("Invalid refresh token"));

        mockMvc.perform(post("/auth/refresh")
                        .param("refreshToken", "bad-token"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Invalid refresh token"));
    }

    // ---------- logout ----------
    @Test
    void logout_Success() throws Exception {
        mockMvc.perform(post("/auth/logout")
                        .param("refreshToken", "valid-token"))
                .andExpect(status().isOk())
                .andExpect(content().string("Logged out successfully"));

        verify(userService).logout("valid-token");
    }

    @Test
    void logout_Failure() throws Exception {
        doThrow(new DataAccessException("DB error") {})
                .when(userService).logout("bad-token");

        mockMvc.perform(post("/auth/logout")
                        .param("refreshToken", "bad-token"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("DB error"));
    }
}

