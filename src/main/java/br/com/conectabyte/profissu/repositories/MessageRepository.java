package br.com.conectabyte.profissu.repositories;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import br.com.conectabyte.profissu.entities.Message;

public interface MessageRepository extends JpaRepository<Message, Long> {
  @Query("FROM Message m WHERE m.conversation.id = :conversationId ORDER BY m.createdAt DESC")
  Page<Message> listMessages(Long conversationId, Pageable pageable);
}
