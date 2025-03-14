package br.com.conectabyte.profissu.utils;

import br.com.conectabyte.profissu.entities.Address;
import br.com.conectabyte.profissu.entities.RequestedService;
import br.com.conectabyte.profissu.entities.User;
import br.com.conectabyte.profissu.enums.RequestedServiceStatusEnum;

public class RequestedServiceUtils {
  public static RequestedService create(User user, Address address) {
    final var requestedService = new RequestedService();

    requestedService.setId(0L);
    requestedService.setTitle("Title");
    requestedService.setDescription("Description");
    requestedService.setStatus(RequestedServiceStatusEnum.PENDING);
    requestedService.setAddress(address);
    requestedService.setUser(user);

    return requestedService;
  }
}
