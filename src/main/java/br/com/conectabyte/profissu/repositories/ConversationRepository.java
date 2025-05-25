package br.com.conectabyte.profissu.repositories;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import br.com.conectabyte.profissu.entities.Conversation;

public interface ConversationRepository extends JpaRepository<Conversation, Long> {
  @Query("FROM Conversation c WHERE c.requester.id = :userId")
  Page<Conversation> findByUserId(Long userId, Pageable pageable);

  @Query("""
      SELECT CASE WHEN COUNT(c) > 0 THEN true ELSE false END
      FROM Conversation c
      WHERE c.id = :conversationId AND (
        c.requester.id = :userId OR c.serviceProvider.id = :userId
      )
      """)
  boolean isUserInConversation(@Param("userId") Long userId, @Param("conversationId") Long conversationId);
}
