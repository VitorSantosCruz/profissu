package br.com.conectabyte.profissu.services;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.http.HttpStatus;
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
    final var userId = this.jwtService.getClaims()
        .map(claims -> Long.valueOf(claims.get("sub").toString()))
        .orElseThrow();
    final var contactToBeSaved = contactMapper.contactRequestDtoToContact(contactRequestDto);
    final var user = this.userService.findById(userId);

    contactToBeSaved.setVerificationRequestedAt(LocalDateTime.now());
    contactToBeSaved.setUser(user);

    final var code = UUID.randomUUID().toString().split("-")[1];

    this.tokenService.deleteByUser(user);
    this.tokenService.flush();
    this.tokenService.save(user, code, bCryptPasswordEncoder);
    this.contactConfirmationService.send(new EmailCodeDto(contactRequestDto.value(), code));

    final var savedContact = contactRepository.save(contactToBeSaved);

    return contactMapper.contactToContactResponseDto(savedContact);
  }

  @Transactional
  public ContactResponseDto update(Long id, ContactRequestDto contactRequestDto) {
    final var contact = findById(id);

    contactRepository.findByValue(contactRequestDto.value())
        .ifPresent(c -> {
          if (!c.getId().equals(id)) {
            throw new ValidationException("Contact value must be unique");
          }
        });

    if (!contact.getValue().equals(contactRequestDto.value())) {
      final var code = UUID.randomUUID().toString().split("-")[1];

      contact.setVerificationRequestedAt(LocalDateTime.now());
      contact.setVerificationCompletedAt(null);

      this.tokenService.deleteByUser(contact.getUser());
      this.tokenService.flush();
      this.tokenService.save(contact.getUser(), code, bCryptPasswordEncoder);
      this.contactConfirmationService.send(new EmailCodeDto(contactRequestDto.value(), code));
    }

    contact.setUpdatedAt(LocalDateTime.now());
    contact.setValue(contactRequestDto.value());

    if (!contact.isStandard() && contactRequestDto.standard()) {
      contact.getUser().getContacts().stream()
          .filter(Contact::isStandard)
          .forEach(c -> {
            c.setStandard(false);
            contactRepository.save(c);
          });
    }

    contact.setStandard(contactRequestDto.standard());

    final var updatedContact = contactRepository.save(contact);

    return contactMapper.contactToContactResponseDto(updatedContact);
  }

  @Transactional
  public MessageValueResponseDto contactConfirmation(ContactConfirmationRequestDto contactConfirmationRequestDto) {
    final var email = contactConfirmationRequestDto.email();
    final var optionalContact = contactRepository.findByValue(email);

    if (optionalContact.isEmpty()) {
      log.warn("No contact found with this value: {}", email);
      return new MessageValueResponseDto(HttpStatus.BAD_REQUEST.value(), "No contact found with this value.");
    }

    final var contact = optionalContact.get();
    final var messageError = tokenService.validateToken(contact.getUser(), email, contactConfirmationRequestDto.code());

    if (messageError != null) {
      return new MessageValueResponseDto(HttpStatus.BAD_REQUEST.value(), messageError);
    }

    contact.setVerificationCompletedAt(LocalDateTime.now());
    this.tokenService.deleteByUser(contact.getUser());
    contactRepository.save(contact);

    return new MessageValueResponseDto(HttpStatus.OK.value(), "Contact was confirmed.");
  }

  public Contact findById(Long id) {
    final var optionalContact = contactRepository.findById(id);
    final var contact = optionalContact.orElseThrow(() -> new ResourceNotFoundException("Contact not found."));

    return contact;
  }
}
