package streetwalker.userservice.services;

import org.apache.coyote.BadRequestException;
import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import streetwalker.userservice.dto.UserCreateDTO;
import streetwalker.userservice.dto.UserDTO;
import streetwalker.userservice.dto.UserUpdateDTO;
import streetwalker.userservice.mappers.UserMapper;
import streetwalker.userservice.models.User;
import streetwalker.userservice.repositories.UserRepository;

import java.util.Optional;

@Service
public class UserService {
    final UserRepository userRepository;
    final UserMapper userMapper;
    public UserService(UserRepository userRepository, UserMapper userMapper) {
        this.userRepository = userRepository;
        this.userMapper = userMapper;
    }
    public Page<User> findAll(Pageable usersPageable) {
            return userRepository.findAll(usersPageable);
    }
    public Optional<User> findUserById(Long id) {
        return userRepository.findById(id);
    }
    public UserDTO create(UserCreateDTO userData) throws DataAccessException {
        var user = userMapper.map(userData);
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

}
