package br.com.conectabyte.profissu.utils;

import java.util.List;

import br.com.conectabyte.profissu.entities.Conversation;
import br.com.conectabyte.profissu.entities.Message;
import br.com.conectabyte.profissu.entities.RequestedService;
import br.com.conectabyte.profissu.entities.User;
import br.com.conectabyte.profissu.enums.OfferStatusEnum;

public class ConversationUtils {
  public static Conversation create(User requester, User serviceProvider, RequestedService requestedService,
      List<Message> messages) {
    var conversation = new Conversation();

    conversation.setId(0L);
    conversation.setOfferStatus(OfferStatusEnum.PENDING);
    conversation.setRequester(requester);
    conversation.setServiceProvider(serviceProvider);
    conversation.setRequestedService(requestedService);
    conversation.setMessages(messages);

    return conversation;
  }
}
