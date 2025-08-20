package streetwalker.userservice.controllers;

import org.apache.coyote.BadRequestException;
import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import streetwalker.userservice.models.dto.UserCreateDTO;
import streetwalker.userservice.models.dto.UserDTO;
import streetwalker.userservice.models.dto.UserUpdateDTO;
import streetwalker.userservice.models.User;
import streetwalker.userservice.services.UserService;

@RestController
@RequestMapping("/users")
public class UserController {

    final UserService userService;
    public UserController(UserService userService) {
        this.userService = userService;
    }
    @GetMapping
    public ResponseEntity<Page<User>> getAllUsers(Pageable pageable) {
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
        // Преобразование в сущность
        try{
            user = userService.create(userData);

        } catch (DataAccessException e) {
            user = new UserDTO();
            user.setUsername(e.getLocalizedMessage());
            return new ResponseEntity<>(user, HttpStatus.BAD_REQUEST);
        }
        return new ResponseEntity<>(user, HttpStatus.CREATED);

    }

    @PutMapping("/users/{id}")
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<UserDTO> update(@RequestBody @Validated UserUpdateDTO userData, @PathVariable Long id) {
        UserDTO user;
        try {
            user = userService.update(userData, id);
        } catch (BadRequestException e) {
            var userDTO = new UserDTO();
            userDTO.setUsername(e.getLocalizedMessage());
            return new ResponseEntity<>(userDTO, HttpStatus.BAD_REQUEST);
        }
        return new ResponseEntity<>(user, HttpStatus.CREATED);
    }

    @GetMapping("/search/username")
    public ResponseEntity<?> searchUsersByUsername(Pageable pageable, @RequestParam String username) {
        Page<User> users;
        try{
             users = userService.findAllByUsernameSub(pageable, username);
        }
        catch (RuntimeException e) {
            return new ResponseEntity<>(e.getLocalizedMessage(), HttpStatus.BAD_REQUEST);
        }
        return new ResponseEntity<>(users, HttpStatus.OK);

    }
}
