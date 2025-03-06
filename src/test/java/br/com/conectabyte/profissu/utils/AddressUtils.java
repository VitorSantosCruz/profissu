package br.com.conectabyte.profissu.utils;

import br.com.conectabyte.profissu.entities.Address;
import br.com.conectabyte.profissu.entities.User;

public class AddressUtils {
  public static Address create(User user) {
    var address = new Address();

    address.setId(0L);
    address.setStreet("123 Main St");
    address.setNumber("101");
    address.setCity("Springfield");
    address.setState("IL");
    address.setZipCode("44444-876");
    address.setUser(user);

    return address;
  }
}
