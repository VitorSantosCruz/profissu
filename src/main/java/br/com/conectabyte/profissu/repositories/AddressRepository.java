package br.com.conectabyte.profissu.repositories;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import br.com.conectabyte.profissu.entities.Address;

public interface AddressRepository extends JpaRepository<Address, Long> {
  @Query("""
      FROM Address a
        WHERE a.id = :id
        AND a.deletedAt IS NULL
      """)
  Optional<Address> findById(@Param("id") Long id);
}
