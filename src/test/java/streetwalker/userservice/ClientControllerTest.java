package streetwalker.userservice;


import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import streetwalker.userservice.controllers.ClientController;
import streetwalker.userservice.dto.ClientRequest;
import streetwalker.userservice.services.ClientService;

import java.util.Arrays;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

public class ClientControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Mock
    private ClientService clientService;

    @InjectMocks
    private ClientController clientController;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        mockMvc = MockMvcBuilders.standaloneSetup(clientController).build();
        objectMapper = new ObjectMapper();
    }

    @Test
    void testCreateClient() throws Exception {

        Client client = new Client(null, null,"John", "Doe", "Smith", "john.doe@example.com", "123456789", Status.WATCHER);
        Client savedClient = new Client(1L,1L, "John", "Doe", "Smith", "john.doe@example.com", "123456789", Status.WATCHER);

        when(clientService.createClient(any(Client.class))).thenReturn(savedClient);


        mockMvc.perform(post("/clients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(client)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.firstName").value("John"))
                .andExpect(jsonPath("$.lastName").value("Smith"))
                .andExpect(jsonPath("$.email").value("john.doe@example.com"));
    }

    @Test
    void testGetAllClients() throws Exception {

        Client client1 = new Client(1L,1L, "John", "Doe", "Smith", "john.doe@example.com", "123456789", Status.WATCHER);
        Client client2 = new Client(2L, 2L,"Jane", "Doe", "Smith", "jane.doe@example.com", "987654321",Status.WATCHER);

        when(clientService.getAllClients()).thenReturn(Arrays.asList(client1, client2));


        mockMvc.perform(get("/clients"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[1].id").value(2));
    }

    @Test
    void testGetClientByIdFound() throws Exception {

        Client client = new Client(1L,1L, "John", "Doe", "Smith", "john.doe@example.com", "123456789",Status.WATCHER);

        when(clientService.getClientById(1L)).thenReturn(client);


        mockMvc.perform(get("/clients/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.firstName").value("John"))
                .andExpect(jsonPath("$.lastName").value("Smith"));
    }

    @Test
    void testGetClientByIdNotFound() throws Exception {

        when(clientService.getClientById(1L)).thenReturn(null);


        mockMvc.perform(get("/clients/1"))
                .andExpect(status().isNotFound());
    }

    @Test
    void testUpdateClient() throws Exception {
        ClientRequest updatedClientRequest = new ClientRequest(1L, 1L,"John", "Doe", "Smith",
                "john.doe@example.com", "123456789", Status.WATCHER, 10.0, 20.0);
        Client expectedClient = updatedClientRequest.toClient();

        when(clientService.updateClient(eq(1L), any(Client.class))).thenReturn(expectedClient);
        doNothing().when(clientService).updateClientLocation(eq(1L), eq(10.0), eq(20.0));

        mockMvc.perform(put("/clients/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updatedClientRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.firstName").value("John"))
                .andExpect(jsonPath("$.lastName").value("Smith"));

        verify(clientService).updateClient(eq(1L), any(Client.class));
        verify(clientService).updateClientLocation(eq(1L), eq(10.0), eq(20.0));
    }



    @Test
    void testDeleteClient() throws Exception {

        when(clientService.deleteClient(1L)).thenReturn(true);


        mockMvc.perform(delete("/clients/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    void testDeleteClientNotFound() throws Exception {

        when(clientService.deleteClient(1L)).thenReturn(false);


        mockMvc.perform(delete("/clients/1"))
                .andExpect(status().isNotFound());
    }
}
