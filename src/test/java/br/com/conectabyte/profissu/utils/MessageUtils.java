package br.com.conectabyte.profissu.utils;

import br.com.conectabyte.profissu.entities.Conversation;
import br.com.conectabyte.profissu.entities.Message;
import br.com.conectabyte.profissu.entities.User;

public class MessageUtils {
  public static Message create(User user, Conversation conversation) {
    final var message = new Message();

    message.setMessage("Teste");
    message.setRead(false);
    message.setUser(user);
    message.setConversation(conversation);

    return message;
  }
}
