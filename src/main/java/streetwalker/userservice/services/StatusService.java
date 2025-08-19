package streetwalker.userservice.services;

import org.springframework.stereotype.Service;
import streetwalker.userservice.models.Status;
import streetwalker.userservice.repositories.StatusRepository;

@Service
public class StatusService {
    private final StatusRepository statusRepository;
    public StatusService(StatusRepository statusRepository) {
        this.statusRepository = statusRepository;
    }
    public Status findStatusByName(String name) {
        Status status;
        try{
            status = statusRepository.findByStatusName(name).orElseThrow(() -> new RuntimeException("Status not found"));
            return status;
        } catch (RuntimeException e) {
            return null;
        }
    }
    public Status getDefaultStatus() {
        return statusRepository.findByStatusName("Active").orElseThrow(() -> new RuntimeException("Status not found"));
    }

}
