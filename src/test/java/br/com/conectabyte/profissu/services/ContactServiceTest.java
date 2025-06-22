package br.com.conectabyte.profissu.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import br.com.conectabyte.profissu.dtos.request.ContactConfirmationRequestDto;
import br.com.conectabyte.profissu.dtos.request.ContactRequestDto;
import br.com.conectabyte.profissu.entities.Contact;
import br.com.conectabyte.profissu.entities.User;
import br.com.conectabyte.profissu.exceptions.ResourceNotFoundException;
import br.com.conectabyte.profissu.exceptions.ValidationException;
import br.com.conectabyte.profissu.mappers.ContactMapper;
import br.com.conectabyte.profissu.repositories.ContactRepository;
import br.com.conectabyte.profissu.services.email.ContactConfirmationService;
import br.com.conectabyte.profissu.utils.ContactUtils;
import br.com.conectabyte.profissu.utils.UserUtils;

@ExtendWith(MockitoExtension.class)
@DisplayName("ContactService Tests")
class ContactServiceTest {
  @Mock
  private ContactRepository contactRepository;

  @Mock
  private UserService userService;

  @Mock
  private TokenService tokenService;

  @Mock
  private ContactConfirmationService contactConfirmationService;

  @Mock
  private BCryptPasswordEncoder bCryptPasswordEncoder;

  @Mock
  private JwtService jwtService;

  @InjectMocks
  private ContactService contactService;

  private ContactMapper contactMapper = ContactMapper.INSTANCE;

  @Test
  @DisplayName("Should register contact successfully and send confirmation email")
  void shouldRegisterContactSuccessfullyAndSendConfirmationEmail() {
    final var user = UserUtils.create();
    final var contact = ContactUtils.create(user);
    final var validRequest = contactMapper.contactToContactRequestDto(contact);

    when(jwtService.getClaims()).thenReturn(Optional.of(new HashMap<>(Map.of("sub", "1"))));
    when(userService.findById(any())).thenReturn(user);
    when(contactRepository.save(any(Contact.class))).thenReturn(contact);
    doNothing().when(tokenService).flush();
    doNothing().when(contactConfirmationService).send(any());

    final var savedContact = contactService.register(validRequest);

    assertNotNull(savedContact);
    assertEquals(contact.getId(), savedContact.id());
    assertEquals(contact.getValue(), savedContact.value());
    assertEquals(contact.isStandard(), savedContact.standard());
    verify(userService).findById(anyLong());
    verify(contactRepository).save(any(Contact.class));
    verify(tokenService).deleteByUser(user);
    verify(tokenService).flush();
    verify(tokenService).save(any(User.class), anyString(), any(BCryptPasswordEncoder.class));
    verify(contactConfirmationService).send(any());
  }

  @Test
  @DisplayName("Should throw NoSuchElementException when JWT claims are missing on register")
  void shouldThrowNoSuchElementExceptionWhenJwtClaimsAreMissingOnRegister() {
    final var user = UserUtils.create();
    final var validRequest = contactMapper.contactToContactRequestDto(ContactUtils.create(user));

    when(jwtService.getClaims()).thenReturn(Optional.empty());

    assertThrows(NoSuchElementException.class, () -> contactService.register(validRequest));
  }

  @Test
  @DisplayName("Should throw ResourceNotFoundException when user not found on register")
  void shouldThrowResourceNotFoundExceptionWhenUserNotFoundOnRegister() {
    final var user = UserUtils.create();
    final var validRequest = contactMapper.contactToContactRequestDto(ContactUtils.create(user));

    when(jwtService.getClaims()).thenReturn(Optional.of(new HashMap<>(Map.of("sub", "1"))));
    when(userService.findById(any())).thenThrow(new ResourceNotFoundException("User not found."));

    assertThrows(ResourceNotFoundException.class, () -> contactService.register(validRequest));
  }

