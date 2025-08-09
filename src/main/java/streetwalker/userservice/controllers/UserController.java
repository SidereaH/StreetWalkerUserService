package streetwalker.userservice.controllers;

import org.apache.coyote.BadRequestException;
import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import streetwalker.userservice.dto.UserCreateDTO;
import streetwalker.userservice.dto.UserDTO;
import streetwalker.userservice.dto.UserUpdateDTO;
import streetwalker.userservice.mappers.UserMapper;
import streetwalker.userservice.models.User;
import streetwalker.userservice.repositories.UserRepository;

@RestController
@RequestMapping("/users")
public class UserController {
    final UserRepository userRepository;
    final UserMapper userMapper;
    public UserController(UserRepository userRepository, UserMapper userMapper) {this.userRepository = userRepository;
        this.userMapper = userMapper;
    }
    @GetMapping
    public ResponseEntity<Page<User>> getAllUsers(Pageable pageable) {
        if(pageable != null) {
            return new ResponseEntity<>(userRepository.findAll(pageable), HttpStatus.OK);
        }
        return new ResponseEntity<>(null, HttpStatus.BAD_REQUEST);
    }
    @GetMapping("/global/{global_id}")
    public ResponseEntity<User> getUserByGlobal(@PathVariable("global_id") Long globalid) {
        if (globalid != null ){
            try{
                return new ResponseEntity<>(userRepository.findByStreetId(globalid).orElseThrow(() -> new BadRequestException("No such user")), HttpStatus.OK);
            }
            catch (BadRequestException e) {
                return new ResponseEntity<>(null, HttpStatus.BAD_REQUEST);
            }
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);

    }
    @GetMapping("/{id}")
    public ResponseEntity<User> getUser(@PathVariable("id") Long id) {
        if (id != null ){
            try{
                return new ResponseEntity<>(userRepository.findById(id).orElseThrow(() -> new BadRequestException("No such user")), HttpStatus.OK);
            }
            catch (BadRequestException e) {
                return new ResponseEntity<>(null, HttpStatus.BAD_REQUEST);
            }
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);

    }
    @PostMapping
    public ResponseEntity<UserDTO>  create(@RequestBody UserCreateDTO userData) {
        // Преобразование в сущность
        var user = userMapper.map(userData);
        try{
            userRepository.save(user);

        } catch (DataAccessException e){
            return ResponseEntity.status(HttpStatus.CONFLICT).body(null);
        }
        // Преобразование в DTO
        var userDTO = userMapper.map(user);
        return new ResponseEntity<>(userDTO, HttpStatus.CREATED);
    }

    @PutMapping("/posts/{id}")
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<UserDTO> update(@RequestBody @Validated UserUpdateDTO userData, @PathVariable Long id) {
        var user = new User();
        try{
             user = userRepository.findById(id)
                    .orElseThrow(() -> new BadRequestException("Not Found"));
        }
        catch (BadRequestException e){
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }

        userMapper.update(userData, user);
        userRepository.save(user);
        var userDTO = userMapper.map(user);
        return new ResponseEntity<>(userDTO, HttpStatus.CREATED);
    }
}
