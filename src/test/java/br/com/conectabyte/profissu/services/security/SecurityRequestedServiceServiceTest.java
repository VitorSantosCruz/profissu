package br.com.conectabyte.profissu.services.security;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import br.com.conectabyte.profissu.exceptions.ResourceNotFoundException;
import br.com.conectabyte.profissu.services.RequestedServiceService;
import br.com.conectabyte.profissu.utils.AddressUtils;
import br.com.conectabyte.profissu.utils.RequestedServiceUtils;
import br.com.conectabyte.profissu.utils.UserUtils;

@ExtendWith(MockitoExtension.class)
public class SecurityRequestedServiceServiceTest {
  @Mock
  private RequestedServiceService requestedServiceService;

  @Mock
  private SecurityService mockedSecurityService;

  @InjectMocks
  private SecurityRequestedServiceService securityRequestedServiceService;

  @Test
  void shouldReturnTrueWhenUserIsOwnerOfRequestedService() {
    final var user = UserUtils.create();
    final var address = AddressUtils.create(user);
    final var requestedService = RequestedServiceUtils.create(user, address);

    user.setId(0L);
    when(requestedServiceService.findById(any())).thenReturn(requestedService);
    when(mockedSecurityService.isOwner(any())).thenReturn(true);

    final var isOwner = securityRequestedServiceService.ownershipCheck(user.getId());

    assertTrue(isOwner);
  }

  @Test
  void shouldReturnFalseWhenUserIsNotOwnerOfRequestedService() {
    final var user = UserUtils.create();
    final var address = AddressUtils.create(user);
    final var requestedService = RequestedServiceUtils.create(user, address);

    when(requestedServiceService.findById(any())).thenReturn(requestedService);

    final var isOwner = securityRequestedServiceService.ownershipCheck(any());

    assertFalse(isOwner);
  }

  @Test
  void shouldReturnFalseWhenRequestedServiceNotFound() {
    when(requestedServiceService.findById(any()))
        .thenThrow(new ResourceNotFoundException("Requested service not found"));

    final var isOwner = securityRequestedServiceService.ownershipCheck(any());

    assertFalse(isOwner);
  }
}
