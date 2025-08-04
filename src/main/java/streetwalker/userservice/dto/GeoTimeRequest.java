package streetwalker.userservice.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class GeoTimeRequest {
    private Long userid;
    private String startTime;
    private String endTime;
}
