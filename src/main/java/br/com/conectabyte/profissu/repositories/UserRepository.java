package br.com.conectabyte.profissu.repositories;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import br.com.conectabyte.profissu.entities.User;

public interface UserRepository extends JpaRepository<User, Long> {
  @Query("FROM User u JOIN FETCH u.contacts c WHERE c.value = :email AND c.type = 'EMAIL' AND c.standard AND c.deletedAt IS NULL")
  Optional<User> findByEmail(@Param("email") String email);
}
