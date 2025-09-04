package streetwalker.userservice.models.security;

import jakarta.persistence.PrimaryKeyJoinColumn;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.net.InetAddress;

@PrimaryKeyJoinColumn(name = "action_id")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class UpdatePassAction extends Action {
    private InetAddress initActionIp;
    //html паттерн для передачи в почтовый/смс сервис
    private final String messagePattern = " ";
}
