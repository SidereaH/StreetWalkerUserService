package streetwalker.userservice.controllers;

import org.apache.coyote.BadRequestException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import streetwalker.userservice.models.User;
import streetwalker.userservice.repositories.UserRepository;

@RestController
@RequestMapping("/users")
public class UserController {
    final UserRepository userRepository;
    public UserController(UserRepository userRepository) {this.userRepository = userRepository;}
    @GetMapping
    public ResponseEntity<Page<User>> getAllUsers(Pageable pageable) {
        if(pageable != null) {
            return new ResponseEntity<>(userRepository.findAll(pageable), HttpStatus.OK);
        }
        return new ResponseEntity<>(null, HttpStatus.BAD_REQUEST);
    }
    @GetMapping("/{global_id}")
    public ResponseEntity<User> getUser(@PathVariable("global_id") Long globalid) {
        if (globalid != null ){
            try{
                return new ResponseEntity<>(userRepository.findByStreetId(globalid).orElseThrow(() -> new BadRequestException("No such user")), HttpStatus.OK);
            }
            catch (BadRequestException e) {
                return new ResponseEntity<>(null, HttpStatus.BAD_REQUEST);
            }
        }
        else{
            return new ResponseEntity<>(null, HttpStatus.BAD_REQUEST);
        }
    }
}
