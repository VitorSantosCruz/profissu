package br.com.conectabyte.profissu.services;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

import br.com.conectabyte.profissu.enums.ContactTypeEnum;
import br.com.conectabyte.profissu.mappers.UserMapper;
import br.com.conectabyte.profissu.repositories.UserRepository;
import br.com.conectabyte.profissu.utils.AddressUtils;
import br.com.conectabyte.profissu.utils.ContactUtils;
import br.com.conectabyte.profissu.utils.UserUtils;

@SpringBootTest
@ActiveProfiles("test")
public class UserServiceTest {
  private final UserMapper userMapper = UserMapper.INSTANCE;

  @MockBean
  private UserRepository userRepository;

  @Autowired
  private UserService userService;

  @Test
  void shouldReturnUserWhenEmailIsValid() {
    final var email = "test@conectabyte.com.br";
    final var user = UserUtils.create();
    user.setContacts(List.of(ContactUtils.create(user)));

    when(userRepository.findByEmail(any())).thenReturn(Optional.of(user));
    final var optionalUser = userService.findByEmail(email);

    assertTrue(optionalUser.isPresent());
    assertTrue(optionalUser.get().getContacts().stream()
        .filter(c -> c.getValue().equals(email) && c.getType().equals(ContactTypeEnum.EMAIL) && c.isStandard()
            && c.getVerificationCompletedAt() != null)
        .findAny().isPresent());
  }

  @Test
  void shouldNotReturnUserWhenEmailIsInvalid() {
    when(userRepository.findByEmail(any())).thenReturn(Optional.empty());
    final var optionalUser = userService.findByEmail("invalid@conectabyte.com.br");

    assertTrue(optionalUser.isEmpty());
  }

  @Test
  void shouldRegisterUserWhenUserDataIsValid() {
    final var user = UserUtils.create();
    user.setContacts(List.of(ContactUtils.create(user)));
    user.setAddresses(List.of(AddressUtils.create(user)));
    when(userRepository.save(any())).thenReturn(user);

    final var savedUser = userService.save(userMapper.userToUserRequestDto(user));

    assertTrue(savedUser != null);
  }
}
