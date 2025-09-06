package streetwalker.userservice.repositories;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import streetwalker.userservice.models.User;
import streetwalker.userservice.models.security.ActionType;
import streetwalker.userservice.models.security.UserActivityLog;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserActivityLogRepository extends JpaRepository<UserActivityLog, Long> {

    // Найти все логи по пользователю
    List<UserActivityLog> findByUser(User user);

    // Найти все логи по типу действия
    List<UserActivityLog> findByActionType(ActionType actionType);

    // Найти все логи по пользователю и типу действия
    List<UserActivityLog> findByUserAndActionType(User user, ActionType actionType);

    // Найти логи за определенный период
    List<UserActivityLog> findByCreatedBetween(OffsetDateTime start, OffsetDateTime end);

    // Найти логи по IP-адресу
    List<UserActivityLog> findByIpAddress(String ipAddress);

    // Найти логи по request_id
    Optional<UserActivityLog> findByRequestId(UUID requestId);

    // Найти неудачные попытки входа для конкретного пользователя
    List<UserActivityLog> findByUserAndActionTypeAndSuccessIsFalse(User user, ActionType actionType);

    // Подсчет количества действий по типу
    long countByActionType(ActionType actionType);

    // Подсчет количества успешных/неуспешных действий по типу
    long countByActionTypeAndSuccess(ActionType actionType, boolean success);

    // Дополнительные методы по необходимости...
}
