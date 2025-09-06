package streetwalker.userservice.services;

import lombok.extern.slf4j.Slf4j;
import org.apache.coyote.BadRequestException;
import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import streetwalker.userservice.dto.*;
import streetwalker.userservice.models.RefreshToken;
import streetwalker.userservice.mappers.UserMapper;
import streetwalker.userservice.models.User;
import streetwalker.userservice.models.security.ActionType;
import streetwalker.userservice.repositories.UserRepository;
import streetwalker.userservice.security.JwtCore;
import streetwalker.userservice.services.security.UserActivityLogService;
import streetwalker.userservice.services.util.RequestContextHelper;

import java.util.Optional;
import java.util.Random;
import java.util.UUID;

@Slf4j
@Service
public class UserService implements UserDetailsService {
    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final RefreshTokenService refreshTokenService;
    private final JwtCore jwtCore;
    private final UserActivityLogService activityLogService;
    private final RequestContextHelper requestContextHelper;

    public UserService(UserRepository userRepository, UserMapper userMapper, PasswordEncoder passwordEncoder, RoleService roleService, StatusService statusService, AuthenticationManager authenticationManager, RefreshTokenService refreshTokenService, JwtCore jwtCore, UserActivityLogService activityLogService, RequestContextHelper requestContextHelper) {
        this.userRepository = userRepository;
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.refreshTokenService = refreshTokenService;
        this.jwtCore = jwtCore;
        this.activityLogService = activityLogService;
        this.requestContextHelper = requestContextHelper;
    }

    public Page<User> findAll(Pageable usersPageable) {
        return userRepository.findAll(usersPageable);
    }

    public Optional<User> findUserById(Long id) {
        return userRepository.findById(id);
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with username: " + username));
    }

    @Transactional
    public UserDTO create(SignupRequest userData) throws RuntimeException {
        UUID requestId = UUID.randomUUID(); // Генерируем requestId для этой операции

        if (userRepository.existsByUsername(userData.getUsername())) {
            String errorMessage = "User already exists with username: " + userData.getUsername();
            log.error(errorMessage);
            activityLogService.logSignupSuccess(
                    null, // Пользователя еще нет или не создали
                    requestId,
                    false, // Неуспешная регистрация
                    errorMessage,
                    userData.getUsername(), // Можно передать имя, по которому пытались зарегистрироваться
                    userData.getEmail(),
                    userData.getPhone()
            );
            throw new RuntimeException(errorMessage);
        } else if (userRepository.existsByEmail(userData.getEmail())) {
            String errorMessage = "User already exists with email: " + userData.getEmail();
            log.error(errorMessage);
            activityLogService.logSignupSuccess(
                    null,
                    requestId,
                    false,
                    errorMessage,
                    userData.getUsername(),
                    userData.getEmail(),
                    userData.getPhone()
            );
            throw new RuntimeException(errorMessage);
        } else if (userRepository.existsByPhone(userData.getPhone())) {
            String errorMessage = "User already exists with phone: " + userData.getPhone();
            log.error(errorMessage);
            activityLogService.logSignupSuccess(
                    null,
                    requestId,
                    false,
                    errorMessage,
                    userData.getUsername(),
                    userData.getEmail(),
                    userData.getPhone()
            );
            throw new RuntimeException(errorMessage);
        }

        userData.setPassword(passwordEncoder.encode(userData.getPassword()));
        User user = userMapper.map(userData);
        userRepository.save(user);

        // Логирование успешной регистрации
        activityLogService.logSignupSuccess(user, requestId);

        return userMapper.map(user);
    }

    public UserDTO update(UserUpdateDTO userData, Long id) throws BadRequestException {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new BadRequestException("Not Found"));

        // Сохраняем старые данные для логирования изменений
        String oldEmail = user.getEmail();
        String oldPhone = user.getPhone();
        String oldUsername = user.getUsername();

        userMapper.update(userData, user);
        User updatedUser = userRepository.save(user);

        // Формируем JSON с измененными полями
        StringBuilder details = new StringBuilder("{");
        boolean first = true;
        if (!oldEmail.equals(updatedUser.getEmail())) {
            details.append("\"oldEmail\": \"").append(oldEmail).append("\", \"newEmail\": \"").append(updatedUser.getEmail()).append("\"");
            first = false;
        }
        if (!oldPhone.equals(updatedUser.getPhone())) {
            if (!first) details.append(", ");
            details.append("\"oldPhone\": \"").append(oldPhone).append("\", \"newPhone\": \"").append(updatedUser.getPhone()).append("\"");
            first = false;
        }
        if (!oldUsername.equals(updatedUser.getUsername())) {
            if (!first) details.append(", ");
            details.append("\"oldUsername\": \"").append(oldUsername).append("\", \"newUsername\": \"").append(updatedUser.getUsername()).append("\"");
            first = false;
        }
        details.append("}");


