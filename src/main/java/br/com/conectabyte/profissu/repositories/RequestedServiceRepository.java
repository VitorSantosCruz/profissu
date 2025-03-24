package br.com.conectabyte.profissu.repositories;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import br.com.conectabyte.profissu.entities.RequestedService;

public interface RequestedServiceRepository extends JpaRepository<RequestedService, Long> {
  @Query("""
      FROM RequestedService rs
        WHERE rs.status = 'PENDING'
        AND rs.deletedAt IS NULL
      """)
  Page<RequestedService> findAvailableServiceRequestsByPage(Pageable pageable);
}
