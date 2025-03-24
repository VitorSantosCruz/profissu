package br.com.conectabyte.profissu.entities;

import java.time.LocalDateTime;
import java.util.List;

import br.com.conectabyte.profissu.enums.OfferStatusEnum;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Table(name = "conversations")
@Data
public class Conversation {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "created_at", nullable = false)
  private LocalDateTime createdAt = LocalDateTime.now();

  @Column(name = "updated_at")
  private LocalDateTime updatedAt;

  @Column(name = "offer_status", nullable = false)
  @Enumerated(EnumType.STRING)
  private OfferStatusEnum offerStatus;

  @ManyToOne
  @JoinColumn(name = "requester_id", nullable = false)
  private User requester;

  @ManyToOne
  @JoinColumn(name = "service_provider_id", nullable = false)
  private User serviceProvider;

  @ManyToOne
  @JoinColumn(name = "requested_service_id", nullable = false)
  private RequestedService requestedService;

  @OneToMany(mappedBy = "conversation")
  private List<Message> messages;
}
