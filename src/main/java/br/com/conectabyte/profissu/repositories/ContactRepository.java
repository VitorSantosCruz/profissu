package br.com.conectabyte.profissu.repositories;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import br.com.conectabyte.profissu.entities.Contact;

public interface ContactRepository extends JpaRepository<Contact, Long> {
  @Query("""
      FROM Contact c
        WHERE c.id = :id
        AND c.deletedAt IS NULL
      """)
  Optional<Contact> findById(@Param("id") Long id);

  @Query("""
      FROM Contact c
        WHERE c.value = :value
        AND c.deletedAt IS NULL
      """)
  Optional<Contact> findByValue(@Param("value") String value);
}
