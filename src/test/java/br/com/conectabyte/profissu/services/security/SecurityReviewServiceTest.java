package br.com.conectabyte.profissu.services.security;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import br.com.conectabyte.profissu.entities.Review;
import br.com.conectabyte.profissu.entities.User;
import br.com.conectabyte.profissu.exceptions.ResourceNotFoundException;
import br.com.conectabyte.profissu.services.ReviewService;
import br.com.conectabyte.profissu.utils.ReviewUtils;
import br.com.conectabyte.profissu.utils.UserUtils;

@ExtendWith(MockitoExtension.class)
@DisplayName("SecurityReviewService Tests")
public class SecurityReviewServiceTest {

  @Mock
  private ReviewService reviewService;

  @Mock
  private SecurityService securityService;

  @InjectMocks
  private SecurityReviewService securityReviewService;

  private static final Long TEST_REVIEW_ID = 1L;
  private static final Long AUTHENTICATED_USER_ID = 10L;
  private static final Long OTHER_USER_ID = 20L;

  @Test
  @DisplayName("Should return true when authenticated user is owner of review")
  void shouldReturnTrueWhenUserIsOwnerOfReview() {
    User ownerUser = UserUtils.create();
    ownerUser.setId(AUTHENTICATED_USER_ID);
    Review review = ReviewUtils.create(ownerUser, null);

    when(reviewService.findById(eq(TEST_REVIEW_ID))).thenReturn(review);
    when(securityService.isOwner(eq(AUTHENTICATED_USER_ID))).thenReturn(true);

    boolean isOwner = securityReviewService.ownershipCheck(TEST_REVIEW_ID);

    assertTrue(isOwner);
    verify(reviewService, times(1)).findById(eq(TEST_REVIEW_ID));
    verify(securityService, times(1)).isOwner(eq(AUTHENTICATED_USER_ID));
  }

  @Test
  @DisplayName("Should return false when authenticated user is not owner of review")
  void shouldReturnFalseWhenUserIsNotOwnerOfReview() {
    User reviewOwner = UserUtils.create();
    reviewOwner.setId(OTHER_USER_ID);
    Review review = ReviewUtils.create(reviewOwner, null);

    when(reviewService.findById(eq(TEST_REVIEW_ID))).thenReturn(review);
    when(securityService.isOwner(eq(OTHER_USER_ID))).thenReturn(false);

    boolean isOwner = securityReviewService.ownershipCheck(TEST_REVIEW_ID);

    assertFalse(isOwner);
    verify(reviewService, times(1)).findById(eq(TEST_REVIEW_ID));
    verify(securityService, times(1)).isOwner(eq(OTHER_USER_ID));
  }

  @Test
  @DisplayName("Should return false when review not found")
  void shouldReturnFalseWhenReviewNotFound() {
    when(reviewService.findById(eq(TEST_REVIEW_ID))).thenThrow(new ResourceNotFoundException("Review not found"));

    boolean isOwner = securityReviewService.ownershipCheck(TEST_REVIEW_ID);

    assertFalse(isOwner);
    verify(reviewService, times(1)).findById(eq(TEST_REVIEW_ID));
    verify(securityService, never()).isOwner(anyLong());
  }

  @Test
  @DisplayName("Should return false when an unexpected exception occurs during ownership check")
  void shouldReturnFalseWhenUnexpectedExceptionOccurs() {
    when(reviewService.findById(eq(TEST_REVIEW_ID))).thenThrow(new RuntimeException("Simulated error"));

    boolean isOwner = securityReviewService.ownershipCheck(TEST_REVIEW_ID);

    assertFalse(isOwner);
    verify(reviewService, times(1)).findById(eq(TEST_REVIEW_ID));
    verify(securityService, never()).isOwner(anyLong());
  }
}
