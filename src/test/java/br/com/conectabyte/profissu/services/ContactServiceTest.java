package br.com.conectabyte.profissu.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import br.com.conectabyte.profissu.dtos.request.ContactConfirmationRequestDto;
import br.com.conectabyte.profissu.dtos.request.ContactRequestDto;
import br.com.conectabyte.profissu.entities.Contact;
import br.com.conectabyte.profissu.entities.User;
import br.com.conectabyte.profissu.enums.ContactTypeEnum;
import br.com.conectabyte.profissu.exceptions.ResourceNotFoundException;
import br.com.conectabyte.profissu.exceptions.ValidationException;
import br.com.conectabyte.profissu.mappers.ContactMapper;
import br.com.conectabyte.profissu.repositories.ContactRepository;
import br.com.conectabyte.profissu.utils.ContactUtils;
import br.com.conectabyte.profissu.utils.UserUtils;

@ExtendWith(MockitoExtension.class)
public class ContactServiceTest {
  @Mock
  private ContactRepository contactRepository;

  @Mock
  private UserService userService;

  @Mock
  private TokenService tokenService;

  @Mock
  private EmailService emailService;

  @Mock
  private BCryptPasswordEncoder bCryptPasswordEncoder;

  @InjectMocks
  private ContactService contactService;

  private ContactMapper contactMapper = ContactMapper.INSTANCE;
  private final User user = UserUtils.create();
  private final Contact contact = ContactUtils.createEmail(user);
  private final ContactRequestDto validRequest = contactMapper.contactToContactRequestDto(contact);

  @Test
  void shouldReturnOkRequestWhenSignUpWasConfirmed() {
    final var contact = ContactUtils.createEmail(user);

    when(contactRepository.findByValue(any())).thenReturn(Optional.of(contact));
    when(contactRepository.save(any())).thenReturn(contact);
    when(tokenService.validateToken(any(), any(), any())).thenReturn(null);

    final var requestDto = new ContactConfirmationRequestDto("test@conectabyte.com.br", "CODE");
    final var response = contactService.contactConfirmation(requestDto);

    assertEquals(HttpStatus.OK.value(), response.responseCode());
    assertEquals("Contact was confirmed.", response.message());
  }

  @Test
  void shouldReturnBadRequestWhenNoUserFoundForInformedEmail() {
    when(contactRepository.findByValue(any())).thenReturn(Optional.empty());

    final var requestDto = new ContactConfirmationRequestDto("test@conectabyte.com.br", "CODE");
    final var response = contactService.contactConfirmation(requestDto);

    assertEquals(HttpStatus.BAD_REQUEST.value(), response.responseCode());
    assertEquals("No contact found with this value.", response.message());
  }

  @Test
  void shouldReturnBadRequestWhenMissingCodeForUserWithThisEmail() {
    final var contact = ContactUtils.createEmail(user);

    when(contactRepository.findByValue(any())).thenReturn(Optional.of(contact));
    when(tokenService.validateToken(any(), any(), any())).thenReturn("Missing reset code for user with this e-mail.");

    final var requestDto = new ContactConfirmationRequestDto("test@conectabyte.com.br", "CODE");
    final var response = contactService.contactConfirmation(requestDto);

    assertEquals(HttpStatus.BAD_REQUEST.value(), response.responseCode());
    assertEquals("Missing reset code for user with this e-mail.", response.message());
  }

  @Test
  void shouldRegisterContactEmailSuccessfully() {
    when(userService.findById(1L)).thenReturn(user);
    when(contactRepository.save(any(Contact.class))).thenReturn(contact);

    final var savedContact = contactService.register(1L, validRequest);

    assertEquals(savedContact.id(), contact.getId());
    assertEquals(savedContact.type(), contact.getType());
    assertEquals(savedContact.value(), contact.getValue());
    assertEquals(savedContact.standard(), contact.isStandard());
  }

  @Test
  void shouldRegisterContactPhoneSuccessfully() {
    final var phone = ContactUtils.createPhone(user);

    when(userService.findById(1L)).thenReturn(user);
    when(contactRepository.save(any(Contact.class))).thenReturn(phone);

    final var registerRequest = new ContactRequestDto(
        phone.getType(),
        phone.getValue(),
        false);

    final var savedContact = contactService.register(1L, registerRequest);

    assertEquals(savedContact.id(), phone.getId());
    assertEquals(savedContact.type(), phone.getType());
    assertEquals(savedContact.value(), phone.getValue());
    assertEquals(savedContact.standard(), phone.isStandard());
  }

