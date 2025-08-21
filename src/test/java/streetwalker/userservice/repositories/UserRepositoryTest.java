package streetwalker.userservice.repositories;

import net.bytebuddy.utility.dispatcher.JavaDispatcher;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import streetwalker.userservice.models.User;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
@Testcontainers
class UserRepositoryTest {
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15")

            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }
    @Autowired
    private UserRepository userRepository;

    @Test
    @DisplayName("Должен находить пользователей по подстроке в username без учёта регистра")
    @Rollback(false) // можно убрать, если не нужно сохранять в тестовой БД
    void testFindUserByUsernameContainingIgnoreCase() {
        // arrange
        User user1 = new User();
        user1.setUsername("AliceWonder");
        user1.setEmail("alice@test.com");
        userRepository.save(user1);

        User user2 = new User();
        user2.setUsername("bobBuilder");
        user2.setEmail("bob@test.com");
        userRepository.save(user2);

        User user3 = new User();
        user3.setUsername("AnotherALICE");
        user3.setEmail("alice2@test.com");
        userRepository.save(user3);


        Pageable pageable = PageRequest.of(0, 2, Sort.by("username").ascending());

        // when
        Page<User> result = userRepository.findUserByUsernameContainingIgnoreCase(pageable, "alice");

        // then
        assertEquals(2, result.getTotalElements()); // user1 и user2
        assertEquals(2, result.getContent().size()); // только первая страница
        assertTrue(result.getContent().get(0).getUsername().toLowerCase().contains("alice"));
    }
}
