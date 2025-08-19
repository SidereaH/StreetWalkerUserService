package streetwalker.userservice.services;

import org.springframework.stereotype.Service;
import streetwalker.userservice.models.Role;
import streetwalker.userservice.repositories.RoleRepository;

@Service
public class RoleService {
    private RoleRepository roleRepository;
    public RoleService(RoleRepository roleRepository) {
        this.roleRepository = roleRepository;
    }
    public Role findByName(String roleName) {
        Role role;
        try{
            role = roleRepository.findByName(roleName).orElseThrow(() -> new RuntimeException("Role not found"));
        } catch (RuntimeException e) {
            return null;
        }
        return role;
    }
    public Role getDefaultRole() {
        return roleRepository.findByName("USER").orElseThrow(() -> new RuntimeException("Default role not found"));
    }

}
