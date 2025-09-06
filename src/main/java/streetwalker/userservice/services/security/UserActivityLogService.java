package streetwalker.userservice.services.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import streetwalker.userservice.models.User;
import streetwalker.userservice.models.security.ActionType;
import streetwalker.userservice.models.security.UserActivityLog;
import streetwalker.userservice.repositories.UserActivityLogRepository;
import streetwalker.userservice.services.util.RequestContextHelper;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j // Для логирования внутри сервиса
public class UserActivityLogService {

    private final UserActivityLogRepository activityLogRepository;
    private final RequestContextHelper requestContextHelper; // Для получения IP и User-Agent

    /**
     * Общий метод для записи лога активности.
     *
     * @param actionType      Тип действия (Enum).
     * @param actionName      Читабельное название действия.
     * @param user            Пользователь, совершивший действие (может быть null).
     * @param success         Флаг успешности действия.
     * @param description     Краткое описание события.
     * @param errorMessage    Сообщение об ошибке, если действие было неудачным (может быть null).
     * @param eventDetails    Дополнительные детали в JSON или текстовом формате (может быть null).
     * @param resourceType    Тип ресурса, на который воздействовали (может быть null).
     * @param resourceId      ID ресурса (может быть null).
     * @param actorId         ID сущности, инициировавшей действие (может быть null, по умолчанию user.id).
     * @param actorType       Тип инициатора действия ("USER", "SYSTEM", "ADMIN") (может быть null).
     * @param requestId       UUID запроса (может быть null, генерируется, если нет).
     * @param ipAddress       IP-адрес клиента (если null, попытается получить из запроса).
     * @param userAgent       User-Agent клиента (если null, попытается получить из запроса).
     */
    @Async // Выполнение в отдельном потоке
    @Transactional(propagation = Propagation.REQUIRES_NEW) // Новая транзакция для логирования
    public void logUserActivity(
            ActionType actionType,
            String actionName,
            User user,
            boolean success,
            String description,
            String errorMessage,
            String eventDetails,
            String resourceType,
            String resourceId,
            Long actorId,
            String actorType,
            UUID requestId,
            String ipAddress,
            String userAgent
    ) {
        try {
            // Если requestId не предоставлен, генерируем новый
            UUID finalRequestId = (requestId != null) ? requestId : UUID.randomUUID();

            // Если IP или User-Agent не предоставлены, пытаемся получить из контекста запроса
            String finalIpAddress = (ipAddress != null) ? ipAddress : requestContextHelper.getCurrentRequestIpAddress();
            String finalUserAgent = (userAgent != null) ? userAgent : requestContextHelper.getCurrentRequestUserAgent();

            // Если actorId не предоставлен, но есть User, используем User.id
            String finalActorType = (actorType != null) ? actorType : (user != null ? "USER" : null);


            UserActivityLog logEntry = UserActivityLog.builder()
                    .actionType(actionType)
                    .actionName(actionName)
                    .user(user)
                    .success(success)
                    .description(description)
                    .errorMessage(errorMessage)
                    .details(eventDetails)
                    .resourceType(resourceType)
                    .resourceId(resourceId)
                    .actorType(finalActorType)
                    .requestId(finalRequestId)
                    .ipAddress(finalIpAddress)
                    .userAgent(finalUserAgent)
                    // created поле будет заполнено автоматически благодаря @CreatedDate и AuditingEntityListener
                    .build();

            activityLogRepository.save(logEntry);
            log.debug("Successfully logged user activity: {}", logEntry.getActionName());
        } catch (Exception e) {
            log.error("Failed to log user activity: {} for user {}. Error: {}",
                    actionName, (user != null ? user.getId() : "N/A"), e.getMessage(), e);
            // Важно: Не бросать исключение дальше, чтобы не влиять на основную бизнес-логику
        }
    }

    // --- Удобные обертки для конкретных типов действий ---

    @Async
    public void logLoginSuccess(User user, UUID requestId) {
        logUserActivity(ActionType.LOGIN, "User Login", user, true,
                "User " + user.getUsername() + " successfully logged in.",
                null, null, "User", user.getId().toString(),
                user.getId(), "USER", requestId, null, null);
    }

