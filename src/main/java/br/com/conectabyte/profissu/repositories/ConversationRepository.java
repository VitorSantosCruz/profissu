package br.com.conectabyte.profissu.repositories;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import br.com.conectabyte.profissu.entities.Conversation;

public interface ConversationRepository extends JpaRepository<Conversation, Long> {
  @Query("FROM Conversation c WHERE c.requester.id = :userId")
  Page<Conversation> findByUserId(Long userId, Pageable pageable);
}
