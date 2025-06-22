package br.com.conectabyte.profissu.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import br.com.conectabyte.profissu.dtos.request.ReviewRequestDto;
import br.com.conectabyte.profissu.dtos.response.ReviewResponseDto;
import br.com.conectabyte.profissu.entities.Contact;
import br.com.conectabyte.profissu.entities.Conversation;
import br.com.conectabyte.profissu.entities.RequestedService;
import br.com.conectabyte.profissu.entities.Review;
import br.com.conectabyte.profissu.entities.User;
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
@DisplayName("ReviewService Tests")
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

  private static final Long TEST_REQUESTED_SERVICE_ID = 1L;
  private static final Long TEST_USER_ID_REQUESTER = 10L;
  private static final Long TEST_USER_ID_SERVICE_PROVIDER = 20L;
  private static final String JWT_SUB_CLAIM = String.valueOf(TEST_USER_ID_REQUESTER);
  private static final String REVIEW_TITLE = "Great Service!";
  private static final String REVIEW_MESSAGE = "The service provided was excellent.";
  private static final int REVIEW_STARS = 5;

  @Test
  @DisplayName("Should register review successfully when requested service is DONE")
  void shouldRegisterReviewSuccessfullyWhenRequestedServiceIsDone() {
    ReviewRequestDto reviewRequestDto = new ReviewRequestDto(REVIEW_TITLE, REVIEW_MESSAGE, REVIEW_STARS);
    User requester = UserUtils.create();
    requester.setId(TEST_USER_ID_REQUESTER);
    User serviceProvider = UserUtils.create();
    serviceProvider.setId(TEST_USER_ID_SERVICE_PROVIDER);
    Contact contact = ContactUtils.create(serviceProvider);
    contact.setStandard(true);
    contact.setVerificationCompletedAt(LocalDateTime.now());

    Conversation conversation = ConversationUtils.create(requester, serviceProvider, null, List.of());
    conversation.setOfferStatus(OfferStatusEnum.ACCEPTED);

    RequestedService requestedService = RequestedServiceUtils.create(requester, null, List.of(conversation));
    requestedService.setStatus(RequestedServiceStatusEnum.DONE);
    conversation.setRequestedService(requestedService);

    Review review = ReviewUtils.create(requester, requestedService);

    serviceProvider.setContacts(List.of(contact));

    doNothing().when(notificationService).send(any());
    when(jwtService.getClaims()).thenReturn(Optional.of(Map.of("sub", JWT_SUB_CLAIM)));
    when(userService.findById(TEST_USER_ID_REQUESTER)).thenReturn(requester);
    when(requestedServiceService.findById(TEST_REQUESTED_SERVICE_ID)).thenReturn(requestedService);
    when(reviewRepository.save(any(Review.class))).thenReturn(review);

    ReviewResponseDto response = reviewService.register(TEST_REQUESTED_SERVICE_ID, reviewRequestDto);

    assertThat(response).isNotNull();
    assertThat(response.title()).isEqualTo(review.getTitle());
    assertThat(response.review()).isEqualTo(review.getReview());
    assertThat(response.stars()).isEqualTo(review.getStars());

    verify(jwtService, times(1)).getClaims();
    verify(userService, times(1)).findById(TEST_USER_ID_REQUESTER);
    verify(requestedServiceService, times(1)).findById(TEST_REQUESTED_SERVICE_ID);
    verify(reviewRepository, times(1)).save(any(Review.class));
    verify(notificationService, times(1)).send(any());
  }

  @Test
  @DisplayName("Should not send notification when receiver has no valid contact")
  void shouldNotSendNotificationWhenReceiverHasNoValidContact() {
    ReviewRequestDto reviewRequestDto = new ReviewRequestDto(REVIEW_TITLE, REVIEW_MESSAGE, REVIEW_STARS);
    User requester = UserUtils.create();
    requester.setId(TEST_USER_ID_REQUESTER);
    User serviceProvider = UserUtils.create();
    serviceProvider.setId(TEST_USER_ID_SERVICE_PROVIDER);
    Contact contact = ContactUtils.create(serviceProvider);
    contact.setStandard(true);
    contact.setVerificationCompletedAt(null);

    Conversation conversation = ConversationUtils.create(requester, serviceProvider, null, List.of());
    conversation.setOfferStatus(OfferStatusEnum.ACCEPTED);

    RequestedService requestedService = RequestedServiceUtils.create(requester, null, List.of(conversation));
    requestedService.setStatus(RequestedServiceStatusEnum.DONE);
    conversation.setRequestedService(requestedService);

    Review review = ReviewUtils.create(requester, requestedService);

    serviceProvider.setContacts(List.of(contact));

    when(jwtService.getClaims()).thenReturn(Optional.of(Map.of("sub", JWT_SUB_CLAIM)));
    when(userService.findById(TEST_USER_ID_REQUESTER)).thenReturn(requester);
    when(requestedServiceService.findById(TEST_REQUESTED_SERVICE_ID)).thenReturn(requestedService);
    when(reviewRepository.save(any(Review.class))).thenReturn(review);

    reviewService.register(TEST_REQUESTED_SERVICE_ID, reviewRequestDto);

    verify(jwtService, times(1)).getClaims();
    verify(userService, times(1)).findById(TEST_USER_ID_REQUESTER);
    verify(requestedServiceService, times(1)).findById(TEST_REQUESTED_SERVICE_ID);
    verify(reviewRepository, times(1)).save(any(Review.class));
    verify(notificationService, never()).send(any());
  }

  @Test
  @DisplayName("Should throw ValidationException when requested service is not DONE")
  void shouldThrowValidationExceptionWhenRequestedServiceIsNotDone() {
    ReviewRequestDto reviewRequestDto = new ReviewRequestDto(REVIEW_TITLE, REVIEW_MESSAGE, REVIEW_STARS);
    User requester = UserUtils.create();
    requester.setId(TEST_USER_ID_REQUESTER);

    RequestedService requestedService = RequestedServiceUtils.create(requester, null, List.of());
    requestedService.setStatus(RequestedServiceStatusEnum.INPROGRESS);

    when(jwtService.getClaims()).thenReturn(Optional.of(Map.of("sub", JWT_SUB_CLAIM)));
    when(userService.findById(TEST_USER_ID_REQUESTER)).thenReturn(requester);
    when(requestedServiceService.findById(TEST_REQUESTED_SERVICE_ID)).thenReturn(requestedService);

    assertThatThrownBy(() -> reviewService.register(TEST_REQUESTED_SERVICE_ID, reviewRequestDto))
        .isInstanceOf(ValidationException.class)
        .hasMessageContaining("Feedback can only be provided for services that have been completed.");

    verify(jwtService, times(1)).getClaims();
    verify(userService, times(1)).findById(TEST_USER_ID_REQUESTER);
    verify(requestedServiceService, times(1)).findById(TEST_REQUESTED_SERVICE_ID);
    verify(reviewRepository, never()).save(any(Review.class));
    verify(notificationService, never()).send(any());
  }

  @Test
  @DisplayName("Should throw NoSuchElementException when JWT does not have sub claim")
  void shouldThrowExceptionWhenTokenDoesNotHaveSubClaim() {
    ReviewRequestDto reviewRequestDto = new ReviewRequestDto(REVIEW_TITLE, REVIEW_MESSAGE, REVIEW_STARS);

    when(jwtService.getClaims()).thenReturn(Optional.empty());

    assertThatThrownBy(() -> reviewService.register(TEST_REQUESTED_SERVICE_ID, reviewRequestDto))
        .isInstanceOf(NoSuchElementException.class);

    verify(jwtService, times(1)).getClaims();
    verify(userService, never()).findById(anyLong());
    verify(requestedServiceService, never()).findById(anyLong());
    verify(reviewRepository, never()).save(any(Review.class));
    verify(notificationService, never()).send(any());
  }

  @Test
  @DisplayName("Should throw ResourceNotFoundException when user not found during registration")
  void shouldThrowExceptionWhenUserNotFound() {
    ReviewRequestDto reviewRequestDto = new ReviewRequestDto(REVIEW_TITLE, REVIEW_MESSAGE, REVIEW_STARS);

    when(jwtService.getClaims()).thenReturn(Optional.of(Map.of("sub", JWT_SUB_CLAIM)));
    when(userService.findById(TEST_USER_ID_REQUESTER)).thenThrow(new ResourceNotFoundException("User not found"));

    assertThatThrownBy(() -> reviewService.register(TEST_REQUESTED_SERVICE_ID, reviewRequestDto))
        .isInstanceOf(ResourceNotFoundException.class)
        .hasMessageContaining("User not found");

    verify(jwtService, times(1)).getClaims();
    verify(userService, times(1)).findById(TEST_USER_ID_REQUESTER);
    verify(requestedServiceService, never()).findById(anyLong());
    verify(reviewRepository, never()).save(any(Review.class));
    verify(notificationService, never()).send(any());
  }

  @Test
  @DisplayName("Should throw ResourceNotFoundException when requested service not found during registration")
  void shouldThrowExceptionWhenRequestedServiceNotFound() {
    ReviewRequestDto reviewRequestDto = new ReviewRequestDto(REVIEW_TITLE, REVIEW_MESSAGE, REVIEW_STARS);
    User requester = UserUtils.create();
    requester.setId(TEST_USER_ID_REQUESTER);

    when(jwtService.getClaims()).thenReturn(Optional.of(Map.of("sub", JWT_SUB_CLAIM)));
    when(userService.findById(TEST_USER_ID_REQUESTER)).thenReturn(requester);
    when(requestedServiceService.findById(TEST_REQUESTED_SERVICE_ID))
        .thenThrow(new ResourceNotFoundException("RequestedService not found"));

    assertThatThrownBy(() -> reviewService.register(TEST_REQUESTED_SERVICE_ID, reviewRequestDto))
        .isInstanceOf(ResourceNotFoundException.class)
        .hasMessageContaining("RequestedService not found");

    verify(jwtService, times(1)).getClaims();
    verify(userService, times(1)).findById(TEST_USER_ID_REQUESTER);
    verify(requestedServiceService, times(1)).findById(TEST_REQUESTED_SERVICE_ID);
    verify(reviewRepository, never()).save(any(Review.class));
    verify(notificationService, never()).send(any());
  }

  @Test
  @DisplayName("Should propagate exception when review repository fails during registration")
  void shouldPropagateExceptionWhenReviewRepositoryFails() {
    ReviewRequestDto reviewRequestDto = new ReviewRequestDto(REVIEW_TITLE, REVIEW_MESSAGE, REVIEW_STARS);
    User requester = UserUtils.create();
    requester.setId(TEST_USER_ID_REQUESTER);

    RequestedService requestedService = RequestedServiceUtils.create(requester, null, List.of());
    requestedService.setStatus(RequestedServiceStatusEnum.DONE);

    when(jwtService.getClaims()).thenReturn(Optional.of(Map.of("sub", JWT_SUB_CLAIM)));
    when(userService.findById(TEST_USER_ID_REQUESTER)).thenReturn(requester);
    when(requestedServiceService.findById(TEST_REQUESTED_SERVICE_ID)).thenReturn(requestedService);
    when(reviewRepository.save(any(Review.class)))
        .thenThrow(new RuntimeException("Database error"));

    assertThatThrownBy(() -> reviewService.register(TEST_REQUESTED_SERVICE_ID, reviewRequestDto))
        .isInstanceOf(RuntimeException.class)
        .hasMessageContaining("Database error");

    verify(jwtService, times(1)).getClaims();
    verify(userService, times(1)).findById(TEST_USER_ID_REQUESTER);
    verify(requestedServiceService, times(1)).findById(TEST_REQUESTED_SERVICE_ID);
    verify(reviewRepository, times(1)).save(any(Review.class));
    verify(notificationService, never()).send(any());
  }

  @Test
  @DisplayName("Should return reviews given by user when isReviewOwner is true")
  void shouldReturnReviewsGivenByUserWhenIsReviewOwnerTrue() {
    Pageable pageable = PageRequest.of(0, 10);
    Review review = new Review();
    review.setId(100L);
    Page<Review> reviewPage = new PageImpl<>(List.of(review), pageable, 1);

    when(reviewRepository.findReviewsGivenByUserId(eq(TEST_USER_ID_REQUESTER), eq(pageable))).thenReturn(reviewPage);

    Page<ReviewResponseDto> result = reviewService.findByUserId(TEST_USER_ID_REQUESTER, true, pageable);

    assertThat(result).isNotNull();
    assertThat(result.getContent()).hasSize(1);
    assertThat(result.getContent().get(0).id()).isEqualTo(100L);

    verify(reviewRepository, times(1)).findReviewsGivenByUserId(eq(TEST_USER_ID_REQUESTER), eq(pageable));
    verify(reviewRepository, never()).findReviewsReceivedByUserId(anyLong(), any(Pageable.class));
  }

  @Test
  @DisplayName("Should return reviews received by user when isReviewOwner is false")
  void shouldReturnReviewsReceivedByUserWhenIsReviewOwnerFalse() {
    Pageable pageable = PageRequest.of(0, 10);
    Review review = new Review();
    review.setId(200L);
    Page<Review> reviewPage = new PageImpl<>(List.of(review), pageable, 1);

    when(reviewRepository.findReviewsReceivedByUserId(eq(TEST_USER_ID_SERVICE_PROVIDER), eq(pageable)))
        .thenReturn(reviewPage);

    Page<ReviewResponseDto> result = reviewService.findByUserId(TEST_USER_ID_SERVICE_PROVIDER, false, pageable);

    assertThat(result).isNotNull();
    assertThat(result.getContent()).hasSize(1);
    assertThat(result.getContent().get(0).id()).isEqualTo(200L);

    verify(reviewRepository, times(1)).findReviewsReceivedByUserId(eq(TEST_USER_ID_SERVICE_PROVIDER), eq(pageable));
    verify(reviewRepository, never()).findReviewsGivenByUserId(anyLong(), any(Pageable.class));
  }

  @Test
  @DisplayName("Should return empty page when no reviews found for user ID")
  void shouldReturnEmptyPageWhenNoReviewsFound() {
    Pageable pageable = PageRequest.of(0, 10);
    Page<Review> emptyPage = Page.empty(pageable);

    when(reviewRepository.findReviewsGivenByUserId(eq(TEST_USER_ID_REQUESTER), eq(pageable))).thenReturn(emptyPage);

    Page<ReviewResponseDto> result = reviewService.findByUserId(TEST_USER_ID_REQUESTER, true, pageable);

    assertThat(result).isNotNull();
    assertThat(result.getContent()).isEmpty();
    verify(reviewRepository, times(1)).findReviewsGivenByUserId(eq(TEST_USER_ID_REQUESTER), eq(pageable));
  }

  @Test
  @DisplayName("Should return review when found by ID")
  void shouldReturnReviewWhenFoundById() {
    Review review = new Review();
    review.setId(1L);

    when(reviewRepository.findById(eq(1L))).thenReturn(Optional.of(review));

    Review result = reviewService.findById(1L);

    assertThat(result).isNotNull();
    assertThat(result.getId()).isEqualTo(1L);
    verify(reviewRepository, times(1)).findById(eq(1L));
  }

  @Test
  @DisplayName("Should throw ResourceNotFoundException when review not found by ID")
  void shouldThrowResourceNotFoundExceptionWhenReviewNotFound() {
    when(reviewRepository.findById(anyLong())).thenReturn(Optional.empty());

    assertThatThrownBy(() -> reviewService.findById(1L))
        .isInstanceOf(ResourceNotFoundException.class)
        .hasMessage("Review not found.");
    verify(reviewRepository, times(1)).findById(anyLong());
  }

  @Test
  @DisplayName("Should set deletedAt and save when review exists for soft deletion")
  void shouldSetDeletedAtAndSaveWhenReviewExists() {
    Review review = new Review();
    review.setId(1L);

    when(reviewRepository.findById(eq(1L))).thenReturn(Optional.of(review));
    when(reviewRepository.save(any(Review.class))).thenAnswer(invocation -> invocation.getArgument(0));

    reviewService.deleteById(1L);

    assertThat(review.getDeletedAt()).isNotNull();
    verify(reviewRepository, times(1)).findById(eq(1L));
    verify(reviewRepository, times(1)).save(eq(review));
  }

  @Test
  @DisplayName("Should do nothing when review does not exist for soft deletion")
  void shouldDoNothingWhenReviewDoesNotExist() {
    when(reviewRepository.findById(anyLong())).thenReturn(Optional.empty());

    reviewService.deleteById(1L);

    verify(reviewRepository, times(1)).findById(anyLong());
    verify(reviewRepository, never()).save(any(Review.class));
  }

  @Test
  @DisplayName("Should update review successfully")
  void shouldUpdateReviewSuccessfully() {
    ReviewRequestDto updatedReviewDto = new ReviewRequestDto("New Title", "New Review", 4);
    User requester = UserUtils.create();
    requester.setId(TEST_USER_ID_REQUESTER);
    User serviceProvider = UserUtils.create();
    serviceProvider.setId(TEST_USER_ID_SERVICE_PROVIDER);

    Contact contact = ContactUtils.create(serviceProvider);
    contact.setStandard(true);
    contact.setVerificationCompletedAt(LocalDateTime.now());
    serviceProvider.setContacts(List.of(contact));

    Conversation conversation = ConversationUtils.create(requester, serviceProvider, null, List.of());
    conversation.setOfferStatus(OfferStatusEnum.ACCEPTED);

    RequestedService requestedService = RequestedServiceUtils.create(requester, null, List.of(conversation));
    requestedService.setStatus(RequestedServiceStatusEnum.DONE);
    conversation.setRequestedService(requestedService);

    Review existingReview = ReviewUtils.create(serviceProvider, requestedService);
    existingReview.setId(1L);
    existingReview.setUser(serviceProvider);

    when(reviewRepository.findById(eq(1L))).thenReturn(Optional.of(existingReview));
    when(reviewRepository.save(any(Review.class))).thenAnswer(invocation -> invocation.getArgument(0));

    ReviewResponseDto response = reviewService.updateById(1L, updatedReviewDto);

    assertThat(response).isNotNull();
    assertThat(response.title()).isEqualTo(updatedReviewDto.title());
    assertThat(response.review()).isEqualTo(updatedReviewDto.review());
    assertThat(response.stars()).isEqualTo(updatedReviewDto.stars());
    assertThat(existingReview.getUpdatedAt()).isNotNull();

    verify(reviewRepository, times(1)).findById(eq(1L));
    verify(reviewRepository, times(1)).save(eq(existingReview));
  }
}
