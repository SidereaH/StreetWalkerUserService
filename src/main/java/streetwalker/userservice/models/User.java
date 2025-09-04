package streetwalker.userservice.models;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

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
@PrimaryKeyJoinColumn(name = "profile_id")
public class User extends Profile implements UserDetails {

//    /**Глобальный юзернейм - аналог юз в тг*/
//    @Column(unique = true)
//    private String username;
    private  String password;
    @Column(unique = true)
    /**Почта для контакта*/
    private String email;
    @Column(unique = true)
    private String phone;
    private String firstName;
    private String lastName;
    /**Хранение описания профиля, заданного пользователем*/
//    private String bio; // заменено на description в super class
    /**Ссылка на s3 хранилище с аватарочкой*/
//    private String avatarUrl;
//    @CreatedDate
//    private OffsetDateTime createdAt;
//    @LastModifiedDate
//    private OffsetDateTime updatedAt;
    @ManyToOne
    private Role role;
    /**Статус пользователя, может быть заблокирован, активен и тд*/
    @ManyToOne
    private Status status;


    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role));
    }

    @Override
    public boolean isAccountNonExpired() {
        return UserDetails.super.isAccountNonExpired();
    }

    @Override
    public boolean isAccountNonLocked() {
        return !this.getStatus().getStatusName().equals("LOCKED");
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return UserDetails.super.isCredentialsNonExpired();
    }

    @Override
    public boolean isEnabled() {
        return UserDetails.super.isEnabled();
    }

    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", username='" + super.getUsername() + '\'' +
                ", password='" + password + '\'' +
                ", email='" + email + '\'' +
                ", phone='" + phone + '\'' +
                ", firstName='" + firstName + '\'' +
                ", lastName='" + lastName + '\'' +
                ", description='" + this.getDescription() + '\'' +
                ", avatarUrl='" + this.getAvatarUrl() + '\'' +
                ", createdAt=" + this.getCreatedAt() +
                ", updatedAt=" + this.getUpdatedAt() +
                ", role=" + role +
                ", status=" + status +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        User user = (User) o;
        return Objects.equals(id, user.id) && Objects.equals(this.getUsername(), user.getUsername()) && Objects.equals(password, user.password) && Objects.equals(email, user.email) && Objects.equals(phone, user.phone) && Objects.equals(firstName, user.firstName) && Objects.equals(lastName, user.lastName) && Objects.equals(this.getDescription(), user.getDescription()) && Objects.equals(this.getAvatarUrl(), user.getAvatarUrl()) && Objects.equals(this.getCreatedAt(), user.getCreatedAt()) && Objects.equals(this.getUpdatedAt(), user.getUpdatedAt()) && Objects.equals(role, user.role) && Objects.equals(status, user.status);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, this.getUsername(), password, email, phone, firstName, lastName, this.getDescription(), this.getUsername(), this.getCreatedAt(), this.getUpdatedAt(), role, status);
    }

    @Override
    public ProfileType getType() {
        return ProfileType.USER;
    }
}
