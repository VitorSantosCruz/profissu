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
import lombok.Data;

@Entity
@Table(name = "addresses")
@Data
public class Address {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "created_at", nullable = false)
  private LocalDateTime createdAt = LocalDateTime.now();

  @Column(name = "updated_at")
  private LocalDateTime updatedAt;

  @Column(nullable = false)
  private String street;

  @Column(nullable = false)
  private String number;

  @Column(nullable = false)
  private String city;

  @Column(nullable = false)
  private String state;

  @Column(name = "zip_code", nullable = false)
  private String zipCode;

  @Column(name = "deleted_at")
  private LocalDateTime deletedAt;

  @OneToOne(mappedBy = "address")
  private RequestedService requestedService;

  @ManyToOne
  @JoinColumn(name = "user_id")
  private User user;
}
