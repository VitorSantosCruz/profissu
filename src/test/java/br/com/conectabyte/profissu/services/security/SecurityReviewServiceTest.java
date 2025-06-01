package br.com.conectabyte.profissu.services.security;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import br.com.conectabyte.profissu.exceptions.ResourceNotFoundException;
import br.com.conectabyte.profissu.services.ReviewService;
import br.com.conectabyte.profissu.utils.ReviewUtils;
import br.com.conectabyte.profissu.utils.UserUtils;

@ExtendWith(MockitoExtension.class)
public class SecurityReviewServiceTest {

  @Mock
  private ReviewService reviewService;

  @Mock
  private SecurityService securityService;

  @InjectMocks
  private SecurityReviewService securityReviewService;

  @Test
  void shouldReturnTrueWhenUserIsOwnerOfReview() {
    final var user = UserUtils.create();
    final var review = ReviewUtils.create(user, null);

    when(reviewService.findById(any())).thenReturn(review);
    when(securityService.isOwner(any())).thenReturn(true);

    final var isOwner = securityReviewService.ownershipCheck(1L);

    assertTrue(isOwner);
  }

  @Test
  void shouldReturnFalseWhenUserIsNotOwnerOfReview() {
    final var user = UserUtils.create();
    final var review = ReviewUtils.create(user, null);

    when(reviewService.findById(any())).thenReturn(review);
    when(securityService.isOwner(any())).thenReturn(false);

    final var isOwner = securityReviewService.ownershipCheck(1L);

    assertFalse(isOwner);
  }

  @Test
  void shouldReturnFalseWhenReviewNotFound() {
    when(reviewService.findById(any())).thenThrow(new ResourceNotFoundException("Review not found"));

    final var isOwner = securityReviewService.ownershipCheck(1L);

    assertFalse(isOwner);
  }
}
