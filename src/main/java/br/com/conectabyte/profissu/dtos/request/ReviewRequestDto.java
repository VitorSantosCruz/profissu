package br.com.conectabyte.profissu.dtos.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record ReviewRequestDto(
    @NotBlank(message = "title: Cannot be null or empty") String title,
    @NotBlank(message = "review: Cannot be null or empty") String review,
    @Min(value = 1, message = "stars: cannot be less than 1") @Max(value = 5, message = "stars: cannot be greater than 5") int stars) {
}
