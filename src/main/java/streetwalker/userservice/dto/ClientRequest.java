package streetwalker.userservice.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter

public class ClientRequest {
    private Long id;
    private Long userId;
    private String firstName;
    private String middleName;
    private String lastName;
    private String email;
    private String phone;
    private Status status;
    private Double latitude;
    private Double longtitude;

    public Client toClient() {
        return new Client(id, userId, firstName, middleName, lastName, email, phone, status);
    }

    public ClientRequest(Long id, String firstName, String middleName, String lastName, String email, String phone, Status status, Double latitude, Double longtitude) {
        this.id = id;
        this.firstName = firstName;
        this.middleName = middleName;
        this.lastName = lastName;
        this.email = email;
        this.phone = phone;
        this.status = status;
        this.latitude = latitude;
        this.longtitude = longtitude;
    }
    public ClientRequest(Long id,Long userId, String firstName, String middleName, String lastName, String email, String phone, Status status, Double latitude, Double longtitude) {
        this.id = id;
        this.userId = userId;
        this.firstName = firstName;
        this.middleName = middleName;
        this.lastName = lastName;
        this.email = email;
        this.phone = phone;
        this.status = status;
        this.latitude = latitude;
        this.longtitude = longtitude;
    }
}