  @Test
  @DisplayName("Should update contact successfully when value does not change")
  void shouldUpdateContactSuccessfullyWhenValueNotChange() {
    final var user = UserUtils.create();
    final var contact = ContactUtils.create(user);

    contact.setId(1L);
    contact.setVerificationRequestedAt(LocalDateTime.now().minusDays(1));

    when(contactRepository.findById(anyLong())).thenReturn(Optional.of(contact));
    when(contactRepository.findByValue(anyString())).thenReturn(Optional.of(contact));
    when(contactRepository.save(any(Contact.class))).thenReturn(contact);

    final var updatedRequest = new ContactRequestDto("test@conectabyte.com.br", false);
    final var updatedContact = contactService.update(1L, updatedRequest);

    assertNotNull(updatedContact);
    assertEquals("test@conectabyte.com.br", updatedContact.value());
    assertEquals(false, updatedContact.standard());
    verify(contactRepository, times(1)).findById(anyLong());
    verify(contactRepository, times(1)).findByValue(anyString());
    verify(contactRepository, times(1)).save(any(Contact.class));
    verify(tokenService, never()).deleteByUser(any());
    verify(tokenService, never()).flush();
    verify(tokenService, never()).save(any(), any(), any());
    verify(contactConfirmationService, never()).send(any());
  }

  @Test
  @DisplayName("Should update contact successfully and send new verification email when value changes")
  void shouldUpdateContactSuccessfullyWhenValueChanges() {
    final var user = UserUtils.create();
    final var contact = ContactUtils.create(user);
    contact.setVerificationCompletedAt(LocalDateTime.now());

    when(contactRepository.findById(anyLong())).thenReturn(Optional.of(contact));
    when(contactRepository.findByValue(anyString())).thenReturn(Optional.empty());
    when(contactRepository.save(any(Contact.class))).thenReturn(contact);
    doNothing().when(tokenService).deleteByUser(any());
    doNothing().when(tokenService).flush();
    doNothing().when(tokenService).save(any(), any(), any());
    doNothing().when(contactConfirmationService).send(any());

    final var updatedRequest = new ContactRequestDto("new@conectabyte.com.br", true);
    final var updatedContact = contactService.update(1L, updatedRequest);

    assertNotNull(updatedContact);
    assertEquals("new@conectabyte.com.br", updatedContact.value());
    assertEquals(true, updatedContact.standard());
    verify(contactRepository, times(1)).findById(anyLong());
    verify(contactRepository, times(1)).findByValue(anyString());
    verify(contactRepository, times(1)).save(any(Contact.class));
    verify(tokenService, times(1)).deleteByUser(any(User.class));
    verify(tokenService, times(1)).flush();
    verify(tokenService, times(1)).save(any(User.class), anyString(), any(BCryptPasswordEncoder.class));
    verify(contactConfirmationService, times(1)).send(any());
  }

  @Test
  @DisplayName("Should throw ValidationException when new contact value already exists for another contact")
  void shouldThrowValidationExceptionWhenContactValueIsNotUniqueForAnotherContact() {
    final var user = UserUtils.create();
    final var existingContact = ContactUtils.create(user);
    existingContact.setId(2L);
    final var contactToUpdate = ContactUtils.create(user);
    contactToUpdate.setId(1L);

    when(contactRepository.findById(anyLong())).thenReturn(Optional.of(contactToUpdate));
    when(contactRepository.findByValue(anyString())).thenReturn(Optional.of(existingContact));

    final var updatedRequest = new ContactRequestDto("test@conectabyte.com.br", false);

    assertThrows(ValidationException.class, () -> contactService.update(1L, updatedRequest));
    verify(contactRepository, never()).save(any(Contact.class));
  }

