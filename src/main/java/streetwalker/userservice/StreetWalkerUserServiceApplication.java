package streetwalker.userservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
//@ComponentScan({"streetwalker.userservice", "streetwalker.userservice.services.security"})
//@EntityScan(basePackages = "streetwalker.userservice.models")
//@EnableJpaRepositories(basePackages = "streetwalker.userservice.repositories")
public class StreetWalkerUserServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(StreetWalkerUserServiceApplication.class, args);
    }

}