        // Логирование успешного обновления профиля
        activityLogService.logProfileUpdate(updatedUser, details.toString(), UUID.randomUUID());

        return userMapper.map(updatedUser);
    }


    public AuthResponse signin(SigninRequest signinRequest) throws RuntimeException, BadCredentialsException {
        UUID requestId = UUID.randomUUID(); // Генерируем requestId для этой операции

        Optional<User> userOptional = userRepository.findByPhone(signinRequest.getPhone());
        User user = null;
        if (userOptional.isPresent()) {
            user = userOptional.get();
        } else {
            String errorMessage = "User not found with phone: " + signinRequest.getPhone();
            log.error(errorMessage);
            activityLogService.logLoginFailure(
                    "Phone: " + signinRequest.getPhone(),
                    requestContextHelper.getCurrentRequestIpAddress(),
                    requestContextHelper.getCurrentRequestUserAgent(),
                    errorMessage,
                    requestId
            );
            throw new RuntimeException(errorMessage);
        }

        Authentication authentication = null;
        try {
            authentication = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(user.getUsername(), signinRequest.getPassword()));
            SecurityContextHolder.getContext().setAuthentication(authentication);
            String jwt = jwtCore.generateToken(authentication);
            RefreshToken refresh = refreshTokenService.create(user);

            // Логирование успешного входа
            activityLogService.logLoginSuccess(user, requestId);

            return new AuthResponse(jwt, refresh.getToken());

        } catch (BadCredentialsException e) {
            String errorMessage = "Invalid credentials for user: " + user.getUsername();
            log.error(errorMessage);
            // Логирование неудачного входа
            activityLogService.logLoginFailure(
                    user.getUsername(),
                    requestContextHelper.getCurrentRequestIpAddress(),
                    requestContextHelper.getCurrentRequestUserAgent(),
                    errorMessage,
                    requestId
            );
            throw new BadCredentialsException(errorMessage);
        }
    }

    public AuthResponse refresh(String refreshToken) throws RuntimeException {
        UUID requestId = UUID.randomUUID(); // Генерируем requestId для этой операции

        try {
            String username = refreshTokenService.check(refreshToken);
            log.info("Extracted username from token: {}", username);

            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("User not found exception during refresh token validation"));

            String newAccessToken = jwtCore.generateToken(user);
            String newRefreshToken = refreshTokenService.create(user).getToken();

            // Логирование успешного обновления токена
            activityLogService.logUserActivity(
                    ActionType.REFRESH_TOKEN, // <-- Добавьте этот тип в ваш ActionType Enum
                    "Token Refresh",
                    user,
                    true,
                    "User " + user.getUsername() + " successfully refreshed authentication token.",
                    null,
                    null,
                    "User",
                    user.getId().toString(),
                    user.getId(),
                    "USER",
                    requestId,
                    requestContextHelper.getCurrentRequestIpAddress(),
                    requestContextHelper.getCurrentRequestUserAgent()
            );

            return new AuthResponse(newAccessToken, newRefreshToken);
        } catch (RuntimeException e) {
            String errorMessage = "Failed to refresh token: " + e.getMessage();
            log.error(errorMessage);
            activityLogService.logUserActivity(
                    streetwalker.userservice.models.security.ActionType.REFRESH_TOKEN,
                    "Token Refresh Failure",
                    null, // Пользователь может быть неизвестен, если токен невалиден
                    false,
                    errorMessage,
                    errorMessage,
                    null,
                    null,
                    null,
                    null,
                    null,
                    requestId,
                    requestContextHelper.getCurrentRequestIpAddress(),
                    requestContextHelper.getCurrentRequestUserAgent()
            );
            throw e; // Пробрасываем исключение дальше
        }
    }

    public void logout(String refreshToken) throws DataAccessException {
        UUID requestId = UUID.randomUUID();
        String username = null;
        User user = null;
        try {
            username = refreshTokenService.getUsernameFromToken(refreshToken);
            user = userRepository.findByUsername(username).orElse(null);

            refreshTokenService.delete(refreshToken);

            // Логирование успешного выхода
            activityLogService.logUserActivity(
                    streetwalker.userservice.models.security.ActionType.LOGOUT, // <-- Добавьте этот тип в ваш ActionType Enum
                    "User Logout",
                    user,
                    true,
                    "User " + (user != null ? user.getUsername() : "N/A") + " successfully logged out.",
                    null,
                    null,
                    "User",
                    user != null ? user.getId().toString() : null,
                    user != null ? user.getId() : null,
                    "USER",
                    requestId,
                    requestContextHelper.getCurrentRequestIpAddress(),
                    requestContextHelper.getCurrentRequestUserAgent()
            );

        } catch (DataAccessException e) {
            String errorMessage = "Failed to logout user: " + e.getMessage();
            log.error(errorMessage);
            activityLogService.logUserActivity(
                    streetwalker.userservice.models.security.ActionType.LOGOUT,
                    "User Logout Failure",
                    user, // Может быть null
                    false,
                    errorMessage,
                    errorMessage,
                    null,
                    null,
                    null,
                    null,
                    null,
                    requestId,
                    requestContextHelper.getCurrentRequestIpAddress(),
                    requestContextHelper.getCurrentRequestUserAgent()
            );
            throw e;
        } catch (RuntimeException e) { // Например, если токен невалиден при извлечении username
            String errorMessage = "Logout attempt with invalid token: " + e.getMessage();
            log.warn(errorMessage);
            activityLogService.logUserActivity(
                    streetwalker.userservice.models.security.ActionType.LOGOUT,
                    "User Logout Attempt (Invalid Token)",
                    null,
                    false,
                    errorMessage,
                    errorMessage,
                    null,
                    null,
                    null,
                    null,
                    null,
                    requestId,
                    requestContextHelper.getCurrentRequestIpAddress(),
                    requestContextHelper.getCurrentRequestUserAgent()
            );
        }
    }

    /* поиск по подстроке юзернейма*/
    public Page<User> findAllByUsernameSub(Pageable pageable, String username) throws RuntimeException {
        Page<User> users = userRepository.findUserByUsernameContainingIgnoreCase(pageable, username);
        if (users.isEmpty()) {
            // Логирование, если пользователи не найдены по подстроке
            activityLogService.logUserActivity(
                    streetwalker.userservice.models.security.ActionType.SEARCH, // <-- Добавьте этот тип в ваш ActionType Enum
                    "User Search by Username Substring",
                    null, // Не связан с конкретным пользователем, инициировавшим поиск
                    false,
                    "No users found for substring: " + username,
                    "No users found",
                    "{\"searchQuery\": \"" + username + "\"}",
                    null,
                    null,
                    null,
                    null,
                    UUID.randomUUID(),
                    requestContextHelper.getCurrentRequestIpAddress(),
                    requestContextHelper.getCurrentRequestUserAgent()
            );
            throw new RuntimeException("User not found exception");
        }
        activityLogService.logUserActivity(
                streetwalker.userservice.models.security.ActionType.SEARCH,
                "User Search by Username Substring",
                null,
                true,
                "Found " + users.getTotalElements() + " users for substring: " + username,
                null,
                "{\"searchQuery\": \"" + username + "\", \"resultsCount\": " + users.getTotalElements() + "}",
                null,
                null,
                null,
                null,
                UUID.randomUUID(),
                requestContextHelper.getCurrentRequestIpAddress(),
                requestContextHelper.getCurrentRequestUserAgent()
        );
        return users;
    }
    //генерируется уникальный код, возвращается ссылка, отправляющаяся на почту юзеру. юзер переходит по ссылке и отправляет пароль
    public void createNewPassLinkAndSendToMail(Long id){
        User user = findUserById(id).orElseThrow(() -> new RuntimeException("User not found exception"));
        //через кафку шлем на почту письмо с кодом
        user.getEmail();
        Random random = new Random();
        Integer code = random.nextInt((9999 - 100) + 1) + 10;
        activityLogService.logPasswordUpdate(user, UUID.randomUUID(), code, false, "In process");
    }

    public void updatePassword(UUID requestId, Integer code, String newPassword) {
        var log = activityLogService.getLogByRequestId(requestId).orElseThrow(() -> new RuntimeException("User not found exception"));
        if (Integer.parseInt(log.getDetails()) != code) {
            activityLogService.updatePasswordUpdateLog(requestId, code);
            var user = log.getUser();
            user.setPassword(passwordEncoder.encode(newPassword));
            userRepository.save(user);
        }
    }
}