package br.com.conectabyte.profissu.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import br.com.conectabyte.profissu.dtos.request.ReviewRequestDto;
import br.com.conectabyte.profissu.entities.Review;
import br.com.conectabyte.profissu.enums.OfferStatusEnum;
import br.com.conectabyte.profissu.enums.RequestedServiceStatusEnum;
import br.com.conectabyte.profissu.exceptions.ResourceNotFoundException;
import br.com.conectabyte.profissu.exceptions.ValidationException;
import br.com.conectabyte.profissu.repositories.ReviewRepository;
import br.com.conectabyte.profissu.services.email.NotificationService;
import br.com.conectabyte.profissu.utils.ContactUtils;
import br.com.conectabyte.profissu.utils.ConversationUtils;
import br.com.conectabyte.profissu.utils.RequestedServiceUtils;
import br.com.conectabyte.profissu.utils.ReviewUtils;
import br.com.conectabyte.profissu.utils.UserUtils;

@ExtendWith(MockitoExtension.class)
class ReviewServiceTest {
  @Mock
  private ReviewRepository reviewRepository;

  @Mock
  private UserService userService;

  @Mock
  private RequestedServiceService requestedServiceService;

  @Mock
  private JwtService jwtService;

  @Mock
  NotificationService notificationService;

  @InjectMocks
  private ReviewService reviewService;

  @Test
  void shouldRegisterReviewSuccessfullyWhenRequestedServiceIsDone() {
    final var reviewRequestDto = new ReviewRequestDto("Title", "Review", 5);
    final var requester = UserUtils.create();
    final var serviceProvider = UserUtils.create();
    final var contact = ContactUtils.create(serviceProvider);
    final var conversation = ConversationUtils.create(requester, serviceProvider, null, List.of());
    final var rejectedConversation = ConversationUtils.create(requester, serviceProvider, null, List.of());
    final var requestedService = RequestedServiceUtils.create(requester, null,
        List.of(rejectedConversation, conversation));
    final var review = ReviewUtils.create(requester, requestedService);

    serviceProvider.setContacts(List.of(contact));
    conversation.setOfferStatus(OfferStatusEnum.ACCEPTED);
    rejectedConversation.setOfferStatus(OfferStatusEnum.REJECTED);
    conversation.setRequestedService(requestedService);

    requestedService.setStatus(RequestedServiceStatusEnum.DONE);

    doNothing().when(notificationService).send(any());
    when(jwtService.getClaims()).thenReturn(Optional.of(Map.of("sub", "1")));
    when(userService.findById(any())).thenReturn(requester);
    when(requestedServiceService.findById(any())).thenReturn(requestedService);
    when(reviewRepository.save(any())).thenReturn(review);

    var response = reviewService.register(1L, reviewRequestDto);

    assertThat(response).isNotNull();
    assertThat(response.title()).isEqualTo("Title Test");
    assertThat(response.review()).isEqualTo("Revew Test");
    assertThat(response.stars()).isEqualTo(1);
    verify(notificationService, times(1)).send(any());
  }

  @Test
  void shouldNotSendNotificationWhenReceiverHasNoValidContact() {
    final var reviewRequestDto = new ReviewRequestDto("Title", "Review", 5);
    final var requester = UserUtils.create();
    final var serviceProvider = UserUtils.create();
    final var contact = ContactUtils.create(serviceProvider);
    final var conversation = ConversationUtils.create(requester, serviceProvider, null, List.of());
    final var requestedService = RequestedServiceUtils.create(requester, null, List.of(conversation));
    final var review = ReviewUtils.create(requester, requestedService);

    contact.setVerificationCompletedAt(null);
    serviceProvider.setContacts(List.of(contact));
    conversation.setOfferStatus(OfferStatusEnum.ACCEPTED);
    conversation.setRequestedService(requestedService);

    requestedService.setStatus(RequestedServiceStatusEnum.DONE);
    when(jwtService.getClaims()).thenReturn(Optional.of(Map.of("sub", "1")));
    when(userService.findById(any())).thenReturn(requester);
    when(requestedServiceService.findById(any())).thenReturn(requestedService);
    when(reviewRepository.save(any())).thenReturn(review);

    reviewService.register(1L, reviewRequestDto);

    verify(notificationService, times(0)).send(any());
  }

