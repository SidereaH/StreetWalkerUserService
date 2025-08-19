package streetwalker.userservice.models.dto;

import lombok.*;

@Getter
@Setter
@Data
@AllArgsConstructor
@NoArgsConstructor
public class SignupRequest {
    private String username;
    private String email;
    private String phone;
    private String password;
}
