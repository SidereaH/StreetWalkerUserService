package streetwalker.userservice.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import streetwalker.userservice.models.Status;

import java.util.Optional;

@Repository
public interface StatusRepository extends JpaRepository<Status, Long> {
    Optional<Status> findByStatusName(String statusName);
}
