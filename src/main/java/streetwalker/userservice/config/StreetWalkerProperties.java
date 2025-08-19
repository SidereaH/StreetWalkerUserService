package streetwalker.userservice.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "walker")
@Getter
@Setter
public class StreetWalkerProperties {
    private String secret;
    private long lifetime;
}