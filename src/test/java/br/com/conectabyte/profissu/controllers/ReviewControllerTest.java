package br.com.conectabyte.profissu.controllers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import com.fasterxml.jackson.databind.ObjectMapper;

import br.com.conectabyte.profissu.config.SecurityConfig;
import br.com.conectabyte.profissu.dtos.request.ReviewRequestDto;
import br.com.conectabyte.profissu.dtos.response.ReviewResponseDto;
import br.com.conectabyte.profissu.exceptions.ResourceNotFoundException;
import br.com.conectabyte.profissu.properties.ProfissuProperties;
import br.com.conectabyte.profissu.services.ReviewService;
import br.com.conectabyte.profissu.services.security.SecurityRequestedServiceService;
import br.com.conectabyte.profissu.services.security.SecurityReviewService;
import br.com.conectabyte.profissu.services.security.SecurityService;

@WebMvcTest({ ReviewController.class, SecurityReviewService.class, SecurityRequestedServiceService.class,
    ProfissuProperties.class, SecurityService.class })
@Import(SecurityConfig.class)
@DisplayName("ReviewController Tests")
class ReviewControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @MockitoBean
  private ReviewService reviewService;

  @MockitoBean
  private SecurityReviewService securityReviewService;

  @MockitoBean
  private SecurityRequestedServiceService securityRequestedServiceService;

  @MockitoBean
  private SecurityService securityService;

  @Test
  @WithMockUser
  @DisplayName("Should find reviews by user ID successfully")
  void shouldFindReviewsByUserIdSuccessfully() throws Exception {
    final var response = new PageImpl<>(List.of(
        new ReviewResponseDto(1L, "Title", "Review", 5, null, null)));

    when(reviewService.findByUserId(anyLong(), anyBoolean(), any())).thenReturn(response);

    mockMvc.perform(get("/reviews")
        .param("userId", "1")
        .param("isReviewOwner", "true"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content[0].title").value("Title"));
  }

  @Test
  @DisplayName("Should return unauthorized when finding reviews by user ID and user is not authenticated")
  void shouldReturnUnauthorizedOnFindReviewsByUserId() throws Exception {
    mockMvc.perform(get("/reviews")
        .param("userId", "1")
        .param("isReviewOwner", "true"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  @WithMockUser
  @DisplayName("Should register review successfully when user is requested service owner")
  void shouldRegisterReviewSuccessfullyWhenIsRequestedServiceOwner() throws Exception {
    final var validRequest = new ReviewRequestDto("Title", "Review", 5);
    final var response = new ReviewResponseDto(1L, validRequest.title(), validRequest.review(),
        validRequest.stars(), null, null);

    when(securityRequestedServiceService.ownershipCheck(anyLong())).thenReturn(true);
    when(securityRequestedServiceService.isServiceProvider(anyLong())).thenReturn(false);
    when(reviewService.register(anyLong(), any(ReviewRequestDto.class))).thenReturn(response);

    performPostRegister(validRequest).andExpect(status().isOk())
        .andExpect(jsonPath("$.title").value("Title"));
  }

  @Test
  @WithMockUser
  @DisplayName("Should register review successfully when user is service provider")
  void shouldRegisterReviewSuccessfullyWhenIsServiceProvider() throws Exception {
    final var validRequest = new ReviewRequestDto("Title", "Review", 5);
    final var response = new ReviewResponseDto(1L, validRequest.title(), validRequest.review(),
        validRequest.stars(), null, null);

    when(securityRequestedServiceService.ownershipCheck(anyLong())).thenReturn(false);
    when(securityRequestedServiceService.isServiceProvider(anyLong())).thenReturn(true);
    when(reviewService.register(anyLong(), any(ReviewRequestDto.class))).thenReturn(response);

    performPostRegister(validRequest).andExpect(status().isOk())
        .andExpect(jsonPath("$.title").value("Title"));
  }

  @Test
  @DisplayName("Should return unauthorized when registering review and user is not authenticated")
  void shouldReturnUnauthorizedOnRegisterReview() throws Exception {
    final var validRequest = new ReviewRequestDto("Title", "Review", 5);

    mockMvc.perform(post("/reviews")
        .param("requestedServiceId", "1")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(validRequest)))
        .andExpect(status().isUnauthorized());
  }

  @Test
  @WithMockUser
  @DisplayName("Should return forbidden when neither requested service owner nor service provider")
  void shouldReturnForbiddenOnRegisterReviewWhenNotAuthorized() throws Exception {
    final var validRequest = new ReviewRequestDto("Title", "Review", 5);

    when(securityRequestedServiceService.ownershipCheck(anyLong())).thenReturn(false);
    when(securityRequestedServiceService.isServiceProvider(anyLong())).thenReturn(false);

    performPostRegister(validRequest).andExpect(status().isForbidden())
        .andExpect(jsonPath("$.message").value("Access denied."));
  }

  @Test
  @WithMockUser
  @DisplayName("Should return not found when requested service does not exist on register review")
  void shouldReturnNotFoundWhenRequestedServiceDoesNotExistOnRegisterReview() throws Exception {
    final var validRequest = new ReviewRequestDto("Title", "Review", 5);

    when(securityRequestedServiceService.ownershipCheck(anyLong())).thenReturn(true);
    when(reviewService.register(anyLong(), any(ReviewRequestDto.class)))
        .thenThrow(new ResourceNotFoundException("Requested service not found"));

    performPostRegister(validRequest).andExpect(status().isNotFound())
        .andExpect(jsonPath("$.message").value("Requested service not found"));
  }

  @Test
  @WithMockUser
  @DisplayName("Should return bad request when title is null on register")
  void shouldReturnBadRequestWhenTitleIsNullOnRegister() throws Exception {
    final var invalidRequest = new ReviewRequestDto(null, "Review", 5);

    performPostRegister(invalidRequest).andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("All fields must be valid"));
  }

  @Test
  @WithMockUser
  @DisplayName("Should return bad request when title is blank on register")
  void shouldReturnBadRequestWhenTitleIsBlankOnRegister() throws Exception {
    final var invalidRequest = new ReviewRequestDto(" ", "Review", 5);

    performPostRegister(invalidRequest).andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("All fields must be valid"));
  }

  @Test
  @WithMockUser
  @DisplayName("Should return bad request when review is null on register")
  void shouldReturnBadRequestWhenReviewIsNullOnRegister() throws Exception {
    final var invalidRequest = new ReviewRequestDto("Title", null, 5);

    performPostRegister(invalidRequest).andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("All fields must be valid"));
  }

  @Test
  @WithMockUser
  @DisplayName("Should return bad request when review is blank on register")
  void shouldReturnBadRequestWhenReviewIsBlankOnRegister() throws Exception {
    final var invalidRequest = new ReviewRequestDto("Title", " ", 5);

    performPostRegister(invalidRequest).andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("All fields must be valid"));
  }

  @Test
  @WithMockUser
  @DisplayName("Should return bad request when stars is zero on register")
  void shouldReturnBadRequestWhenStarsIsZeroOnRegister() throws Exception {
    final var invalidRequest = new ReviewRequestDto("Title", "Review", 0);

    performPostRegister(invalidRequest).andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("All fields must be valid"));
  }

  @Test
  @WithMockUser
  @DisplayName("Should return bad request when stars is above five on register")
  void shouldReturnBadRequestWhenStarsIsAboveFiveOnRegister() throws Exception {
    final var invalidRequest = new ReviewRequestDto("Title", "Review", 6);

    performPostRegister(invalidRequest).andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("All fields must be valid"));
  }

  @Test
  @WithMockUser
  @DisplayName("Should return bad request for malformed JSON on register review")
  void shouldReturnBadRequestForMalformedJsonOnRegisterReview() throws Exception {
    mockMvc.perform(post("/reviews")
        .param("requestedServiceId", "1")
        .contentType(MediaType.APPLICATION_JSON)
        .content("{invalidJson}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("Malformed json"));
  }

  @Test
  @WithMockUser
  @DisplayName("Should update review successfully")
  void shouldUpdateReviewSuccessfully() throws Exception {
    final var validRequest = new ReviewRequestDto("Updated Title", "Updated Review", 4);
    final var response = new ReviewResponseDto(1L, validRequest.title(), validRequest.review(), validRequest.stars(),
        null, null);

    when(securityReviewService.ownershipCheck(anyLong())).thenReturn(true);
    when(reviewService.updateById(anyLong(), any(ReviewRequestDto.class))).thenReturn(response);

    mockMvc.perform(put("/reviews/{id}", 1L)
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(validRequest)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.title").value("Updated Title"));
  }

  @Test
  @DisplayName("Should return unauthorized when updating review and user is not authenticated")
  void shouldReturnUnauthorizedOnUpdateReview() throws Exception {
    final var validRequest = new ReviewRequestDto("Updated Title", "Updated Review", 4);

    mockMvc.perform(put("/reviews/{id}", 1L)
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(validRequest)))
        .andExpect(status().isUnauthorized());
  }

  @Test
  @WithMockUser
  @DisplayName("Should return forbidden when ownership check fails on update")
  void shouldReturnForbiddenWhenOwnershipCheckFailsOnUpdate() throws Exception {
    final var validRequest = new ReviewRequestDto("Updated Title", "Updated Review", 4);

    when(securityReviewService.ownershipCheck(anyLong())).thenReturn(false);

    mockMvc.perform(put("/reviews/{id}", 1L)
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(validRequest)))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.message").value("Access denied."));
  }

  @Test
  @WithMockUser
  @DisplayName("Should return not found when review does not exist on update")
  void shouldReturnNotFoundWhenReviewDoesNotExistOnUpdate() throws Exception {
    final var validRequest = new ReviewRequestDto("Updated Title", "Updated Review", 4);

    when(securityReviewService.ownershipCheck(anyLong())).thenReturn(true);
    when(reviewService.updateById(anyLong(), any(ReviewRequestDto.class)))
        .thenThrow(new ResourceNotFoundException("Review not found"));

    mockMvc.perform(put("/reviews/{id}", 999L)
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(validRequest)))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.message").value("Review not found"));
  }

  @Test
  @WithMockUser
  @DisplayName("Should return bad request when update title is null")
  void shouldReturnBadRequestWhenUpdateTitleIsNull() throws Exception {
    final var invalidRequest = new ReviewRequestDto(null, "Updated Review", 4);

    mockMvc.perform(put("/reviews/{id}", 1L)
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(invalidRequest)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("All fields must be valid"));
  }

  @Test
  @WithMockUser
  @DisplayName("Should return bad request when update review is blank")
  void shouldReturnBadRequestWhenUpdateReviewIsBlank() throws Exception {
    final var invalidRequest = new ReviewRequestDto("Updated Title", " ", 4);

    mockMvc.perform(put("/reviews/{id}", 1L)
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(invalidRequest)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("All fields must be valid"));
  }

  @Test
  @WithMockUser
  @DisplayName("Should return bad request for malformed JSON on update review")
  void shouldReturnBadRequestForMalformedJsonOnUpdateReview() throws Exception {
    when(securityReviewService.ownershipCheck(anyLong())).thenReturn(true);

    mockMvc.perform(put("/reviews/{id}", 1L)
        .contentType(MediaType.APPLICATION_JSON)
        .content("{invalidJson}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("Malformed json"));
  }

  @Test
  @WithMockUser
  @DisplayName("Should delete review successfully when user is owner")
  void shouldDeleteReviewSuccessfullyWhenIsOwner() throws Exception {
    doNothing().when(reviewService).deleteById(anyLong());
    when(securityReviewService.ownershipCheck(anyLong())).thenReturn(true);
    when(securityService.isAdmin()).thenReturn(false);

    mockMvc.perform(delete("/reviews/{id}", 1L))
        .andExpect(status().isAccepted());
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  @DisplayName("Should delete review successfully when user is admin")
  void shouldDeleteReviewSuccessfullyWhenIsAdmin() throws Exception {
    doNothing().when(reviewService).deleteById(anyLong());
    when(securityReviewService.ownershipCheck(anyLong())).thenReturn(false);
    when(securityService.isAdmin()).thenReturn(true);

    mockMvc.perform(delete("/reviews/{id}", 1L))
        .andExpect(status().isAccepted());
  }

  @Test
  @DisplayName("Should return unauthorized when deleting review and user is not authenticated")
  void shouldReturnUnauthorizedOnDeleteReview() throws Exception {
    mockMvc.perform(delete("/reviews/{id}", 1L))
        .andExpect(status().isUnauthorized());
  }

  @Test
  @WithMockUser
  @DisplayName("Should return forbidden when user is neither owner nor admin on delete review")
  void shouldReturnForbiddenOnDeleteReviewWhenNotAuthorized() throws Exception {
    when(securityReviewService.ownershipCheck(anyLong())).thenReturn(false);
    when(securityService.isAdmin()).thenReturn(false);

    mockMvc.perform(delete("/reviews/{id}", 1L))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.message").value("Access denied."));
  }

  @Test
  @WithMockUser
  @DisplayName("Should return not found when review does not exist on delete")
  void shouldReturnNotFoundWhenReviewDoesNotExistOnDelete() throws Exception {
    doNothing().when(reviewService).deleteById(anyLong());
    when(securityReviewService.ownershipCheck(anyLong())).thenReturn(true);
    when(securityService.isAdmin()).thenReturn(false);
    doThrow(new ResourceNotFoundException("Review not found")).when(reviewService).deleteById(anyLong());

    mockMvc.perform(delete("/reviews/{id}", 999L))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.message").value("Review not found"));
  }

  private ResultActions performPostRegister(ReviewRequestDto request) throws Exception {
    return mockMvc.perform(post("/reviews")
        .param("requestedServiceId", "1")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request)));
  }
}
