package streetwalker.userservice.mappers;

import org.springframework.stereotype.Component;
import streetwalker.userservice.models.Role;

@Component
public class RoleMapper {
    public String map(Role role) {
        return role != null ? role.getName() : null;
    }
}
