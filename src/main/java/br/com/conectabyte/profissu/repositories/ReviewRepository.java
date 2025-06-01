package br.com.conectabyte.profissu.repositories;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import br.com.conectabyte.profissu.entities.Review;

public interface ReviewRepository extends JpaRepository<Review, Long> {
  @Query("""
      FROM Review r
        WHERE r.id = :id
        AND r.deletedAt IS NULL
      """)
  Optional<Review> findById(@Param("id") Long id);

  @Query("""
      FROM Review r
        WHERE r.user.id = :userId
        AND r.deletedAt IS NULL
      """)
  Page<Review> findReviewsGivenByUserId(Long userId, Pageable pageable);

  @Query("""
      FROM Review r
        WHERE r.user.id <> :userId
        AND r.requestedService.id
        IN (
          SELECT DISTINCT rs.id
            FROM RequestedService rs
            JOIN rs.conversations c
              WHERE c.requester.id = :userId
              OR c.serviceProvider.id = :userId
        )
        AND r.deletedAt IS NULL
      """)
  Page<Review> findReviewsReceivedByUserId(Long userId, Pageable pageable);
}
