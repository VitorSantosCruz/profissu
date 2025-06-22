package br.com.conectabyte.profissu.services;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import br.com.conectabyte.profissu.dtos.request.ContactConfirmationRequestDto;
import br.com.conectabyte.profissu.dtos.request.ContactRequestDto;
import br.com.conectabyte.profissu.dtos.request.EmailCodeDto;
import br.com.conectabyte.profissu.dtos.response.ContactResponseDto;
import br.com.conectabyte.profissu.dtos.response.MessageValueResponseDto;
import br.com.conectabyte.profissu.entities.Contact;
import br.com.conectabyte.profissu.exceptions.ResourceNotFoundException;
import br.com.conectabyte.profissu.exceptions.ValidationException;
import br.com.conectabyte.profissu.mappers.ContactMapper;
import br.com.conectabyte.profissu.repositories.ContactRepository;
import br.com.conectabyte.profissu.services.email.ContactConfirmationService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class ContactService {
  private final ContactRepository contactRepository;
  private final UserService userService;
  private final TokenService tokenService;
  private final ContactConfirmationService contactConfirmationService;
  private final BCryptPasswordEncoder bCryptPasswordEncoder;
  private final JwtService jwtService;

  private final ContactMapper contactMapper = ContactMapper.INSTANCE;

  @Transactional
  public ContactResponseDto register(ContactRequestDto contactRequestDto) {
    log.debug("Registering new contact with data: {}", contactRequestDto);

    final var userId = this.jwtService.getClaims()
        .map(claims -> Long.valueOf(claims.get("sub").toString()))
        .orElseThrow();

    log.debug("Retrieved user ID from JWT: {}", userId);

    final var contactToBeSaved = contactMapper.contactRequestDtoToContact(contactRequestDto);
    final var user = this.userService.findById(userId);

    contactToBeSaved.setVerificationRequestedAt(LocalDateTime.now());
    contactToBeSaved.setUser(user);

    final var code = UUID.randomUUID().toString().split("-")[1];

    log.debug("Generated confirmation code: {}", code);

    this.tokenService.deleteByUser(user);
    this.tokenService.flush();
    this.tokenService.save(user, code, bCryptPasswordEncoder);

    log.debug("Token saved for user: {}", user.getId());

    final var savedContact = contactRepository.save(contactToBeSaved);

    log.info("Contact registered successfully with ID: {} for user: {}", savedContact.getId(), user.getId());
    this.contactConfirmationService.send(new EmailCodeDto(contactRequestDto.value(), code));
    log.debug("Contact confirmation email sent to: {}", contactRequestDto.value());
    return contactMapper.contactToContactResponseDto(savedContact);
  }

  @Transactional
  public ContactResponseDto update(Long id, ContactRequestDto contactRequestDto) {
    log.debug("Updating contact with ID: {} with data: {}", id, contactRequestDto);

    final var contact = findById(id);

    log.debug("Found contact to update: {}", contact.getId());

    contactRepository.findByValue(contactRequestDto.value())
        .ifPresent(c -> {
          if (!c.getId().equals(id)) {
            log.warn("Attempted to update contact with a value that already exists for another contact: {}",
                contactRequestDto.value());
            throw new ValidationException("Contact value must be unique");
          }
        });

    final var code = UUID.randomUUID().toString().split("-")[1];
    final var wasContactValueChanged = !contact.getValue().equals(contactRequestDto.value());

    log.debug("Contact value changed: {}", wasContactValueChanged);

    if (wasContactValueChanged) {
      contact.setVerificationRequestedAt(LocalDateTime.now());
      contact.setVerificationCompletedAt(null);
      log.debug("Contact verification status reset due to value change for contact ID: {}", id);
      this.tokenService.deleteByUser(contact.getUser());
      this.tokenService.flush();
      this.tokenService.save(contact.getUser(), code, bCryptPasswordEncoder);
      log.debug("New token saved for updated contact value for user: {}", contact.getUser().getId());
    }

    contact.setUpdatedAt(LocalDateTime.now());
    contact.setValue(contactRequestDto.value());
    log.debug("Contact value updated for contact ID: {}", id);

    if (!contact.isStandard() && contactRequestDto.standard()) {
      log.debug("Setting contact ID {} as standard. Unsetting other standard contacts for user: {}", id,
          contact.getUser().getId());
      contact.getUser().getContacts().stream()
          .filter(Contact::isStandard)
          .forEach(c -> {
            log.debug("Unsetting standard status for contact ID: {}", c.getId());
            c.setStandard(false);
            contactRepository.save(c);
          });
    }

    contact.setStandard(contactRequestDto.standard());

    final var updatedContact = contactRepository.save(contact);

    log.info("Contact with ID: {} updated successfully.", updatedContact.getId());

    if (wasContactValueChanged) {
      this.contactConfirmationService.send(new EmailCodeDto(contactRequestDto.value(), code));
      log.debug("New contact confirmation email sent for updated value to: {}", contactRequestDto.value());
    }

    return contactMapper.contactToContactResponseDto(updatedContact);
  }

  @Transactional
  public MessageValueResponseDto contactConfirmation(ContactConfirmationRequestDto contactConfirmationRequestDto) {
    log.debug("Attempting contact confirmation for email: {}", contactConfirmationRequestDto.email());

    final var email = contactConfirmationRequestDto.email();
    final var optionalContact = contactRepository.findByValue(email);

    if (optionalContact.isEmpty()) {
      log.warn("No contact found with this value: {}", email);
      throw new ValidationException("No contact found with this value.");
    }

    final var contact = optionalContact.get();

    log.debug("Found contact for confirmation: {}", contact.getId());

    final var messageError = tokenService.validateToken(contact.getUser(), email, contactConfirmationRequestDto.code());

    if (messageError != null) {
      log.warn("Contact confirmation failed for contact ID {} with error: {}", contact.getId(), messageError);
      throw new ValidationException(messageError);
    }

    contact.setVerificationCompletedAt(LocalDateTime.now());
    this.tokenService.deleteByUser(contact.getUser());
    final var savedContact = contactRepository.save(contact);

    log.info("Contact with ID: {} confirmed successfully.", savedContact.getId());

    savedContact.getUser().getContacts().stream()
        .filter(Contact::isStandard)
        .filter(c -> c.getVerificationCompletedAt() != null)
        .filter(c -> !c.getValue().equals(contact.getValue()))
        .map(c -> {
          log.debug("Unsetting standard status for old standard contact ID: {}", c.getId());
          c.setStandard(false);
          return c;
        })
        .forEach(c -> contactRepository.save(c));

    log.debug("Updated standard status for other contacts after confirmation of contact ID: {}", savedContact.getId());
    return new MessageValueResponseDto("Contact was confirmed.");
  }

  public Contact findById(Long id) {
    log.debug("Attempting to find contact by ID: {}", id);

    final var optionalContact = contactRepository.findById(id);
    final var contact = optionalContact.orElseThrow(() -> {
      log.warn("Contact with ID: {} not found.", id);
      return new ResourceNotFoundException("Contact not found.");
    });

    log.debug("Found contact with ID: {}", contact.getId());
    return contact;
  }
}
