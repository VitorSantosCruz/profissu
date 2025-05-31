package br.com.conectabyte.profissu.repositories;

import org.springframework.data.jpa.repository.JpaRepository;

import br.com.conectabyte.profissu.entities.Review;

public interface ReviewRepository extends JpaRepository<Review, Long> {
}
