package br.com.conectabyte.profissu.repositories;

import org.springframework.data.jpa.repository.JpaRepository;

import br.com.conectabyte.profissu.entities.Message;

public interface MessageRepository extends JpaRepository<Message, Long> {
}
