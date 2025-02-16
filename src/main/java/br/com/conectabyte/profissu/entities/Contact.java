package br.com.conectabyte.profissu.entities;

import java.time.LocalDateTime;

import br.com.conectabyte.profissu.enums.ContactTypeEnum;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "contacts")
@Getter
@Setter
public class Contact {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "created_at", nullable = true)
  private LocalDateTime createdAt;

  @Column(name = "updated_at", nullable = true)
  private LocalDateTime updatedAt;

  @Column(nullable = true)
  @Enumerated(EnumType.STRING)
  private ContactTypeEnum type;

  @Column(nullable = true)
  private String value;

  @Column(nullable = true)
  private boolean standard;

  @Column(name = "verification_requested_at", nullable = true)
  private LocalDateTime verificationRequestedAt;

  @Column(name = "verification_completed_at", nullable = true)
  private LocalDateTime verificationCompletedAt;

  @Column(name = "deleted_at", nullable = true)
  private LocalDateTime deletedAt;

  @ManyToOne
  @JoinColumn(name = "user_id")
  private User user;
}
