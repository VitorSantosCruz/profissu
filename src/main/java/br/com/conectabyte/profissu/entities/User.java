package br.com.conectabyte.profissu.entities;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import org.springframework.security.crypto.password.PasswordEncoder;

import br.com.conectabyte.profissu.dtos.LoginRequestDto;
import br.com.conectabyte.profissu.enums.GenderEnum;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "users")
@Getter
@Setter
public class User {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "created_at", nullable = false)
  private LocalDateTime createdAt = LocalDateTime.now();

  @Column(name = "updated_at")
  private LocalDateTime updatedAt;

  @Column(nullable = false)
  private String name;

  @Column(nullable = false)
  private String password;

  @Column(nullable = false)
  @Enumerated(EnumType.STRING)
  private GenderEnum gender;

  @Column(name = "deleted_at")
  private LocalDateTime deletedAt;

  @OneToOne(mappedBy = "user", cascade = CascadeType.ALL)
  private Profile profile;

  @Column(nullable = false)
  @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
  private List<Contact> contacts;

  @Column(nullable = false)
  @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
  private List<Address> addresses;

  @OneToMany(mappedBy = "requester")
  private List<Conversation> conversationsAsARequester;

  @OneToMany(mappedBy = "serviceProvider")
  private List<Conversation> conversationsAsAServiceProvider;

  @OneToMany(mappedBy = "user")
  private List<Message> messages;

  @Column(nullable = false)
  @ManyToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
  @JoinTable(name = "users_roles", joinColumns = @JoinColumn(name = "user_id"), inverseJoinColumns = @JoinColumn(name = "role_id"))
  private Set<Role> roles;

  public boolean isValidPassword(LoginRequestDto loginRequest, PasswordEncoder passwordEncoder) {
    return passwordEncoder.matches(loginRequest.password(), this.password);
  }
}
