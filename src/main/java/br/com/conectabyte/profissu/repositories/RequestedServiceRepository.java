package br.com.conectabyte.profissu.repositories;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import br.com.conectabyte.profissu.entities.RequestedService;

public interface RequestedServiceRepository extends JpaRepository<RequestedService, Long> {
  @Query("""
      FROM RequestedService rs
        WHERE rs.status = 'PENDING'
        AND rs.deletedAt IS NULL
      """)
  Page<RequestedService> findAvailableServiceRequests(Pageable pageable);

  @Query("""
      FROM RequestedService rs
        WHERE (
          rs.user.id = :userId
          OR EXISTS (
            FROM rs.conversations c
              WHERE c.serviceProvider.id = :userId
              AND c.offerStatus = 'ACCEPTED'
          )
        )
        AND rs.deletedAt IS NULL
      """)
  Page<RequestedService> findByUserId(@Param("userId") Long userId, Pageable pageable);
}
