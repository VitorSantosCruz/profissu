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
@Table(name = "addresses")
@Getter
@Setter
public class Address {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "created_at", nullable = true)
  private LocalDateTime createdAt;

  @Column(name = "updated_at", nullable = true)
  private LocalDateTime updatedAt;

  @Column(nullable = true)
  private String street;

  @Column(nullable = true)
  private String number;

  @Column(nullable = true)
  private String city;

  @Column(nullable = true)
  private String state;

  @Column(name = "zip_code", nullable = true)
  private String zipCode;

  @Column(name = "deleted_at", nullable = true)
  private LocalDateTime deletedAt;

  @OneToOne(mappedBy = "address")
  private RequestedService requestedService;

  @ManyToOne
  @JoinColumn(name = "user_id")
  private User user;
}
