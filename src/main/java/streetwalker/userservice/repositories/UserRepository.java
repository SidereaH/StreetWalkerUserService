package streetwalker.userservice.repositories;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import streetwalker.userservice.models.User;

import java.util.Collection;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    Page<User> findByFirstNameAndLastName(Pageable pageable, String firstName, String lastName);
    Boolean existsByEmail(String email);
    Boolean existsByUsername(String username);
    Boolean existsByPhone(String phone);
    Optional<User> findByPhone(String phone);
    Page<User> findUserByUsernameContainingIgnoreCase(Pageable pageable, String substring);

    Page<User> findAllByIdIn(Collection<Long> ids, Pageable pageable);
    Page<User> findByIdInAndUsernameContainingIgnoreCase(Collection<Long> ids, String username, Pageable pageable);
    Page<User> findByIdNotIn(Collection<Long> ids, Pageable pageable);
    Optional<User> findById(Long id);
}
