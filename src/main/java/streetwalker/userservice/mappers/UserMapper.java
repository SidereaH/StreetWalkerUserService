package streetwalker.userservice.mappers;

import org.mapstruct.*;
import streetwalker.userservice.dto.UserCreateDTO;
import streetwalker.userservice.dto.UserDTO;
import streetwalker.userservice.dto.UserUpdateDTO;
import streetwalker.userservice.models.User;

@Mapper(
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
        componentModel = MappingConstants.ComponentModel.SPRING,
        unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public abstract class UserMapper {
    @Mapping(target = "bio", defaultValue = " ")
    @Mapping(target = "avatarUrl", defaultValue = "https://cdn.lifehacker.ru/wp-content/uploads/2018/04/vk_1610723985.jpg")
    public abstract User map(UserCreateDTO dto);
    public abstract UserDTO map(User model);
    public abstract void update(UserUpdateDTO dto, @MappingTarget User model);
}
