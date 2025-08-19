package streetwalker.userservice.controllers;

import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import streetwalker.userservice.models.dto.AuthResponse;
import streetwalker.userservice.models.dto.SigninRequest;
import streetwalker.userservice.models.dto.SignupRequest;
import streetwalker.userservice.models.dto.UserDTO;
import streetwalker.userservice.services.UserService;

@Slf4j
@RestController
@RequestMapping("/auth")
public class SecurityController {

    private final UserService userService;

    public SecurityController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/signup")
    public ResponseEntity<?> signup(@RequestBody SignupRequest userCreateDTO){
        UserDTO user;
        try{
            user = userService.create(userCreateDTO);
        }
        catch (RuntimeException e){
            log.error(e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
        log.info("User created {}", user.getUsername());
        return ResponseEntity.status(HttpStatus.CREATED).body(user);
    }


    @PostMapping("/signin")
    public ResponseEntity<?> signin(@RequestBody SigninRequest signinRequest) {
        AuthResponse response;
        try{
            response = userService.signin(signinRequest);

        } catch (RuntimeException e){
            log.error(e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(@RequestParam String refreshToken) {
        AuthResponse response;
        try{
            response = userService.refresh(refreshToken);
        } catch (RuntimeException e){
            log.error(e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
        return ResponseEntity.ok(response);
    }
    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestParam String refreshToken) {
        try{
            userService.logout(refreshToken);
        } catch (DataAccessException e){
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
        return ResponseEntity.ok("Logged out successfully");
    }



    //new pass
////
//    @PostMapping("/update_password")
//    public ResponseEntity<?> updatePassword(@RequestParam String login){
//        User user = userRepository.findByPhone(login).ifPresent(this::);
//        if (!userRepository.existsByPhone(updatePasswordRequest.getUsername())){
//            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("User not found");
//        }
//        User user = userRepository.findByUsername(updatePasswordRequest.getUsername()).orElseThrow(() -> new RuntimeException("User not found"));
//    }


}
