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

import java.util.*;
import java.util.stream.Collectors;

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
        if (!Objects.equals(oldEmail, user.getEmail())) {
            details.append("\"oldEmail\": \"").append(oldEmail).append("\", \"newEmail\": \"").append(updatedUser.getEmail()).append("\"");
            first = false;
        }
        if (!Objects.equals(oldPhone, updatedUser.getPhone())) {
            if (!first) details.append(", ");
            details.append("\"oldPhone\": \"").append(oldPhone).append("\", \"newPhone\": \"").append(updatedUser.getPhone()).append("\"");
            first = false;
        }
        if (!Objects.equals(oldUsername, updatedUser.getUsername())) {
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

    /**
     * Добавить пользователя в друзья
     */
    @Transactional
    public void addFriend(Long userId, Long friendId) {
        UUID requestId = UUID.randomUUID();

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));

        User friend = userRepository.findById(friendId)
                .orElseThrow(() -> new RuntimeException("Friend not found with id: " + friendId));

        if (user.equals(friend)) {
            throw new RuntimeException("Cannot add yourself as a friend");
        }

        if (user.isFriend(friend)) {
            throw new RuntimeException("Users are already friends");
        }

        user.addFriend(friend);
        userRepository.save(user);
        userRepository.save(friend);

        // Логирование
        activityLogService.logUserActivity(
                ActionType.ADD_FRIEND,
                "Add Friend",
                user,
                true,
                "User " + user.getUsername() + " added " + friend.getUsername() + " as friend",
                null,
                "{\"friendId\": " + friendId + ", \"friendUsername\": \"" + friend.getUsername() + "\"}",
                "User",
                friendId.toString(),
                friendId,
                "USER",
                requestId,
                requestContextHelper.getCurrentRequestIpAddress(),
                requestContextHelper.getCurrentRequestUserAgent()
        );
    }

    /**
     * Удалить пользователя из друзей
     */
    @Transactional
    public void removeFriend(Long userId, Long friendId) {
        UUID requestId = UUID.randomUUID();

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));

        User friend = userRepository.findById(friendId)
                .orElseThrow(() -> new RuntimeException("Friend not found with id: " + friendId));

        if (!user.isFriend(friend)) {
            throw new RuntimeException("Users are not friends");
        }

        user.removeFriend(friend);
        userRepository.save(user);
        userRepository.save(friend);

        // Логирование
        activityLogService.logUserActivity(
                ActionType.REMOVE_FRIEND,
                "Remove Friend",
                user,
                true,
                "User " + user.getUsername() + " removed " + friend.getUsername() + " from friends",
                null,
                "{\"friendId\": " + friendId + ", \"friendUsername\": \"" + friend.getUsername() + "\"}",
                "User",
                friendId.toString(),
                friendId,
                "USER",
                requestId,
                requestContextHelper.getCurrentRequestIpAddress(),
                requestContextHelper.getCurrentRequestUserAgent()
        );
    }

    /**
     * Проверить, являются ли пользователи друзьями
     */
    public boolean areFriends(Long userId, Long friendId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));

        User friend = userRepository.findById(friendId)
                .orElseThrow(() -> new RuntimeException("Friend not found with id: " + friendId));

        return user.isFriend(friend);
    }

    /**
     * Получить список друзей пользователя
     */
    public Page<UserDTO> getFriends(Long userId, Pageable pageable) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));

        // Получаем ID всех друзей
        Set<Long> friendIds = user.getFriends().stream()
                .map(User::getId)
                .collect(Collectors.toSet());

        if (friendIds.isEmpty()) {
            return Page.empty(pageable);
        }

        // Получаем страницу друзей
        Page<User> friendsPage = userRepository.findAllByIdIn(friendIds, pageable);

        // Логирование
        activityLogService.logUserActivity(
                ActionType.VIEW_FRIENDS,
                "View Friends List",
                user,
                true,
                "User " + user.getUsername() + " viewed friends list (" + friendsPage.getTotalElements() + " friends)",
                null,
                "{\"friendsCount\": " + friendsPage.getTotalElements() + "}",
                null,
                null,
                null,
                null,
                UUID.randomUUID(),
                requestContextHelper.getCurrentRequestIpAddress(),
                requestContextHelper.getCurrentRequestUserAgent()
        );

        return friendsPage.map(userMapper::map);
    }

    /**
     * Получить список общих друзей
     */
    public Page<UserDTO> getMutualFriends(Long userId, Long otherUserId, Pageable pageable) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));

        User otherUser = userRepository.findById(otherUserId)
                .orElseThrow(() -> new RuntimeException("Other user not found with id: " + otherUserId));

        Set<User> mutualFriends = user.getMutualFriends(otherUser);
        Set<Long> mutualFriendIds = mutualFriends.stream()
                .map(User::getId)
                .collect(Collectors.toSet());

        if (mutualFriendIds.isEmpty()) {
            return Page.empty(pageable);
        }

        Page<User> mutualFriendsPage = userRepository.findAllByIdIn(mutualFriendIds, pageable);

        // Логирование
        activityLogService.logUserActivity(
                ActionType.VIEW_MUTUAL_FRIENDS,
                "View Mutual Friends",
                user,
                true,
                "User " + user.getUsername() + " viewed mutual friends with " + otherUser.getUsername() +
                        " (" + mutualFriendsPage.getTotalElements() + " mutual friends)",
                null,
                "{\"otherUserId\": " + otherUserId + ", \"otherUsername\": \"" + otherUser.getUsername() +
                        "\", \"mutualFriendsCount\": " + mutualFriendsPage.getTotalElements() + "}",
                "User",
                otherUserId.toString(),
                otherUserId,
                "USER",
                UUID.randomUUID(),
                requestContextHelper.getCurrentRequestIpAddress(),
                requestContextHelper.getCurrentRequestUserAgent()
        );

        return mutualFriendsPage.map(userMapper::map);
    }

    /**
     * Поиск друзей по имени пользователя
     */
    public Page<UserDTO> searchFriends(Long userId, String searchQuery, Pageable pageable) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));

        // Получаем ID всех друзей
        Set<Long> friendIds = user.getFriends().stream()
                .map(User::getId)
                .collect(Collectors.toSet());

        if (friendIds.isEmpty()) {
            return Page.empty(pageable);
        }

        // Ищем друзей по подстроке среди ID друзей
        Page<User> friendsPage = userRepository.findByIdInAndUsernameContainingIgnoreCase(
                friendIds, searchQuery, pageable);

        // Логирование
        activityLogService.logUserActivity(
                ActionType.SEARCH_FRIENDS,
                "Search Friends",
                user,
                true,
                "User " + user.getUsername() + " searched friends with query: " + searchQuery +
                        " (" + friendsPage.getTotalElements() + " results)",
                null,
                "{\"searchQuery\": \"" + searchQuery + "\", \"resultsCount\": " + friendsPage.getTotalElements() + "}",
                null,
                null,
                null,
                null,
                UUID.randomUUID(),
                requestContextHelper.getCurrentRequestIpAddress(),
                requestContextHelper.getCurrentRequestUserAgent()
        );

        return friendsPage.map(userMapper::map);
    }

    /**
     * Получить количество друзей
     */
    public int getFriendsCount(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));

        return user.getFriendsCount();
    }

    /**
     * Рекомендации друзей (пользователи, которые не являются друзьями)
     */
    public Page<UserDTO> getFriendSuggestions(Long userId, Pageable pageable) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));

        // Получаем ID всех друзей и самого пользователя
        Set<Long> excludedIds = user.getFriends().stream()
                .map(User::getId)
                .collect(Collectors.toSet());
        excludedIds.add(userId);

        // Получаем рекомендации (исключая друзей и самого пользователя)
        Page<User> suggestionsPage = userRepository.findByIdNotIn(excludedIds, pageable);

        // Логирование
        activityLogService.logUserActivity(
                ActionType.VIEW_FRIEND_SUGGESTIONS,
                "View Friend Suggestions",
                user,
                true,
                "User " + user.getUsername() + " viewed friend suggestions (" + suggestionsPage.getTotalElements() + " suggestions)",
                null,
                "{\"suggestionsCount\": " + suggestionsPage.getTotalElements() + "}",
                null,
                null,
                null,
                null,
                UUID.randomUUID(),
                requestContextHelper.getCurrentRequestIpAddress(),
                requestContextHelper.getCurrentRequestUserAgent()
        );

        return suggestionsPage.map(userMapper::map);
    }

}