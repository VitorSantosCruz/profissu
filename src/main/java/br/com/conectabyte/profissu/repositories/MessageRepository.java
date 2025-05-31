package br.com.conectabyte.profissu.repositories;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import br.com.conectabyte.profissu.entities.Conversation;
import br.com.conectabyte.profissu.entities.Message;

public interface MessageRepository extends JpaRepository<Message, Long> {
  @Query("FROM Message m WHERE m.conversation.id = :conversationId ORDER BY m.createdAt DESC")
  Page<Message> listMessages(Long conversationId, Pageable pageable);

  @Query("""
      SELECT m.conversation
        FROM Message m
          WHERE m.read IS FALSE AND m.notificationSent IS FALSE AND m.createdAt < :thresholdDate
      """)
  List<Conversation> findConversationsWithUnreadMessages(LocalDateTime thresholdDate);
}
