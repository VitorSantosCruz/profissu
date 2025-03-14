package br.com.conectabyte.profissu.utils;

import java.time.LocalDateTime;

import br.com.conectabyte.profissu.entities.Contact;
import br.com.conectabyte.profissu.entities.User;

public class ContactUtils {
  public static Contact create(User user) {
    final var contact = new Contact();

    contact.setId(0L);
    contact.setValue("test@conectabyte.com.br");
    contact.setStandard(true);
    contact.setVerificationRequestedAt(LocalDateTime.now());
    contact.setVerificationCompletedAt(LocalDateTime.now());
    contact.setUser(user);

    return contact;
  }
}
