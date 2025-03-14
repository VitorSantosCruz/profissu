package br.com.conectabyte.profissu.repositories;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import br.com.conectabyte.profissu.entities.User;

public interface UserRepository extends JpaRepository<User, Long> {
  @Query("""
      FROM User u
        WHERE u.id = :id
        AND EXISTS (
          FROM u.contacts c
            WHERE c.standard
            AND c.verificationCompletedAt IS NOT NULL
            AND c.deletedAt IS NULL
        )
        AND u.deletedAt IS NULL
      """)
  Optional<User> findById(@Param("id") Long id);

  @Query("""
      FROM User u
        WHERE EXISTS (
          FROM u.contacts c
            WHERE c.value = :email
            AND c.standard
            AND c.deletedAt IS NULL
        )
      """)
  Optional<User> findByEmail(@Param("email") String email);
}
