package streetwalker.userservice.models;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;

import java.time.OffsetDateTime;
/** Класс для хранения Профиля пользователя платформы
   * @author Siderea
   * @version 0.1
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "street_users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    /**Глобальный юзернейм - аналог юз в тг*/
    @Column(unique = true)
    private String username;
    @Column(unique = true)
    /**Почта для контакта*/
    private String email;
    private String firstName;
    private String lastName;
    /**Хранение описания профиля, заданного пользователем*/
    private String bio;
    /**Ссылка на s3 хранилище с аватарочкой*/
    private String avatarUrl;
    @CreatedDate
    private OffsetDateTime createdAt;
    @LastModifiedDate
    private OffsetDateTime updatedAt;
    /**Роль пользователя под вопросом, скорее нужно обращаться к серверу авторизации, там искать роль*/
    @ManyToOne
    private Role role;
    /**Статус пользователя, может быть заблокирован, активен и тд*/
    @ManyToOne
    private Status status;
}
