package br.com.conectabyte.profissu.entities;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "requested_services")
@Getter
@Setter
public class RequestedService {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "created_at", nullable = true)
  private LocalDateTime createdAt;

  @Column(name = "updated_at", nullable = true)
  private LocalDateTime updatedAt;

  @Column(name = "deleted_at", nullable = true)
  private LocalDateTime deletedAt;

  @OneToOne
  @JoinColumn(name = "address_id")
  private Address address;

  @ManyToOne
  @JoinColumn(name = "user_id")
  private User user;
}
