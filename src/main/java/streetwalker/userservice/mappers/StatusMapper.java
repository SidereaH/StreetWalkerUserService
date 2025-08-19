package streetwalker.userservice.mappers;

import org.springframework.stereotype.Component;
import streetwalker.userservice.models.Status;

@Component
public class StatusMapper {
    public String map(Status status) {
        return status != null ? status.getStatusName() : null;
    }
}

