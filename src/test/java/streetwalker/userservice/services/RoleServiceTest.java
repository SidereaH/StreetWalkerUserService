package streetwalker.userservice.services;


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import streetwalker.userservice.models.Role;
import streetwalker.userservice.repositories.RoleRepository;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RoleServiceTest {

    @Mock
    private RoleRepository roleRepository;

    private RoleService roleService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        roleService = new RoleService(roleRepository);
    }

    @Test
    void findByName_shouldReturnRole_whenFound() {
        Role role = new Role(1L, "ADMIN", "j est");
        when(roleRepository.findByName("ADMIN")).thenReturn(Optional.of(role));

        Role result = roleService.findByName("ADMIN");

        assertNotNull(result);
        assertEquals("ADMIN", result.getName());
    }

    @Test
    void findByName_shouldReturnNull_whenNotFound() {
        when(roleRepository.findByName("GUEST")).thenReturn(Optional.empty());

        Role result = roleService.findByName("GUEST");

        assertNull(result);
    }

    @Test
    void getDefaultRole_shouldReturnRole_whenExists() {
        Role role = new Role(2L, "USER", "def");
        when(roleRepository.findByName("USER")).thenReturn(Optional.of(role));

        Role result = roleService.getDefaultRole();

        assertEquals("USER", result.getName());
    }

    @Test
    void getDefaultRole_shouldThrow_whenNotFound() {
        when(roleRepository.findByName("USER")).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class, () -> roleService.getDefaultRole());
        assertEquals("Default role not found", ex.getMessage());
    }
}

