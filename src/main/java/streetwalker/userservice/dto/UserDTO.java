package streetwalker.userservice.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserDTO{
    private String username;
    private String email;
    private String phone;
    private String firstName;
    private String lastName;
    private String bio;
    private String avatarUrl;
    private String role;
    private String status;
}
