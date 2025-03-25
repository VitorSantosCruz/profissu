package br.com.conectabyte.profissu.repositories;

import org.springframework.data.jpa.repository.JpaRepository;

import br.com.conectabyte.profissu.entities.Conversation;

public interface ConversationRepository extends JpaRepository<Conversation, Long> {
}