  @Test
  void shouldThrowValidationExceptionWhenRequestedServiceIsNotDone() {
    final var reviewRequestDto = new ReviewRequestDto("Title", "Review", 5);
    final var requester = UserUtils.create();
    final var serviceProvider = UserUtils.create();
    final var conversation = ConversationUtils.create(requester, serviceProvider, null, List.of());
    final var requestedService = RequestedServiceUtils.create(requester, null, List.of(conversation));

    requestedService.setStatus(RequestedServiceStatusEnum.INPROGRESS);

    when(jwtService.getClaims()).thenReturn(Optional.of(Map.of("sub", "1")));
    when(userService.findById(any())).thenReturn(requester);
    when(requestedServiceService.findById(any())).thenReturn(requestedService);

    assertThatThrownBy(() -> reviewService.register(1L, reviewRequestDto))
        .isInstanceOf(ValidationException.class)
        .hasMessageContaining("Feedback can only be provided for services that have been completed.");
  }

  @Test
  void shouldThrowExceptionWhenTokenDoesNotHaveSubClaim() {
    final var reviewRequestDto = new ReviewRequestDto("Title", "Review", 5);

    when(jwtService.getClaims()).thenReturn(Optional.empty());

    assertThatThrownBy(() -> reviewService.register(1L, reviewRequestDto))
        .isInstanceOf(NoSuchElementException.class);
  }

  @Test
  void shouldThrowExceptionWhenUserNotFound() {
    final var reviewRequestDto = new ReviewRequestDto("Title", "Review", 5);

    when(jwtService.getClaims()).thenReturn(Optional.of(Map.of("sub", "1")));
    when(userService.findById(any())).thenThrow(new IllegalArgumentException("User not found"));

    assertThatThrownBy(() -> reviewService.register(1L, reviewRequestDto))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("User not found");
  }

  @Test
  void shouldThrowExceptionWhenRequestedServiceNotFound() {
    final var reviewRequestDto = new ReviewRequestDto("Title", "Review", 5);
    final var user = UserUtils.create();

    when(jwtService.getClaims()).thenReturn(Optional.of(Map.of("sub", "1")));
    when(userService.findById(any())).thenReturn(user);
    when(requestedServiceService.findById(any()))
        .thenThrow(new IllegalArgumentException("RequestedService not found"));

    assertThatThrownBy(() -> reviewService.register(1L, reviewRequestDto))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("RequestedService not found");
  }

  @Test
  void shouldPropagateExceptionWhenReviewRepositoryFails() {
    final var reviewRequestDto = new ReviewRequestDto("Title", "Review", 5);
    final var requester = UserUtils.create();
    final var serviceProvider = UserUtils.create();
    final var contact = ContactUtils.create(serviceProvider);
    final var conversation = ConversationUtils.create(requester, serviceProvider, null, List.of());
    final var requestedService = RequestedServiceUtils.create(requester, null, List.of(conversation));

    serviceProvider.setContacts(List.of(contact));
    conversation.setOfferStatus(OfferStatusEnum.ACCEPTED);
    conversation.setRequestedService(requestedService);
    requestedService.setStatus(RequestedServiceStatusEnum.DONE);

    when(jwtService.getClaims()).thenReturn(Optional.of(Map.of("sub", "1")));
    when(userService.findById(any())).thenReturn(requester);
    when(requestedServiceService.findById(any())).thenReturn(requestedService);
    when(reviewRepository.save(any(Review.class)))
        .thenThrow(new RuntimeException("Database error"));

    assertThatThrownBy(() -> reviewService.register(1L, reviewRequestDto))
        .isInstanceOf(RuntimeException.class)
        .hasMessageContaining("Database error");
  }

  @Test
  void shouldReturnReviewsGivenByUserWhenIsReviewOwnerTrue() {
    final var pageable = PageRequest.of(0, 10);
    final var review = new Review();

    review.setId(100L);

    final var reviewPage = new PageImpl<>(List.of(review), pageable, 1);

    when(reviewRepository.findReviewsGivenByUserId(any(), any())).thenReturn(reviewPage);

    final var result = reviewService.findByUserId(1L, true, pageable);

    assertThat(result).isNotNull();
    assertThat(result.getContent()).hasSize(1);
    assertThat(result.getContent().get(0).id()).isEqualTo(100L);
  }

