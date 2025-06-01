package br.com.conectabyte.profissu.controllers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;

import br.com.conectabyte.profissu.config.SecurityConfig;
import br.com.conectabyte.profissu.dtos.request.ReviewRequestDto;
import br.com.conectabyte.profissu.dtos.response.ReviewResponseDto;
import br.com.conectabyte.profissu.properties.ProfissuProperties;
import br.com.conectabyte.profissu.services.ReviewService;
import br.com.conectabyte.profissu.services.security.SecurityRequestedServiceService;
import br.com.conectabyte.profissu.services.security.SecurityReviewService;

@WebMvcTest({ ReviewController.class, SecurityReviewService.class, SecurityRequestedServiceService.class,
    ProfissuProperties.class })
@Import(SecurityConfig.class)
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

  @Test
  @WithMockUser
  void shouldRegisterReviewSuccessfully() throws Exception {
    final var validRequest = new ReviewRequestDto("Title", "Review", 5);
    final var response = new ReviewResponseDto(1L, validRequest.title(), validRequest.review(),
        validRequest.stars(), null, null);

    when(securityRequestedServiceService.ownershipCheck(any())).thenReturn(true);
    when(reviewService.register(any(), any())).thenReturn(response);

    performPost(validRequest).andExpect(status().isOk());
  }

  @Test
  @WithMockUser
  void shouldReturnBadRequestWhenTitleIsNull() throws Exception {
    final var invalidRequest = new ReviewRequestDto(null, "Review", 5);

    performPost(invalidRequest).andExpect(status().isBadRequest());
  }

  @Test
  @WithMockUser
  void shouldReturnBadRequestWhenTitleIsBlank() throws Exception {
    final var invalidRequest = new ReviewRequestDto(" ", "Review", 5);

    performPost(invalidRequest).andExpect(status().isBadRequest());
  }

  @Test
  @WithMockUser
  void shouldReturnBadRequestWhenReviewIsNull() throws Exception {
    final var invalidRequest = new ReviewRequestDto("Title", null, 5);

    performPost(invalidRequest).andExpect(status().isBadRequest());
  }

  @Test
  @WithMockUser
  void shouldReturnBadRequestWhenReviewIsBlank() throws Exception {
    final var invalidRequest = new ReviewRequestDto("Title", " ", 5);

    performPost(invalidRequest).andExpect(status().isBadRequest());
  }

  @Test
  @WithMockUser
  void shouldReturnBadRequestWhenStarsIsZero() throws Exception {
    final var invalidRequest = new ReviewRequestDto("Title", "Review", 0);

    performPost(invalidRequest).andExpect(status().isBadRequest());
  }

  @Test
  @WithMockUser
  void shouldReturnBadRequestWhenStarsIsAboveFive() throws Exception {
    final var invalidRequest = new ReviewRequestDto("Title", "Review", 6);

    performPost(invalidRequest).andExpect(status().isBadRequest());
  }

  @Test
  @WithMockUser
  void shouldReturnForbiddenWhenOwnershipCheckFails() throws Exception {
    final var validRequest = new ReviewRequestDto("Title", "Review", 5);

    when(securityRequestedServiceService.ownershipCheck(any())).thenReturn(false);

    mockMvc.perform(post("/reviews")
        .param("requestedServiceId", "1")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(validRequest)))
        .andExpect(status().isForbidden());
  }

  @Test
  @WithMockUser
  void shouldFindReviewsByUserIdSuccessfully() throws Exception {
    final var response = new PageImpl<>(List.of(
        new ReviewResponseDto(1L, "Title", "Review", 5, null, null)));

    when(reviewService.findByUserId(anyLong(), anyBoolean(), any())).thenReturn(response);

    mockMvc.perform(get("/reviews")
        .param("userId", "1")
        .param("isReviewOwner", "true"))
        .andExpect(status().isOk());
  }

  @Test
  @WithMockUser
  void shouldDeleteReviewSuccessfully() throws Exception {
    doNothing().when(reviewService).deleteById(anyLong());

    when(securityReviewService.ownershipCheck(any())).thenReturn(true);

    mockMvc.perform(delete("/reviews/{id}", 1L))
        .andExpect(status().isAccepted());
  }

  @Test
  @WithMockUser
  void shouldUpdateReviewSuccessfully() throws Exception {
    final var validRequest = new ReviewRequestDto("Updated Title", "Updated Review", 4);
    final var response = new ReviewResponseDto(1L, validRequest.title(), validRequest.review(), validRequest.stars(),
        null, null);

    when(securityReviewService.ownershipCheck(any())).thenReturn(true);
    when(reviewService.updateById(anyLong(), any())).thenReturn(response);

    mockMvc.perform(put("/reviews/{id}", 1L)
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(validRequest)))
        .andExpect(status().isOk());
  }

  @Test
  @WithMockUser
  void shouldReturnBadRequestWhenUpdateTitleIsNull() throws Exception {
    final var invalidRequest = new ReviewRequestDto(null, "Updated Review", 4);

    mockMvc.perform(put("/reviews/{id}", 1L)
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(invalidRequest)))
        .andExpect(status().isBadRequest());
  }

  @Test
  @WithMockUser
  void shouldReturnBadRequestWhenUpdateReviewIsBlank() throws Exception {
    final var invalidRequest = new ReviewRequestDto("Updated Title", " ", 4);

    mockMvc.perform(put("/reviews/{id}", 1L)
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(invalidRequest)))
        .andExpect(status().isBadRequest());
  }

  @Test
  @WithMockUser
  void shouldReturnForbiddenWhenOwnershipCheckFailsOnUpdate() throws Exception {
    final var validRequest = new ReviewRequestDto("Updated Title", "Updated Review", 4);

    when(securityReviewService.ownershipCheck(any())).thenReturn(false);

    mockMvc.perform(put("/reviews/{id}", 1L)
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(validRequest)))
        .andExpect(status().isForbidden());
  }

  private org.springframework.test.web.servlet.ResultActions performPost(ReviewRequestDto request) throws Exception {
    return mockMvc.perform(post("/reviews")
        .param("requestedServiceId", "1")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request)));
  }
}
