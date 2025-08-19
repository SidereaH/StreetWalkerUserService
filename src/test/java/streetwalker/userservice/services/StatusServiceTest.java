package streetwalker.userservice.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import streetwalker.userservice.models.Status;
import streetwalker.userservice.repositories.StatusRepository;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class StatusServiceTest {

    @Mock
    private StatusRepository statusRepository;

    private StatusService statusService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        statusService = new StatusService(statusRepository);
    }

    @Test
    void findStatusByName_shouldReturnStatus_whenFound() {
        Status status = new Status(1L, "Active");
        when(statusRepository.findByStatusName("Active")).thenReturn(Optional.of(status));

        Status result = statusService.findStatusByName("Active");

        assertNotNull(result);
        assertEquals("Active", result.getStatusName());
    }

    @Test
    void findStatusByName_shouldReturnNull_whenNotFound() {
        when(statusRepository.findByStatusName("Inactive")).thenReturn(Optional.empty());

        Status result = statusService.findStatusByName("Inactive");

        assertNull(result);
    }

    @Test
    void getDefaultStatus_shouldReturnStatus_whenExists() {
        Status status = new Status(2L, "Active");
        when(statusRepository.findByStatusName("Active")).thenReturn(Optional.of(status));

        Status result = statusService.getDefaultStatus();

        assertEquals("Active", result.getStatusName());
    }

    @Test
    void getDefaultStatus_shouldThrow_whenNotFound() {
        when(statusRepository.findByStatusName("Active")).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class, () -> statusService.getDefaultStatus());
        assertEquals("Status not found", ex.getMessage());
    }
}
