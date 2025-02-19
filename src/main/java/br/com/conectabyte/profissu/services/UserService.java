package br.com.conectabyte.profissu.services;

import java.util.Optional;
import java.util.Set;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.conectabyte.profissu.dtos.UserRequestDto;
import br.com.conectabyte.profissu.dtos.UserResponseDto;
import br.com.conectabyte.profissu.entities.Profile;
import br.com.conectabyte.profissu.entities.Role;
import br.com.conectabyte.profissu.entities.User;
import br.com.conectabyte.profissu.enums.RoleEnum;
import br.com.conectabyte.profissu.mappers.UserMapper;
import br.com.conectabyte.profissu.repositories.RoleRepository;
import br.com.conectabyte.profissu.repositories.UserRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserService {
  private final UserRepository userRepository;
  private final RoleRepository roleRepository;
  private final BCryptPasswordEncoder bCryptPasswordEncoder;

  private final UserMapper userMapper = UserMapper.INSTANCE;

  @Transactional(readOnly = true)
  public Optional<User> findByEmail(String email) {
    return userRepository.findByEmail(email);
  }

  @Transactional
  public UserResponseDto registerUser(UserRequestDto userDto) {
    var userToBeSaved = userMapper.userRequestDtoToUser(userDto);
    userToBeSaved.setPassword(bCryptPasswordEncoder.encode(userDto.password()));
    userToBeSaved.getContacts().forEach(c -> c.setUser(userToBeSaved));
    userToBeSaved.getAddresses().forEach(a -> a.setUser(userToBeSaved));
    userToBeSaved.setProfile(Profile.builder().user(userToBeSaved).build());
    userToBeSaved.setRoles(
        Set.of(roleRepository.findByName(RoleEnum.USER.toString())
            .orElse(Role.builder().name("USER").build())));

    var user = userRepository.save(userToBeSaved);

    return userMapper.userToUserResponseDto(user);
  }
}
