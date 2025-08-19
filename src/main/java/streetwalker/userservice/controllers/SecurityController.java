package streetwalker.userservice.controllers;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import streetwalker.userservice.models.RefreshToken;
import streetwalker.userservice.models.dto.AuthResponse;
import streetwalker.userservice.models.dto.SigninRequest;
import streetwalker.userservice.models.dto.SignupRequest;
import streetwalker.userservice.models.User;
import streetwalker.userservice.repositories.RefreshTokenRepository;
import streetwalker.userservice.repositories.UserRepository;
import streetwalker.userservice.security.JwtCore;
import streetwalker.userservice.services.RoleService;
import streetwalker.userservice.services.StatusService;
import streetwalker.userservice.services.UserService;

import java.util.Date;

@Slf4j
@RestController
@RequestMapping("/auth")
public class SecurityController {

    private UserRepository userRepository;
    private PasswordEncoder passwordEncoder;
    private AuthenticationManager authenticationManager;
    private JwtCore jwtCore;
    private RefreshTokenRepository refreshTokenRepository;
    private RoleService roleService;
    private StatusService statusService;
    private UserService userService;

    @Autowired
    public void setStatusService(StatusService statusService) {
        this.statusService = statusService;
    }
    @Autowired
    public void setRoleService(RoleService roleService) {
        this.roleService = roleService;
    }
    @Autowired
    public void setRefreshTokenRepository(RefreshTokenRepository refreshTokenRepository) {
        this.refreshTokenRepository = refreshTokenRepository;
    }
    @Autowired
    public void setUserRepository(UserRepository userRepository) {
        this.userRepository = userRepository;
    }
    @Autowired
    public void setPasswordEncoder(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }
    @Autowired
    public void setAuthenticationManager(AuthenticationManager authenticationManager) {
        this.authenticationManager = authenticationManager;
    }
    @Autowired
    public void setJwtCore(JwtCore jwtCore) {
        this.jwtCore = jwtCore;
    }


    @PostMapping("/signup")
    public ResponseEntity<?> signup(@RequestBody SignupRequest signupRequest){
        User user;
        try{
            user = userService.signUp(signupRequest);
        }
        catch (RuntimeException e){
            log.error(e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
        log.info("User created" + user.getUsername());
        return ResponseEntity.status(HttpStatus.CREATED).body(user);
    }


//    @PostMapping("/create_admin")
//    public ResponseEntity<?> createAdmin(@RequestBody SignupRequest signupRequest){
//        if (userRepository.existsByUsername(signupRequest.getUsername())){
//            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Username already exists");
//        }
//        if (userRepository.existsByEmail(signupRequest.getEmail())){
//            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Email already exists");
//        }
//
//        User user = new User();
//        user.setUsername(signupRequest.getUsername());
//        user.setEmail(signupRequest.getEmail());
//        user.setPhone(signupRequest.getPhone());
//        user.setPassword(passwordEncoder.encode(signupRequest.getPassword()));
//        user.setRole("ADMIN");
//        userRepository.save(user);
//        return ResponseEntity.status(HttpStatus.CREATED).body(user);
//    }


    @PostMapping("/signin")
    public ResponseEntity<?> signin(@RequestBody SigninRequest signinRequest) {
        User user;
        try{
            user = userRepository.findByPhone(signinRequest.getPhone()).orElseThrow(()  -> new RuntimeException("User not found exception"));

        }
        catch (RuntimeException e){
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
        Authentication authentication = null;
        try {
            authentication = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(user.getUsername(), signinRequest.getPassword()));
        } catch (BadCredentialsException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid username or password");
        }
        SecurityContextHolder.getContext().setAuthentication(authentication);
        String jwt = jwtCore.generateToken(authentication);
        String refresh = jwtCore.generateRefreshToken(user.getUsername());

        RefreshToken refreshTokenEntity = new RefreshToken();
        refreshTokenEntity.setUsername(user.getUsername());
        refreshTokenEntity.setToken(refresh);
        refreshTokenEntity.setExpiryDate(new Date(System.currentTimeMillis() + jwtCore.getRefreshTokenLifetime()));
        refreshTokenRepository.save(refreshTokenEntity);
        return ResponseEntity.status(HttpStatus.OK).body(new AuthResponse(jwt,refresh, signinRequest.getPhone()));
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(@RequestParam String refreshToken) {
        // Проверяем, существует ли refresh token в базе данных
        RefreshToken storedToken = refreshTokenRepository.findByToken(refreshToken)
                .orElseThrow(() -> new RuntimeException("Invalid refresh token"));

        // Проверяем, не истек ли refresh token
        if (storedToken.getExpiryDate().before(new Date())) {
            refreshTokenRepository.delete(storedToken);
            throw new RuntimeException("Refresh token expired");
        }
        // Генерируем новый access token
        String username = jwtCore.getUserNameFromJwt(refreshToken);
        log.info("Extracted username from token: {}", username);

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found exception"));;

        String newAccessToken = jwtCore.generateToken(user);
        String newRefreshToken = jwtCore.generateRefreshToken(username);
        storedToken.setToken(newRefreshToken);
        storedToken.setExpiryDate(new Date(System.currentTimeMillis() + jwtCore.getRefreshTokenLifetime()));
        refreshTokenRepository.save(storedToken);

        return ResponseEntity.ok(new AuthResponse(newAccessToken, newRefreshToken, ""));
    }
    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestParam String refreshToken) {
        refreshTokenRepository.findByToken(refreshToken).ifPresent(refreshTokenRepository::delete);
        return ResponseEntity.ok("Logged out successfully");
    }

    @Autowired
    public void setUserService(UserService userService) {
        this.userService = userService;
    }


    //new pass
//
//    @PostMapping("/update_password")
//    public ResponseEntity<?> updatePassword(@RequestParam String login){
//        User user = userRepository.findByPhone(login).ifPresent(this::);
//        if (!userRepository.existsByPhone(updatePasswordRequest.getUsername())){
//            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("User not found");
//        }
//        User user = userRepository.findByUsername(updatePasswordRequest.getUsername()).orElseThrow(() -> new RuntimeException("User not found"));
//    }


}
