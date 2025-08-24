package streetwalker.userservice.mappers;

import org.mapstruct.*;
import streetwalker.userservice.dto.SignupRequest;
import streetwalker.userservice.dto.UserCreateDTO;
import streetwalker.userservice.dto.UserDTO;
import streetwalker.userservice.dto.UserUpdateDTO;
import streetwalker.userservice.models.User;

@Mapper(
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
        componentModel = MappingConstants.ComponentModel.SPRING,
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        uses = { StatusMapper.class, RoleMapper.class }
)
public interface UserMapper {

    @Mapping(target = "description", defaultValue = " ")
    @Mapping(target = "avatarUrl", defaultValue = "https://cdn.lifehacker.ru/wp-content/uploads/2018/04/vk_1610723985.jpg")
    User map(UserCreateDTO dto);
    User map(SignupRequest dto);
    UserDTO map(User model);
    void update(UserUpdateDTO dto, @MappingTarget User model);

}