  @Test
  void shouldThrowResourceNotFoundExceptionWhenUserNotFound() {
    when(userService.findById(1L)).thenThrow(new ResourceNotFoundException("User not found."));

    assertThrows(ResourceNotFoundException.class, () -> contactService.register(1L, validRequest));
  }

  @Test
  void shouldUpdateContactSuccessfully() {
    when(contactRepository.findById(1L)).thenReturn(Optional.of(contact));
    when(contactRepository.save(any(Contact.class))).thenReturn(contact);

    final var updatedRequest = new ContactRequestDto(
        ContactTypeEnum.EMAIL,
        "updated@example.com",
        false);

    final var updatedContact = contactService.update(1L, updatedRequest);

    assertEquals("updated@example.com", updatedContact.value());
    assertEquals(ContactTypeEnum.EMAIL, updatedContact.type());
    assertEquals(false, updatedContact.standard());
  }

  @Test
  void shouldThrowResourceNotFoundExceptionWhenContactNotFound() {
    when(contactRepository.findById(1L)).thenReturn(Optional.empty());

    final var updatedRequest = new ContactRequestDto(
        ContactTypeEnum.EMAIL,
        "updated@example.com",
        false);

    assertThrows(ResourceNotFoundException.class, () -> contactService.update(1L, updatedRequest));
  }

  @Test
  void shouldRegisterContactWithEmailAndSendConfirmation() {
    when(userService.findById(any())).thenReturn(user);
    when(contactRepository.save(any())).thenReturn(contact);

    final var savedContact = contactService.register(1L, validRequest);

    assertEquals(savedContact.value(), "test@conectabyte.com.br");
    assertEquals(savedContact.type(), ContactTypeEnum.EMAIL);

    verify(emailService).sendContactConfirmation(any(), any());
  }

  @Test
  void shouldThrowValidationExceptionWhenContactValueIsNotUnique() {
    when(contactRepository.findById(any())).thenReturn(Optional.of(contact));
    when(contactRepository.findByValue(any())).thenReturn(Optional.of(contact));

    assertThrows(ValidationException.class, () -> contactService.update(contact.getId() + 1, validRequest));
  }

  @Test
  void shouldUpdateContactAndSendVerificationEmailWhenValueIsChangedToEmail() {
    when(contactRepository.findById(any())).thenReturn(Optional.of(contact));
    when(contactRepository.findByValue(any())).thenReturn(Optional.of(contact));
    when(contactRepository.save(any())).thenReturn(contact);

    final var updatedRequest = new ContactRequestDto(
        ContactTypeEnum.EMAIL,
        "new@conectabyte.com.br",
        true);
    final var updatedContact = contactService.update(contact.getId(), updatedRequest);

    assertEquals("new@conectabyte.com.br", updatedContact.value());
    assertEquals(ContactTypeEnum.EMAIL, updatedContact.type());
    assertEquals(true, updatedContact.standard());

    verify(tokenService).save(any(), any(), any());
    verify(emailService).sendContactConfirmation(any(), any());
  }

  @Test
  void shouldUpdateContactWhenStandardIsNewValue() {
    final var phone = ContactUtils.createPhone(user);
    when(contactRepository.findById(any())).thenReturn(Optional.of(phone));
    when(contactRepository.save(any())).thenReturn(phone);

    final var updatedRequest = new ContactRequestDto(
        phone.getType(),
        phone.getValue(),
        false);

    final var updatedContact = contactService.update(1L, updatedRequest);

    assertEquals(false, updatedContact.standard());

    verify(tokenService, times(0)).save(any(), any(), any());
    verify(emailService, times(0)).sendContactConfirmation(any(), any());
  }

  @Test
  void shouldUpdateContactWhenValueIsNewValue() {
    final var phone = ContactUtils.createPhone(user);
    when(contactRepository.findById(any())).thenReturn(Optional.of(phone));
    when(contactRepository.save(any())).thenReturn(phone);

    final var updatedRequest = new ContactRequestDto(
        phone.getType(),
        "00000000000",
        phone.isStandard());

    final var updatedContact = contactService.update(1L, updatedRequest);

    assertEquals("00000000000", updatedContact.value());

    verify(tokenService, times(0)).save(any(), any(), any());
    verify(emailService, times(0)).sendContactConfirmation(any(), any());
  }
}