    @Async
    public void logLoginFailure(String usernameAttempt, String ipAddress, String userAgent, String errorMessage, UUID requestId) {
        logUserActivity(ActionType.LOGIN, "Failed Login Attempt", null, false,
                "Failed login attempt for username: " + usernameAttempt,
                errorMessage, null, null, null, null, null, requestId, ipAddress, userAgent);
    }

    @Async
    public void logSignupSuccess(User newUser, UUID requestId) {
        logUserActivity(ActionType.SIGNUP, "New User Registration", newUser, true,
                "New user " + newUser.getUsername() + " registered successfully.",
                null, null, "User", newUser.getId().toString(),
                newUser.getId(), "USER", requestId, null, null);
    }


    // Перегруженный метод для записи неудачной попытки регистрации
    @Async
    public void logSignupSuccess(User user, UUID requestId, boolean success, String errorMessage,
                                 String usernameAttempt, String emailAttempt, String phoneAttempt) {
        String description = success ?
                "New user " + user.getUsername() + " registered successfully." :
                "Failed registration attempt.";
        String details = "{\"usernameAttempt\": \"" + usernameAttempt + "\", \"emailAttempt\": \"" + emailAttempt + "\", \"phoneAttempt\": \"" + phoneAttempt + "\"}";

        logUserActivity(ActionType.SIGNUP, "User Registration", user, success,
                description, errorMessage, details, "User", user != null ? user.getId().toString() : null,
                null, null, requestId, null, null);
    }


    public void logPasswordUpdate(User user, UUID requestId, Integer code, boolean success, String errorMessage) {
        logUserActivity(ActionType.UPDATE_PASSWORD, "Password Update", user, success,
                "User " + user.getUsername() + " updated password.",
                errorMessage, code.toString(), "User", user.getId().toString(),
                user.getId(), "USER", requestId, null, null);
    }

    public void updatePasswordUpdateLog(UUID requestId, Integer code) {
        if (requestId != null) {
            Optional<UserActivityLog> logEntry = activityLogRepository.findByRequestId(requestId);
            if (logEntry.isPresent()) {
                UserActivityLog log = logEntry.get();
                log.setDetails(code.toString());
                log.setSuccess(true);
                log.setErrorMessage("Password Updated");
                activityLogRepository.save(log);
            }
        }
    }

    @Async
    public void logProfileUpdate(User user, String detailsJson, UUID requestId) {
        logUserActivity(ActionType.PROFILE_UPDATE, "User Profile Update", user, true,
                "User " + user.getUsername() + " updated profile.",
                null, detailsJson, "User", user.getId().toString(),
                user.getId(), "USER", requestId, null, null);
    }

    @Async
    public void logDeleteAccount(User deletedUser, User actor, UUID requestId) {
        logUserActivity(ActionType.DELETE_ACCOUNT, "Account Deletion", deletedUser, true,
                "Account " + deletedUser.getUsername() + " was deleted.",
                null, null, "User", deletedUser.getId().toString(),
                actor != null ? actor.getId() : null, actor != null ? "ADMIN" : "USER", // Если удалил сам пользователь, то USER, если админ, то ADMIN
                requestId, null, null);
    }


    // --- Методы для чтения логов (примеры) ---

    @Transactional(readOnly = true)
    public List<UserActivityLog> getLogsForUser(User user) {
        return activityLogRepository.findByUser(user);
    }

    @Transactional(readOnly = true)
    public List<UserActivityLog> getLogsByType(ActionType actionType) {
        return activityLogRepository.findByActionType(actionType);
    }

    @Transactional(readOnly = true)
    public List<UserActivityLog> getLogsForUserAndType(User user, ActionType actionType) {
        return activityLogRepository.findByUserAndActionType(user, actionType);
    }

    @Transactional(readOnly = true)
    public List<UserActivityLog> getFailedLoginAttempts(User user) {
        return activityLogRepository.findByUserAndActionTypeAndSuccessIsFalse(user, ActionType.LOGIN);
    }

    @Transactional(readOnly = true)
    public Optional<UserActivityLog> getLogByRequestId(UUID requestId) {
        return activityLogRepository.findByRequestId(requestId);
    }

    // Методы для пагинации и сортировки
    @Transactional(readOnly = true)
    public List<UserActivityLog> getAllLogs(int page, int size) {
        // Здесь можно использовать Pageable
        // return activityLogRepository.findAll(PageRequest.of(page, size, Sort.by("created").descending()));
        return activityLogRepository.findAll(); // Для простоты, пока без пагинации
    }
}