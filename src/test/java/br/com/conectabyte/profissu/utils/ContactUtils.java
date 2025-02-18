package br.com.conectabyte.profissu.utils;

import java.time.LocalDateTime;

import br.com.conectabyte.profissu.entities.Contact;
import br.com.conectabyte.profissu.entities.User;
import br.com.conectabyte.profissu.enums.ContactTypeEnum;

public class ContactUtils {
  public static Contact createContact(User user) {
    var contact = new Contact();

    contact.setCreatedAt(LocalDateTime.now());
    contact.setType(ContactTypeEnum.EMAIL);
    contact.setValue("test@conectabyte.com.br");
    contact.setStandard(true);
    contact.setVerificationRequestedAt(LocalDateTime.now());
    contact.setVerificationCompletedAt(LocalDateTime.now());
    contact.setUser(user);

    return contact;
  }
}
