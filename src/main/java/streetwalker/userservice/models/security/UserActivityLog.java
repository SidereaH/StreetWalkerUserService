package streetwalker.userservice.models.security;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import streetwalker.userservice.models.User; // Предполагается, что у вас есть сущность User

import java.time.OffsetDateTime;
import java.util.UUID; // Пример для request_id

@Entity
@Table(name = "user_activity_log")
@EntityListeners(AuditingEntityListener.class) // Для автоматического заполнения CreatedDate
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder // Удобно для создания записей
public class UserActivityLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING) // Храним название ENUM как строку
    @Column(nullable = false)
    private ActionType actionType; // Тип действия (LOGIN, SIGNUP и т.д.)

    @Column(nullable = false)
    private String actionName; // Название действия (например, "User Login", "New User Registration")

    @ManyToOne(fetch = FetchType.LAZY) // Связь с пользователем, LAZY загрузка
    @JoinColumn(name = "user_id") // Имя колонки внешнего ключа
    private User user; // Пользователь, совершивший действие (может быть null для некоторых действий, например, неудачная попытка входа)

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private OffsetDateTime created; // Время совершения действия

    @Column(length = 500) // Увеличиваем размер, если описание может быть длинным
    private String description; // Подробное описание действия

    @Column(columnDefinition = "TEXT") // Может содержать JSON или просто текстовое сообщение
    private String details; // Доuser.getEmail();полнительные детали, например, JSON с измененными полями при обновлении профиля

    @Column(name = "ip_address")
    private String ipAddress; // IP-адрес, с которого было совершено действие (храним как String)

    @Column(name = "user_agent", length = 500)
    private String userAgent; // User-Agent пользователя

    @Column(name = "request_id")
    private UUID requestId; // Идентификатор запроса для трассировки

    private boolean success; // Было ли действие успешным?

    @Column(columnDefinition = "TEXT")
    private String errorMessage; // Сообщение об ошибке, если действие было неудачным

    // Если нужно отслеживать ресурс, на который было воздействие
    @Column(name = "resource_type")
    private String resourceType; // Например, "User", "Product"
    @Column(name = "resource_id")
    private String resourceId; // ID ресурса, если применимо (например, ID пользователя, которого удалили)


    @Column(name = "actor_type")
    private String actorType; // "USER", "SYSTEM", "ADMIN"
}