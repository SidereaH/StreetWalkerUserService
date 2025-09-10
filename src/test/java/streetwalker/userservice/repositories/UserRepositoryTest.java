package streetwalker.userservice.repositories;

import org.junit.jupiter.api.BeforeEach;
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

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
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

    private User user1;
    private User user2;
    private User user3;
    private User user4;
    private User user5;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();

        user1 = new User();
        user1.setUsername("AliceWonder");
        user1.setEmail("alice@test.com");
        user1.setPhone("+1234567890");
        user1 = userRepository.save(user1);

        user2 = new User();
        user2.setUsername("bobBuilder");
        user2.setEmail("bob@test.com");
        user2.setPhone("+0987654321");
        user2 = userRepository.save(user2);

        user3 = new User();
        user3.setUsername("AnotherALICE");
        user3.setEmail("alice2@test.com");
        user3.setPhone("+1111111111");
        user3 = userRepository.save(user3);

        user4 = new User();
        user4.setUsername("charlieBrown");
        user4.setEmail("charlie@test.com");
        user4.setPhone("+2222222222");
        user4 = userRepository.save(user4);

        user5 = new User();
        user5.setUsername("davidSmith");
        user5.setEmail("david@test.com");
        user5.setPhone("+3333333333");
        user5 = userRepository.save(user5);
    }

    @Test
    @DisplayName("Должен находить пользователей по подстроке в username без учёта регистра")
    void testFindUserByUsernameContainingIgnoreCase() {
        // arrange
        Pageable pageable = PageRequest.of(0, 10, Sort.by("username").ascending());

        // when
        Page<User> result = userRepository.findUserByUsernameContainingIgnoreCase(pageable, "alice");

        // then
        assertEquals(2, result.getTotalElements());
        assertEquals(2, result.getContent().size());
        assertTrue(result.getContent().stream()
                .allMatch(user -> user.getUsername().toLowerCase().contains("alice")));
    }

    @Test
    @DisplayName("findAllByIdIn - должен возвращать страницу пользователей по списку ID")
    void testFindAllByIdIn() {
        // arrange
        Set<Long> ids = new HashSet<>(Arrays.asList(user1.getId(), user2.getId(), user3.getId()));
        Pageable pageable = PageRequest.of(0, 10);

        // when
        Page<User> result = userRepository.findAllByIdIn(ids, pageable);

        // then
        assertEquals(3, result.getTotalElements());
        assertEquals(3, result.getContent().size());
        assertThat(result.getContent())
                .extracting(User::getId)
                .containsExactlyInAnyOrder(user1.getId(), user2.getId(), user3.getId());
    }

    @Test
    @DisplayName("findAllByIdIn - должен возвращать пустую страницу при пустом списке ID")
    void testFindAllByIdIn_EmptyIds() {
        // arrange
        Set<Long> ids = new HashSet<>();
        Pageable pageable = PageRequest.of(0, 10);

        // when
        Page<User> result = userRepository.findAllByIdIn(ids, pageable);

        // then
        assertTrue(result.isEmpty());
        assertEquals(0, result.getTotalElements());
    }

    @Test
    @DisplayName("findAllByIdIn - должен возвращать пустую страницу при несуществующих ID")
    void testFindAllByIdIn_NonExistentIds() {
        // arrange
        Set<Long> ids = new HashSet<>(Arrays.asList(999L, 1000L));
        Pageable pageable = PageRequest.of(0, 10);

        // when
        Page<User> result = userRepository.findAllByIdIn(ids, pageable);

        // then
        assertTrue(result.isEmpty());
        assertEquals(0, result.getTotalElements());
    }

    @Test
    @DisplayName("findAllByIdIn - должен поддерживать пагинацию")
    void testFindAllByIdIn_WithPagination() {
        // arrange
        Set<Long> ids = new HashSet<>(Arrays.asList(user1.getId(), user2.getId(), user3.getId(), user4.getId()));
        Pageable firstPage = PageRequest.of(0, 2);
        Pageable secondPage = PageRequest.of(1, 2);

        // when
        Page<User> firstPageResult = userRepository.findAllByIdIn(ids, firstPage);
        Page<User> secondPageResult = userRepository.findAllByIdIn(ids, secondPage);

        // then
        assertEquals(4, firstPageResult.getTotalElements());
        assertEquals(2, firstPageResult.getNumberOfElements());
        assertEquals(2, secondPageResult.getNumberOfElements());
    }

    @Test
    @DisplayName("findByIdInAndUsernameContainingIgnoreCase - должен возвращать пользователей по ID и подстроке username")
    void testFindByIdInAndUsernameContainingIgnoreCase() {
        // arrange
        Set<Long> ids = new HashSet<>(Arrays.asList(user1.getId(), user2.getId(), user3.getId()));
        Pageable pageable = PageRequest.of(0, 10);

        // when
        Page<User> result = userRepository.findByIdInAndUsernameContainingIgnoreCase(ids, "alice", pageable);

        // then
        assertEquals(2, result.getTotalElements());
        assertEquals(2, result.getContent().size());
        assertThat(result.getContent())
                .extracting(User::getId)
                .containsExactlyInAnyOrder(user1.getId(), user3.getId());
        assertTrue(result.getContent().stream()
                .allMatch(user -> user.getUsername().toLowerCase().contains("alice")));
    }

    @Test
    @DisplayName("findByIdInAndUsernameContainingIgnoreCase - должен возвращать пустой результат при отсутствии совпадений")
    void testFindByIdInAndUsernameContainingIgnoreCase_NoMatches() {
        // arrange
        Set<Long> ids = new HashSet<>(Arrays.asList(user1.getId(), user2.getId()));
        Pageable pageable = PageRequest.of(0, 10);

        // when
        Page<User> result = userRepository.findByIdInAndUsernameContainingIgnoreCase(ids, "nonexistent", pageable);

        // then
        assertTrue(result.isEmpty());
        assertEquals(0, result.getTotalElements());
    }

    @Test
    @DisplayName("findByIdInAndUsernameContainingIgnoreCase - должен возвращать пустой результат при пустом списке ID")
    void testFindByIdInAndUsernameContainingIgnoreCase_EmptyIds() {
        // arrange
        Set<Long> ids = new HashSet<>();
        Pageable pageable = PageRequest.of(0, 10);

        // when
        Page<User> result = userRepository.findByIdInAndUsernameContainingIgnoreCase(ids, "alice", pageable);

        // then
        assertTrue(result.isEmpty());
        assertEquals(0, result.getTotalElements());
    }

    @Test
    @DisplayName("findByIdNotIn - должен возвращать пользователей, не входящих в список ID")
    void testFindByIdNotIn() {
        // arrange
        Set<Long> excludedIds = new HashSet<>(Arrays.asList(user1.getId(), user2.getId()));
        Pageable pageable = PageRequest.of(0, 10);

        // when
        Page<User> result = userRepository.findByIdNotIn(excludedIds, pageable);

        // then
        assertEquals(3, result.getTotalElements()); // user3, user4, user5
        assertEquals(3, result.getContent().size());
        assertThat(result.getContent())
                .extracting(User::getId)
                .containsExactlyInAnyOrder(user3.getId(), user4.getId(), user5.getId())
                .doesNotContain(user1.getId(), user2.getId());
    }



    @Test
    @DisplayName("findByIdNotIn - должен возвращать пустой результат при исключении всех пользователей")
    void testFindByIdNotIn_ExcludeAll() {
        // arrange
        Set<Long> excludedIds = new HashSet<>(Arrays.asList(
                user1.getId(), user2.getId(), user3.getId(), user4.getId(), user5.getId()
        ));
        Pageable pageable = PageRequest.of(0, 10);

        // when
        Page<User> result = userRepository.findByIdNotIn(excludedIds, pageable);

        // then
        assertTrue(result.isEmpty());
        assertEquals(0, result.getTotalElements());
    }

    @Test
    @DisplayName("findByIdNotIn - должен поддерживать пагинацию")
    void testFindByIdNotIn_WithPagination() {
        // arrange
        Set<Long> excludedIds = new HashSet<>(Arrays.asList(user1.getId()));
        Pageable firstPage = PageRequest.of(0, 2);
        Pageable secondPage = PageRequest.of(1, 2);

        // when
        Page<User> firstPageResult = userRepository.findByIdNotIn(excludedIds, firstPage);
        Page<User> secondPageResult = userRepository.findByIdNotIn(excludedIds, secondPage);

        // then
        assertEquals(4, firstPageResult.getTotalElements()); // user2, user3, user4, user5
        assertEquals(2, firstPageResult.getNumberOfElements());
        assertEquals(2, secondPageResult.getNumberOfElements());
    }

    @Test
    @DisplayName("findById - должен возвращать пользователя по ID")
    void testFindById() {
        // when
        Optional<User> result = userRepository.findById(user1.getId());

        // then
        assertTrue(result.isPresent());
        assertEquals(user1.getId(), result.get().getId());
        assertEquals("AliceWonder", result.get().getUsername());
    }

    @Test
    @DisplayName("findById - должен возвращать пустой Optional при несуществующем ID")
    void testFindById_NonExistent() {
        // when
        Optional<User> result = userRepository.findById(999L);

        // then
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("Комбинированный тест - поиск друзей с фильтрацией")
    void testCombinedFriendSearch() {
        // arrange
        // Создаем дружеские связи
        user1.getFriends().add(user2);
        user1.getFriends().add(user3);
        user1.getFriends().add(user4);
        userRepository.save(user1);

        Set<Long> friendIds = new HashSet<>(Arrays.asList(user2.getId(), user3.getId(), user4.getId()));
        Pageable pageable = PageRequest.of(0, 10);

        // when - ищем друзей с подстрокой "a" в username
        Page<User> result = userRepository.findByIdInAndUsernameContainingIgnoreCase(friendIds, "a", pageable);

        // then - user2 (bobBuilder) не должен быть найден, так как не содержит "a"
        assertEquals(2, result.getTotalElements()); // user3 (AnotherALICE) и user4 (charlieBrown)
        assertThat(result.getContent())
                .extracting(User::getId)
                .containsExactlyInAnyOrder(user3.getId(), user4.getId())
                .doesNotContain(user2.getId());
    }

    @Test
    @DisplayName("Комбинированный тест - рекомендации друзей с исключениями")
    void testCombinedFriendSuggestions() {
        // arrange
        // Пользователь уже дружит с user2 и user3
        Set<Long> excludedIds = new HashSet<>(Arrays.asList(
                user1.getId(), // исключаем себя
                user2.getId(), // исключаем друга 1
                user3.getId()  // исключаем друга 2
        ));
        Pageable pageable = PageRequest.of(0, 10);

        // when - получаем рекомендации
        Page<User> result = userRepository.findByIdNotIn(excludedIds, pageable);

        // then - должны получить user4 и user5
        assertEquals(2, result.getTotalElements());
        assertThat(result.getContent())
                .extracting(User::getId)
                .containsExactlyInAnyOrder(user4.getId(), user5.getId())
                .doesNotContain(user1.getId(), user2.getId(), user3.getId());
    }

    @Test
    @DisplayName("Комбинированный тест - пагинация и сортировка")
    void testPaginationAndSorting() {
        // arrange
        Set<Long> allIds = new HashSet<>(Arrays.asList(
                user1.getId(), user2.getId(), user3.getId(), user4.getId(), user5.getId()
        ));
        Pageable pageableWithSort = PageRequest.of(0, 3, Sort.by("username").ascending());

        // when
        Page<User> result = userRepository.findAllByIdIn(allIds, pageableWithSort);

        // then
        assertEquals(5, result.getTotalElements());
        assertEquals(3, result.getNumberOfElements());

        // Проверяем сортировку по username
        List<User> content = result.getContent();
        assertTrue(content.get(0).getUsername().compareTo(content.get(1).getUsername()) <= 0);
        assertTrue(content.get(1).getUsername().compareTo(content.get(2).getUsername()) <= 0);
    }

    @Test
    @DisplayName("Комбинированный тест - поиск по несуществующей подстроке среди существующих ID")
    void testSearchNonExistentSubstringInExistingIds() {
        // arrange
        Set<Long> existingIds = new HashSet<>(Arrays.asList(user1.getId(), user2.getId(), user3.getId()));
        Pageable pageable = PageRequest.of(0, 10);

        // when
        Page<User> result = userRepository.findByIdInAndUsernameContainingIgnoreCase(
                existingIds, "xyz", pageable);

        // then
        assertTrue(result.isEmpty());
        assertEquals(0, result.getTotalElements());
    }

    @Test
    @DisplayName("Комбинированный тест - поиск по существующей подстроке среди несуществующих ID")
    void testSearchExistingSubstringInNonExistentIds() {
        // arrange
        Set<Long> nonExistentIds = new HashSet<>(Arrays.asList(999L, 1000L));
        Pageable pageable = PageRequest.of(0, 10);

        // when
        Page<User> result = userRepository.findByIdInAndUsernameContainingIgnoreCase(
                nonExistentIds, "alice", pageable);

        // then
        assertTrue(result.isEmpty());
        assertEquals(0, result.getTotalElements());
    }


    @Test
    @DisplayName("findByIdInAndUsernameContainingIgnoreCase - чувствительность к регистру")
    void testCaseInsensitiveSearch() {
        // arrange
        Set<Long> ids = new HashSet<>(Arrays.asList(user1.getId(), user2.getId(), user3.getId()));
        Pageable pageable = PageRequest.of(0, 10);

        // when - поиск с разным регистром
        Page<User> result1 = userRepository.findByIdInAndUsernameContainingIgnoreCase(ids, "ALICE", pageable);
        Page<User> result2 = userRepository.findByIdInAndUsernameContainingIgnoreCase(ids, "alice", pageable);
        Page<User> result3 = userRepository.findByIdInAndUsernameContainingIgnoreCase(ids, "Alice", pageable);

        // then - все должны вернуть одинаковый результат
        assertEquals(2, result1.getTotalElements());
        assertEquals(2, result2.getTotalElements());
        assertEquals(2, result3.getTotalElements());
    }

    @Test
    @DisplayName("findByIdNotIn - исключение несуществующих ID")
    void testFindByIdNotIn_WithNonExistentIds() {
        // arrange
        Set<Long> excludedIds = new HashSet<>(Arrays.asList(user1.getId(), 999L, 1000L));
        Pageable pageable = PageRequest.of(0, 10);

        // when
        Page<User> result = userRepository.findByIdNotIn(excludedIds, pageable);

        // then - должны получить всех, кроме user1
        assertEquals(4, result.getTotalElements()); // user2, user3, user4, user5
        assertThat(result.getContent())
                .extracting(User::getId)
                .containsExactlyInAnyOrder(user2.getId(), user3.getId(), user4.getId(), user5.getId())
                .doesNotContain(user1.getId());
    }
}