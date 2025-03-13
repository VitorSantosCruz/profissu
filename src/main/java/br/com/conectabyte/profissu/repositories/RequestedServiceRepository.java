package br.com.conectabyte.profissu.repositories;

import org.springframework.data.jpa.repository.JpaRepository;

import br.com.conectabyte.profissu.entities.RequestedService;

public interface RequestedServiceRepository extends JpaRepository<RequestedService, Long> {
}
