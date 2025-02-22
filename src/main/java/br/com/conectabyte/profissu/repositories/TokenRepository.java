package br.com.conectabyte.profissu.repositories;

import org.springframework.data.jpa.repository.JpaRepository;

import br.com.conectabyte.profissu.entities.Token;

public interface TokenRepository extends JpaRepository<Token, Long> {
}