  @Test
  @DisplayName("Should update contact to standard and unset other standard contact")
  void shouldUpdateContactToStandardAndUnsetOtherStandardContact() {
    final var user = UserUtils.create();
    final var currentStandardContact = ContactUtils.create(user);
    currentStandardContact.setId(10L);
    currentStandardContact.setStandard(true);
    currentStandardContact.setVerificationCompletedAt(LocalDateTime.now());

    final var contactToMakeStandard = ContactUtils.create(user);
    contactToMakeStandard.setId(1L);
    contactToMakeStandard.setStandard(false);
    contactToMakeStandard.setValue("new_standard@example.com");
    contactToMakeStandard.setVerificationCompletedAt(LocalDateTime.now());

    user.setContacts(List.of(currentStandardContact, contactToMakeStandard));

    when(contactRepository.findById(contactToMakeStandard.getId())).thenReturn(Optional.of(contactToMakeStandard));
    when(contactRepository.findByValue(anyString())).thenReturn(Optional.empty());
    when(contactRepository.save(contactToMakeStandard)).thenReturn(contactToMakeStandard);
    when(contactRepository.save(currentStandardContact)).thenReturn(currentStandardContact);

    final var updatedRequest = new ContactRequestDto("new_standard@example.com", true);
    final var updatedContact = contactService.update(contactToMakeStandard.getId(), updatedRequest);

    assertNotNull(updatedContact);
    assertTrue(updatedContact.standard());
    assertFalse(currentStandardContact.isStandard());
    verify(contactRepository, times(2)).save(any(Contact.class));
  }

  @Test
  @DisplayName("Should throw ResourceNotFoundException when contact not found on update")
  void shouldThrowResourceNotFoundExceptionWhenContactNotFoundOnUpdate() {
    when(contactRepository.findById(anyLong())).thenReturn(Optional.empty());

    final var updatedRequest = new ContactRequestDto("updated@example.com", false);

    assertThrows(ResourceNotFoundException.class, () -> contactService.update(1L, updatedRequest));
  }

  @Test
  @DisplayName("Should confirm contact successfully")
  void shouldConfirmContactSuccessfully() {
    final var user = UserUtils.create();
    final var contactToConfirm = ContactUtils.create(user);
    contactToConfirm.setVerificationCompletedAt(null);
    user.setContacts(List.of(contactToConfirm));

    when(contactRepository.findByValue(anyString())).thenReturn(Optional.of(contactToConfirm));
    when(tokenService.validateToken(any(User.class), anyString(), anyString())).thenReturn(null);
    when(contactRepository.save(any(Contact.class))).thenReturn(contactToConfirm);
    doNothing().when(tokenService).deleteByUser(any(User.class));

    final var requestDto = new ContactConfirmationRequestDto("test@conectabyte.com.br", "CODE");
    final var response = contactService.contactConfirmation(requestDto);

    assertNotNull(response);
    assertEquals("Contact was confirmed.", response.message());
    assertNotNull(contactToConfirm.getVerificationCompletedAt());
    verify(contactRepository).findByValue(anyString());
    verify(tokenService).validateToken(any(), any(), any());
    verify(contactRepository).save(contactToConfirm);
    verify(tokenService).deleteByUser(user);
  }

  @Test
  @DisplayName("Should unset other standard contact when a new one is confirmed and becomes standard")
  void shouldUnsetOtherStandardContactWhenNewOneIsConfirmed() {
    final var user = UserUtils.create();
    final var contactToConfirm = ContactUtils.create(user);
    contactToConfirm.setVerificationCompletedAt(null);
    contactToConfirm.setStandard(true);

    final var oldStandardContact = ContactUtils.create(user);
    oldStandardContact.setId(2L);
    oldStandardContact.setValue("old_standard@conectabyte.com.br");
    oldStandardContact.setStandard(true);
    oldStandardContact.setVerificationCompletedAt(LocalDateTime.now().minusDays(1));

    user.setContacts(List.of(contactToConfirm, oldStandardContact));

    when(contactRepository.findByValue(anyString())).thenReturn(Optional.of(contactToConfirm));
    when(tokenService.validateToken(any(User.class), anyString(), anyString())).thenReturn(null);
    when(contactRepository.save(contactToConfirm)).thenReturn(contactToConfirm);
    when(contactRepository.save(oldStandardContact)).thenReturn(oldStandardContact);
    doNothing().when(tokenService).deleteByUser(any(User.class));

    final var requestDto = new ContactConfirmationRequestDto("test@conectabyte.com.br", "CODE");
    contactService.contactConfirmation(requestDto);

    assertFalse(oldStandardContact.isStandard());
    assertTrue(contactToConfirm.isStandard());
    verify(contactRepository, times(2)).save(any(Contact.class));
  }

