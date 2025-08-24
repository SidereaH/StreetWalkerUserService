package streetwalker.userservice.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserUpdateDTO {
    private String username;
    private String email;
    private String phone;
    private String firstName;
    private String lastName;
    private String description;
    private String avatarUrl;
}
