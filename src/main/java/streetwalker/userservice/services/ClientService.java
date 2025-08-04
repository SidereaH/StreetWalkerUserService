package streetwalker.userservice.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import streetwalker.userservice.repositories.ClientRepository;

import java.util.List;

@Slf4j
@Service
public class ClientService {
    private final LocationServiceClient locationServiceClient;
    private final ClientRepository clientRepository;

    @Autowired
    public ClientService(LocationServiceClient locationServiceClient, ClientRepository clientRepository) {
        this.locationServiceClient = locationServiceClient;
        this.clientRepository = clientRepository;
    }
    @Transactional
    public Client createClient(Client client) {
        return clientRepository.save(client);
    }
    @Transactional
    public List<Client> getAllClients() {
        return clientRepository.findAll();
    }
    @Transactional(readOnly = true)
    public Client getClientById(Long id) {
        return clientRepository.findById(id).orElse(null);
    }
    @Transactional
    public Client getClientByPhoneNumber(String phoneNumber) {
        return clientRepository.findByPhone(phoneNumber);
    }
    @Transactional(readOnly = true)
    public String getClientEmail(Long id){
        return clientRepository.findById(id).orElseThrow(() -> new RuntimeException("No user with id " + id)).getEmail();
    }
    @Transactional(readOnly = true)
    public Long getClientIdByEmail(String email){
        return clientRepository.findByEmail(email).orElseThrow(() -> new RuntimeException("No user with email " + email)).getId();
    }
    @Transactional
    public Client updateClient(Long id, Client updatedClient) {
        Client existingClient = clientRepository.findById(id).orElse(null);
        if (existingClient == null) {
            return null;
        }

        if (updatedClient.getFirstName() != null && !updatedClient.getFirstName().isEmpty()) {
            existingClient.setFirstName(updatedClient.getFirstName());
        }

        if (updatedClient.getMiddleName() != null && !updatedClient.getMiddleName().isEmpty()) {
            existingClient.setMiddleName(updatedClient.getMiddleName());
        }

        if (updatedClient.getLastName() != null && !updatedClient.getLastName().isEmpty()) {
            existingClient.setLastName(updatedClient.getLastName());
        }

        if (updatedClient.getEmail() != null && !updatedClient.getEmail().isEmpty()) {
            existingClient.setEmail(updatedClient.getEmail());
        }

        if (updatedClient.getPhone() != null && !updatedClient.getPhone().isEmpty()) {
            existingClient.setPhone(updatedClient.getPhone());
        }

        if (updatedClient.getStatus() != null) {
            existingClient.setStatus(updatedClient.getStatus());
        }

        return clientRepository.save(existingClient);
    }
    public boolean deleteClient(Long id) {
        if (clientRepository.existsById(id)) {
            clientRepository.deleteById(id);
            return true;
        }
        return false;
    }

    public void updateClientLocation(Long id, double lat, double lng) {
        log.info("Updating location for client with ID: {}", id);
        locationServiceClient.updateLocation(id, lat, lng);
    }

    public GeoPair getClientLocation(long userId) {
        return locationServiceClient.getLocation(userId);
    }
    public GeoPair[] getLocationHistory(long userId, String startTime, String endTime){

        return locationServiceClient.getLocationHistory(userId, startTime, endTime);
    }








}