  @Test
  void shouldReturnReviewsReceivedByUserWhenIsReviewOwnerFalse() {
    final var pageable = PageRequest.of(0, 10);
    final var review = new Review();

    review.setId(200L);

    final var reviewPage = new PageImpl<>(List.of(review), pageable, 1);

    when(reviewRepository.findReviewsReceivedByUserId(any(), any())).thenReturn(reviewPage);

    final var result = reviewService.findByUserId(1L, false, pageable);

    assertThat(result).isNotNull();
    assertThat(result.getContent()).hasSize(1);
    assertThat(result.getContent().get(0).id()).isEqualTo(200L);
  }

  @Test
  void shouldReturnEmptyPageWhenNoReviewsFound() {
    final var pageable = PageRequest.of(0, 10);
    final Page<Review> emptyPage = Page.empty(pageable);

    when(reviewRepository.findReviewsGivenByUserId(any(), any())).thenReturn(emptyPage);

    final var result = reviewService.findByUserId(1L, true, pageable);

    assertThat(result).isNotNull();
    assertThat(result.getContent()).isEmpty();
  }

  @Test
  void shouldReturnReviewWhenFoundById() {
    final var review = new Review();

    review.setId(1L);

    when(reviewRepository.findById(1L)).thenReturn(Optional.of(review));

    final var result = reviewService.findById(1L);

    assertThat(result).isNotNull();
    assertThat(result.getId()).isEqualTo(1L);
  }

  @Test
  void shouldThrowResourceNotFoundExceptionWhenReviewNotFound() {
    when(reviewRepository.findById(1L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> reviewService.findById(1L))
        .isInstanceOf(ResourceNotFoundException.class)
        .hasMessage("User not found.");
  }

  @Test
  void shouldSetDeletedAtAndSaveWhenReviewExists() {
    final var review = new Review();

    review.setId(1L);

    when(reviewRepository.findById(1L)).thenReturn(Optional.of(review));
    when(reviewRepository.save(any(Review.class))).thenAnswer(invocation -> invocation.getArgument(0));

    reviewService.deleteById(1L);

    assertThat(review.getDeletedAt()).isNotNull();
    verify(reviewRepository).save(review);
  }

  @Test
  void shouldDoNothingWhenReviewDoesNotExist() {
    when(reviewRepository.findById(1L)).thenReturn(Optional.empty());

    reviewService.deleteById(1L);

    verify(reviewRepository, never()).save(any());
  }

  @Test
  void shouldUpdateReviewSuccessfully() {
    final var updatedReviewDto = new ReviewRequestDto("New Title", "New Review", 5);
    final var requester = UserUtils.create();
    final var serviceProvider = UserUtils.create();
    final var contact = ContactUtils.create(serviceProvider);
    final var notVerifiedContact = ContactUtils.create(serviceProvider);
    final var conversation = ConversationUtils.create(requester, serviceProvider, null, List.of());
    final var requestedService = RequestedServiceUtils.create(requester, null, List.of(conversation));
    final var review = ReviewUtils.create(serviceProvider, requestedService);

    requester.setId(1L);
    serviceProvider.setId(2L);
    contact.setVerificationCompletedAt(null);
    requester.setContacts(List.of(notVerifiedContact, contact));
    conversation.setOfferStatus(OfferStatusEnum.ACCEPTED);
    conversation.setRequestedService(requestedService);

    when(reviewRepository.findById(any())).thenReturn(Optional.of(review));
    when(reviewRepository.save(any(Review.class))).thenAnswer(invocation -> invocation.getArgument(0));

    final var response = reviewService.updateById(1L, updatedReviewDto);

    assertThat(response).isNotNull();
    assertThat(response.title()).isEqualTo(updatedReviewDto.title());
    assertThat(response.review()).isEqualTo(updatedReviewDto.review());
    assertThat(response.stars()).isEqualTo(updatedReviewDto.stars());
    assertThat(review.getUpdatedAt()).isNotNull();
    verify(reviewRepository).save(review);
  }
}
