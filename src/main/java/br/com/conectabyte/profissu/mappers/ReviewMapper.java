package br.com.conectabyte.profissu.mappers;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import br.com.conectabyte.profissu.dtos.request.ReviewRequestDto;
import br.com.conectabyte.profissu.dtos.response.ReviewResponseDto;
import br.com.conectabyte.profissu.entities.Review;

@Mapper
public interface ReviewMapper {
  ReviewMapper INSTANCE = Mappers.getMapper(ReviewMapper.class);

  @Mapping(target = "createdAt", ignore = true)
  @Mapping(target = "deletedAt", ignore = true)
  @Mapping(target = "id", ignore = true)
  @Mapping(target = "requestedService", ignore = true)
  @Mapping(target = "updatedAt", ignore = true)
  @Mapping(target = "user", ignore = true)
  Review reviewRequestDtoToReview(ReviewRequestDto reviewRequestDto);

  ReviewRequestDto reviewToReviewRequestDto(Review review);

  ReviewResponseDto reviewToReviewResponseDto(Review review);
}
