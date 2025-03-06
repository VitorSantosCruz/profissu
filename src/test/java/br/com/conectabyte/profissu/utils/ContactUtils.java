package br.com.conectabyte.profissu.utils;

import java.time.LocalDateTime;

import br.com.conectabyte.profissu.entities.Contact;
import br.com.conectabyte.profissu.entities.User;
import br.com.conectabyte.profissu.enums.ContactTypeEnum;

public class ContactUtils {
  public static Contact createEmail(User user) {
    final var contact = new Contact();

    contact.setId(0L);
    contact.setType(ContactTypeEnum.EMAIL);
    contact.setValue("test@conectabyte.com.br");
    contact.setStandard(true);
    contact.setVerificationRequestedAt(LocalDateTime.now());
    contact.setVerificationCompletedAt(LocalDateTime.now());
    contact.setUser(user);

    return contact;
  }

  public static Contact createPhone(User user) {
    final var contact = new Contact();

    contact.setId(0L);
    contact.setType(ContactTypeEnum.PHONE);
    contact.setValue("1111233009988");
    contact.setStandard(true);
    contact.setVerificationRequestedAt(LocalDateTime.now());
    contact.setVerificationCompletedAt(LocalDateTime.now());
    contact.setUser(user);

    return contact;
  }
}
