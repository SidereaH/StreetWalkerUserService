package streetwalker.userservice.controllers;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import streetwalker.userservice.dto.GeoTimeRequest;
import streetwalker.userservice.dto.ClientRequest;
import streetwalker.userservice.services.ClientService;
import streetwalker.userservice.services.LocationServiceClient;

import java.util.Arrays;
import java.util.List;
@Slf4j
@RestController
@RequestMapping("/clients")
public class ClientController {

    private final ClientService clientService;
    private final LocationServiceClient locationService;

    @Autowired
    public ClientController(ClientService clientService, LocationServiceClient locationService) {
        this.clientService = clientService;
        this.locationService = locationService;
    }

    @PostMapping
    public ResponseEntity<Client> createClient(@RequestBody Client client) {
        Client savedClient = clientService.createClient(client);
        clientService.updateClientLocation(savedClient.getId(), 0, 0);
        return new ResponseEntity<>(savedClient, HttpStatus.CREATED);
    }
    @GetMapping
    public ResponseEntity<List<Client>> getAllClients() {
        List<Client> clients = clientService.getAllClients();
        return new ResponseEntity<>(clients, HttpStatus.OK);
    }
    @GetMapping("/phone")
    public ResponseEntity<Client> getClientByPhone(@RequestParam String phone) {
        Client client = clientService.getClientByPhoneNumber(phone);
        if (client == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        return new ResponseEntity<>(client, HttpStatus.OK);
    }
    @GetMapping("/{id}")
    public ResponseEntity<Client> getClientById(@PathVariable Long id) {
        Client client = clientService.getClientById(id);
        if (client == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        return new ResponseEntity<>(client, HttpStatus.OK);
    }
    @PutMapping("/{id}")
    public ResponseEntity<Client> updateClient(@PathVariable Long id, @RequestBody ClientRequest client) {
        Client updatedClient = clientService.updateClient(id, client.toClient());
        if (client.getLatitude() == null || client.getLongtitude() == null) {
            return new ResponseEntity<>(updatedClient, HttpStatus.OK);
        }
        if (updatedClient == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        clientService.updateClientLocation(updatedClient.getId(), client.getLatitude(), client.getLongtitude());
        return new ResponseEntity<>(updatedClient, HttpStatus.OK);
    }
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteClient(@PathVariable Long id) {
        boolean isDeleted = clientService.deleteClient(id);
        if (!isDeleted) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
    @PostMapping("/{id}/location")
    public ResponseEntity<Void> updateClientLocation(
            @PathVariable Long id,
            @RequestParam double lat,
            @RequestParam double lng) {
        try {

            clientService.updateClientLocation(id, lat, lng);
            return new ResponseEntity<>(HttpStatus.OK);
        } catch (RuntimeException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }
    @GetMapping("/{id}/mail")
    public ResponseEntity<String> getClientMail(@PathVariable Long id) {
        try{
            return new ResponseEntity<>(clientService.getClientEmail(id), HttpStatus.OK);
        }
        catch (RuntimeException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }
    @GetMapping("/{mail}/id")
    public ResponseEntity<Long> getClientMail(@PathVariable String mail) {
        mail = mail.toLowerCase();
        try{
            return new ResponseEntity<>(clientService.getClientIdByEmail(mail), HttpStatus.OK);
        }
        catch (RuntimeException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    @GetMapping("/{id}/status")
    public ResponseEntity<String> getClientStatus(@PathVariable Long id) {
        try{

            return new ResponseEntity<>(clientService.getClientById(id).getStatus().toString(), HttpStatus.OK);
        }
        catch (RuntimeException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }
    @GetMapping("/{id}/location")
    public ResponseEntity<GeoPair> getClientLocation(@PathVariable Long id) {
        try {
            GeoPair location = clientService.getClientLocation(id);
            return new ResponseEntity<>(location, HttpStatus.OK);
        } catch (RuntimeException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }
    //добавить валидацию через аннотацию
    @GetMapping("/locations")
    public ResponseEntity<GeoPair[]> getLocationHistory(
            @RequestBody GeoTimeRequest timeRequest
    ) {
        log.info("getting locatyions startTime = {}", timeRequest.getStartTime());
        try {
            GeoPair[] location = clientService.getLocationHistory(timeRequest.getUserid(), timeRequest.getStartTime(), timeRequest.getEndTime());
            return new ResponseEntity<>(location, HttpStatus.OK);
        } catch (RuntimeException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }
    @GetMapping("/location/area")
    public ResponseEntity<Long[]> getLocationHistory(@RequestParam double lat,
                                                     @RequestParam double lng,
                                                     @RequestParam int maxUsers){
        return new ResponseEntity<>(Arrays.stream(locationService.getUsersByLatLng(lat,lng, maxUsers)).boxed().toArray(Long[]::new), HttpStatus.OK);
    }

}
