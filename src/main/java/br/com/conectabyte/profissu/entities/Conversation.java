package br.com.conectabyte.profissu.entities;

import java.time.LocalDateTime;
import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "conversations")
@Getter
@Setter
public class Conversation {
@Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "created_at", nullable = true)
  private LocalDateTime createdAt;

  @Column(name = "updated_at", nullable = true)
  private LocalDateTime updatedAt;

  @Column(name = "is_active", nullable = true)
  private boolean isActive;

  @ManyToOne
  @JoinColumn(name = "requester_id")
  private User requester;

  @ManyToOne
  @JoinColumn(name = "service_provider_id")
  private User serviceProvider;

  @OneToMany(mappedBy = "conversation")
  private List<Message> messages;
}
