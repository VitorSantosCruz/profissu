package br.com.conectabyte.profissu.services;

import org.springframework.stereotype.Service;

import br.com.conectabyte.profissu.dtos.request.ReviewRequestDto;
import br.com.conectabyte.profissu.dtos.response.ReviewResponseDto;
import br.com.conectabyte.profissu.mappers.ReviewMapper;
import br.com.conectabyte.profissu.repositories.ReviewRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ReviewService {
  private final ReviewRepository reviewRepository;
  private final UserService userService;
  private final RequestedServiceService requestedServiceService;
  private final JwtService jwtService;

  private final ReviewMapper reviewMapper = ReviewMapper.INSTANCE;

  @Transactional
  public ReviewResponseDto register(Long requestedServiceId, ReviewRequestDto reviewRequestDto) {
    final var userId = this.jwtService.getClaims()
        .map(claims -> Long.valueOf(claims.get("sub").toString()))
        .orElseThrow();
    final var user = userService.findById(userId);
    final var requestedService = requestedServiceService.findById(requestedServiceId);
    final var review = reviewMapper.reviewRequestDtoToReview(reviewRequestDto);

    review.setUser(user);
    review.setRequestedService(requestedService);

    return reviewMapper.reviewToReviewResponseDto(reviewRepository.save(review));
  }
}
