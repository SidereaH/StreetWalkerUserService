package streetwalker.userservice.controllers;

import lombok.extern.slf4j.Slf4j;
import org.apache.coyote.BadRequestException;
import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import streetwalker.userservice.dto.*;
import streetwalker.userservice.models.User;
import streetwalker.userservice.services.UserService;
import streetwalker.userservice.services.security.SecurityUtils;

import java.util.UUID;
@Slf4j
@RestController
@RequestMapping("/users")
public class UserController {

    private final UserService userService;
    private final SecurityUtils securityUtils;
    public UserController(UserService userService, SecurityUtils securityUtils) {
        this.userService = userService;
        this.securityUtils = securityUtils;
    }
    //переделать в возвращение dto
    @GetMapping
    public ResponseEntity<Page<UserDTO>> getAllUsers(Pageable pageable) {
        log.info("getAllUsers" + pageable);
        if(pageable != null) {
            return new ResponseEntity<>(userService.findAll(pageable), HttpStatus.OK);
        }
        return new ResponseEntity<>(null, HttpStatus.BAD_REQUEST);
    }

    @GetMapping("/{id}")
    public ResponseEntity<User> getUser(@PathVariable("id") Long id) {
        if (id != null ){
            try{
                return new ResponseEntity<>(userService.findUserById(id).orElseThrow(() -> new BadRequestException("No such user")), HttpStatus.OK);
            }
            catch (BadRequestException e) {
                return new ResponseEntity<>(null, HttpStatus.BAD_REQUEST);
            }
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
    }

    @PostMapping
    public ResponseEntity<UserDTO>  create(@RequestBody UserCreateDTO userData) {
        UserDTO user;
        try{
            user = userService.create(userData);
        } catch (DataAccessException e) {
            user = new UserDTO();
            user.setUsername(e.getLocalizedMessage());
            return new ResponseEntity<>(user, HttpStatus.BAD_REQUEST);
        } catch (RuntimeException e) {
            user = new UserDTO();
            user.setUsername(e.getMessage());
            return new ResponseEntity<>(user, HttpStatus.CONFLICT);
        }
        return new ResponseEntity<>(user, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<UserDTO> update(@RequestBody @Validated UserUpdateDTO userData, @PathVariable Long id) {

        if (!securityUtils.isCurrentUser(id)&& !securityUtils.isCurrentUserAdmin()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(null);
        }
        UserDTO user;
        try {
            user = userService.update(userData, id);
        } catch (BadRequestException e) {
            var userDTO = new UserDTO();
            userDTO.setUsername(e.getLocalizedMessage());
            return new ResponseEntity<>(userDTO, HttpStatus.BAD_REQUEST);
        } catch (RuntimeException e) {
            var userDTO = new UserDTO();
            userDTO.setUsername(e.getMessage());
            return new ResponseEntity<>(userDTO, HttpStatus.NOT_FOUND);
        }
        return new ResponseEntity<>(user, HttpStatus.OK);
    }

    @GetMapping("/search/username")
    public ResponseEntity<?> searchUsersByUsername(Pageable pageable, @RequestParam String username) {
        Page<User> users;
        try{
            users = userService.findAllByUsernameSub(pageable, username);
        }
        catch (RuntimeException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.NOT_FOUND);
        }
        return new ResponseEntity<>(users, HttpStatus.OK);
    }

    // Новые методы для работы с друзьями

    @PostMapping("/{userId}/friends/{friendId}")
    public ResponseEntity<?> addFriend(@PathVariable Long userId, @PathVariable Long friendId) {
        if (!securityUtils.isCurrentUser(userId)&& !securityUtils.isCurrentUserAdmin()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(null);
        }
        try {
            userService.addFriend(userId, friendId);
            return new ResponseEntity<>("Friend added successfully", HttpStatus.OK);
        } catch (RuntimeException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    @DeleteMapping("/{userId}/friends/{friendId}")
    public ResponseEntity<?> removeFriend(@PathVariable Long userId, @PathVariable Long friendId) {
        if (!securityUtils.isCurrentUser(userId) && !securityUtils.isCurrentUserAdmin()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(null);
        }
        try {
            userService.removeFriend(userId, friendId);
            return new ResponseEntity<>("Friend removed successfully", HttpStatus.OK);
        } catch (RuntimeException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    @GetMapping("/{userId}/friends/check/{friendId}")
    public ResponseEntity<Boolean> areFriends(@PathVariable Long userId, @PathVariable Long friendId) {
        try {
            boolean areFriends = userService.areFriends(userId, friendId);
            return new ResponseEntity<>(areFriends, HttpStatus.OK);
        } catch (RuntimeException e) {
            return new ResponseEntity<>(false, HttpStatus.BAD_REQUEST);
        }
    }

    @GetMapping("/{userId}/friends")
    public ResponseEntity<Page<UserDTO>> getFriends(@PathVariable Long userId, Pageable pageable) {
        try {
            Page<UserDTO> friends = userService.getFriends(userId, pageable);
            return new ResponseEntity<>(friends, HttpStatus.OK);
        } catch (RuntimeException e) {
            return new ResponseEntity<>(Page.empty(), HttpStatus.NOT_FOUND);
        }
    }

    @GetMapping("/{userId}/friends/mutual/{otherUserId}")
    public ResponseEntity<Page<UserDTO>> getMutualFriends(@PathVariable Long userId,
                                                          @PathVariable Long otherUserId,
                                                          Pageable pageable) {
        try {
            Page<UserDTO> mutualFriends = userService.getMutualFriends(userId, otherUserId, pageable);
            return new ResponseEntity<>(mutualFriends, HttpStatus.OK);
        } catch (RuntimeException e) {
            return new ResponseEntity<>(Page.empty(), HttpStatus.NOT_FOUND);
        }
    }

    @GetMapping("/{userId}/friends/search")
    public ResponseEntity<Page<UserDTO>> searchFriends(@PathVariable Long userId,
                                                       @RequestParam String query,
                                                       Pageable pageable) {
        try {
            Page<UserDTO> friends = userService.searchFriends(userId, query, pageable);
            return new ResponseEntity<>(friends, HttpStatus.OK);
        } catch (RuntimeException e) {
            return new ResponseEntity<>(Page.empty(), HttpStatus.NOT_FOUND);
        }
    }

    @GetMapping("/{userId}/friends/count")
    public ResponseEntity<Integer> getFriendsCount(@PathVariable Long userId) {
        try {
            int count = userService.getFriendsCount(userId);
            return new ResponseEntity<>(count, HttpStatus.OK);
        } catch (RuntimeException e) {
            return new ResponseEntity<>(0, HttpStatus.NOT_FOUND);
        }
    }

    @GetMapping("/{userId}/friends/suggestions")
    public ResponseEntity<Page<UserDTO>> getFriendSuggestions(@PathVariable Long userId, Pageable pageable) {
        try {
            Page<UserDTO> suggestions = userService.getFriendSuggestions(userId, pageable);
            return new ResponseEntity<>(suggestions, HttpStatus.OK);
        } catch (RuntimeException e) {
            return new ResponseEntity<>(Page.empty(), HttpStatus.NOT_FOUND);
        }
    }

    // Методы для аутентификации и управления паролями

    @PostMapping("/signin")
    public ResponseEntity<?> signin(@RequestBody SigninRequest signinRequest) {
        try {
            AuthResponse authResponse = userService.signin(signinRequest);
            return new ResponseEntity<>(authResponse, HttpStatus.OK);
        } catch (RuntimeException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.UNAUTHORIZED);
        }
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(@RequestParam String refreshToken) {
        try {
            AuthResponse authResponse = userService.refresh(refreshToken);
            return new ResponseEntity<>(authResponse, HttpStatus.OK);
        } catch (RuntimeException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.UNAUTHORIZED);
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestParam String refreshToken) {
        try {
            userService.logout(refreshToken);
            return new ResponseEntity<>("Logged out successfully", HttpStatus.OK);
        } catch (DataAccessException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (RuntimeException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    @PostMapping("/{id}/password/reset")
    public ResponseEntity<?> createPasswordResetLink(@PathVariable Long id) {
        if (!securityUtils.isCurrentUser(id) && !securityUtils.isCurrentUserAdmin()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(null);
        }
        try {
            userService.createNewPassLinkAndSendToMail(id);
            return new ResponseEntity<>("Password reset link sent to email", HttpStatus.OK);
        } catch (RuntimeException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.NOT_FOUND);
        }
    }

    @PostMapping("/password/update")
    public ResponseEntity<?> updatePassword(@RequestParam UUID requestId,
                                            @RequestParam Integer code,
                                            @RequestParam String newPassword) {
        try {
            userService.updatePassword(requestId, code, newPassword);
            return new ResponseEntity<>("Password updated successfully", HttpStatus.OK);
        } catch (RuntimeException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }
}