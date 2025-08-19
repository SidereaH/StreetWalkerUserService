package streetwalker.userservice.models.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserCreateDTO {
    private String username;
    private String password;
    private String email;
    private String phone;
    private String firstName;
    private String lastName;
    private String bio;
    private String avatarUrl;
}