  @Test
  @DisplayName("Should unset other standard contact if the confirmed one is not standard")
  void shouldUnsetOtherStandardContactIfConfirmedIsNotStandard() {
    final var user = UserUtils.create();
    final var contactToConfirm = ContactUtils.create(user);
    contactToConfirm.setVerificationCompletedAt(null);
    contactToConfirm.setStandard(false);

    final var existingStandardContact = ContactUtils.create(user);
    existingStandardContact.setId(2L);
    existingStandardContact.setValue("existing_standard@conectabyte.com.br");
    existingStandardContact.setStandard(true);
    existingStandardContact.setVerificationCompletedAt(LocalDateTime.now().minusDays(1));

    user.setContacts(List.of(contactToConfirm, existingStandardContact));

    when(contactRepository.findByValue(anyString())).thenReturn(Optional.of(contactToConfirm));
    when(tokenService.validateToken(any(User.class), anyString(), anyString())).thenReturn(null);
    when(contactRepository.save(contactToConfirm)).thenReturn(contactToConfirm);
    when(contactRepository.save(existingStandardContact)).thenReturn(existingStandardContact);
    doNothing().when(tokenService).deleteByUser(any(User.class));

    contactService.contactConfirmation(new ContactConfirmationRequestDto("test@conectabyte.com.br", "CODE"));

    assertFalse(existingStandardContact.isStandard());
    assertFalse(contactToConfirm.isStandard());
    verify(contactRepository, times(2)).save(any(Contact.class));
  }

  @Test
  @DisplayName("Should throw ValidationException when no contact found for informed email on confirmation")
  void shouldThrowValidationExceptionWhenNoContactFoundForInformedEmailOnConfirmation() {
    when(contactRepository.findByValue(anyString())).thenReturn(Optional.empty());

    final var requestDto = new ContactConfirmationRequestDto("test@conectabyte.com.br", "CODE");
    final var exception = assertThrows(ValidationException.class, () -> contactService.contactConfirmation(requestDto));

    assertEquals("No contact found with this value.", exception.getMessage());
  }

  @Test
  @DisplayName("Should throw ValidationException when token validation fails on confirmation")
  void shouldThrowValidationExceptionWhenTokenValidationFailsOnConfirmation() {
    final var user = UserUtils.create();
    final var contact = ContactUtils.create(user);
    user.setContacts(List.of(contact));

    when(contactRepository.findByValue(anyString())).thenReturn(Optional.of(contact));
    when(tokenService.validateToken(any(User.class), anyString(), anyString())).thenReturn("Invalid code.");

    final var requestDto = new ContactConfirmationRequestDto("test@conectabyte.com.br", "WRONGCODE");
    final var exception = assertThrows(ValidationException.class, () -> contactService.contactConfirmation(requestDto));

    assertEquals("Invalid code.", exception.getMessage());
    verify(contactRepository, never()).save(any(Contact.class));
    verify(tokenService, never()).deleteByUser(any(User.class));
  }

  @Test
  @DisplayName("Should find contact by ID successfully")
  void shouldFindContactByIdSuccessfully() {
    final var user = UserUtils.create();
    final var contact = ContactUtils.create(user);

    when(contactRepository.findById(anyLong())).thenReturn(Optional.of(contact));

    final var foundContact = contactService.findById(1L);

    assertNotNull(foundContact);
    assertEquals(contact.getId(), foundContact.getId());
    assertEquals(contact.getValue(), foundContact.getValue());
    verify(contactRepository).findById(anyLong());
  }

  @Test
  @DisplayName("Should throw ResourceNotFoundException when contact not found by ID")
  void shouldThrowResourceNotFoundExceptionWhenContactNotFoundById() {
    when(contactRepository.findById(anyLong())).thenReturn(Optional.empty());

    assertThrows(ResourceNotFoundException.class, () -> contactService.findById(1L));
  }
}
