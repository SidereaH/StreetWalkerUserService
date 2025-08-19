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
import streetwalker.userservice.models.RefreshToken;
import streetwalker.userservice.mappers.UserMapper;
import streetwalker.userservice.models.User;
import streetwalker.userservice.models.dto.*;
import streetwalker.userservice.repositories.UserRepository;
import streetwalker.userservice.security.JwtCore;

import java.util.Optional;
@Slf4j
@Service
public class UserService implements UserDetailsService {
    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final RoleService roleService;
    private final StatusService statusService;
    private final AuthenticationManager authenticationManager;
    private final RefreshTokenService refreshTokenService;
    private final JwtCore jwtCore;
    public UserService(UserRepository userRepository, UserMapper userMapper, PasswordEncoder passwordEncoder, RoleService roleService, StatusService statusService, AuthenticationManager authenticationManager, RefreshTokenService refreshTokenService, JwtCore jwtCore) {
        this.userRepository = userRepository;
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.roleService = roleService;
        this.statusService = statusService;
        this.authenticationManager = authenticationManager;
        this.refreshTokenService = refreshTokenService;
        this.jwtCore = jwtCore;
    }
    public Page<User> findAll(Pageable usersPageable) {
            return userRepository.findAll(usersPageable);
    }
    public Optional<User> findUserById(Long id)  {
        return userRepository.findById(id);
    }
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with username: " + username));
    }
    @Transactional
    public UserDTO create(SignupRequest userData) throws RuntimeException {
        if (userRepository.existsByUsername(userData.getUsername())) {
            log.error("User already exists with username: {}", userData.getUsername());
            throw new RuntimeException("User already exists with username: " + userData.getUsername());
        } else if (userRepository.existsByEmail(userData.getEmail())) {
            log.error("User already exists with email: {}", userData.getEmail());
            throw new RuntimeException("User already exists with email: " + userData.getEmail());
        } else if (userRepository.existsByPhone(userData.getPhone())) {
            log.error("User already exists with phone: {}", userData.getPhone());
            throw new RuntimeException("User already exists with phone: " + userData.getPhone());
        }
        userData.setPassword(passwordEncoder.encode(userData.getPassword()));
        User user = userMapper.map(userData);
        userRepository.save(user);

        return userMapper.map(user);
    }
    public UserDTO update(UserUpdateDTO userData, Long id ) throws BadRequestException {
        var user = userRepository.findById(id)
                    .orElseThrow(() -> new BadRequestException("Not Found"));

        userMapper.update(userData, user);
        userRepository.save(user);
        return userMapper.map(user);
    }
//    @Transactional
//    public User signUp(SignupRequest request){
//        System.out.println(request);
//        if (userRepository.existsByUsername(request.getUsername())) {
//            log.error("User already exists with username: {}", request.getUsername());
//            throw new RuntimeException("User already exists with username: " + request.getUsername());
//        } else if (userRepository.existsByEmail(request.getEmail())) {
//            log.error("User already exists with email: {}", request.getEmail());
//            throw new RuntimeException("User already exists with email: " + request.getEmail());
//        } else if (userRepository.existsByPhone(request.getPhone())) {
//            log.error("User already exists with phone: {}", request.getPhone());
//            throw new RuntimeException("User already exists with phone: " + request.getPhone());
//        }
//        User user = new User();
//        user.setUsername(request.getUsername());
//        user.setEmail(request.getEmail());
//        user.setPhone(request.getPhone());
//        user.setPassword(passwordEncoder.encode(request.getPassword()));
//        user.setRole(roleService.getDefaultRole());
//        user.setStatus(statusService.getDefaultStatus());
//        log.info("User created: " + user);
//        userRepository.save(user);
//        return user;
//    }

    public AuthResponse signin (SigninRequest signinRequest) throws RuntimeException, BadCredentialsException{
        User user = userRepository.findByPhone(signinRequest.getPhone()).orElseThrow(()  -> new RuntimeException("User not found exception"));


        Authentication authentication = null;
        try {
            authentication = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(user.getUsername(), signinRequest.getPassword()));
        } catch (BadCredentialsException e) {
            throw new BadCredentialsException("Invalid username or password");
        }
        SecurityContextHolder.getContext().setAuthentication(authentication);
        String jwt = jwtCore.generateToken(authentication);
        RefreshToken refresh = refreshTokenService.create(user);

        return new AuthResponse(jwt, refresh.getToken());
    }
    public AuthResponse refresh(String refreshToken) throws RuntimeException {
        String username = refreshTokenService.check(refreshToken);

        // Генерируем новый access token
        log.info("Extracted username from token: {}", username);

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found exception"));;

        String newAccessToken = jwtCore.generateToken(user);
        String newRefreshToken = refreshTokenService.create(user).getToken();
        return new AuthResponse(newAccessToken, newRefreshToken);
    }
    public void logout( String refreshToken) throws DataAccessException {
        refreshTokenService.delete(refreshToken);
    }


}
