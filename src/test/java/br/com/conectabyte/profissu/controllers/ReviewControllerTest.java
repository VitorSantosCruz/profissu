package br.com.conectabyte.profissu.controllers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
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

@WebMvcTest({ ReviewController.class, SecurityRequestedServiceService.class, ProfissuProperties.class })
@Import(SecurityConfig.class)
class ReviewControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @MockitoBean
  private ReviewService reviewService;

  @MockitoBean
  private SecurityRequestedServiceService securityRequestedServiceService;

  @Test
  @WithMockUser
  void shouldRegisterReviewSuccessfully() throws Exception {
    ReviewRequestDto validRequest = new ReviewRequestDto("Title", "Review", 5);
    ReviewResponseDto response = new ReviewResponseDto(1L, validRequest.title(), validRequest.review(),
        validRequest.stars(), null, null);

    when(securityRequestedServiceService.ownershipCheck(any())).thenReturn(true);
    when(reviewService.register(any(), any())).thenReturn(response);

    performPost(validRequest).andExpect(status().isOk());
  }

  @Test
  @WithMockUser
  void shouldReturnBadRequestWhenTitleIsNull() throws Exception {
    ReviewRequestDto invalidRequest = new ReviewRequestDto(null, "Review", 5);
    performPost(invalidRequest).andExpect(status().isBadRequest());
  }

  @Test
  @WithMockUser
  void shouldReturnBadRequestWhenTitleIsBlank() throws Exception {
    ReviewRequestDto invalidRequest = new ReviewRequestDto(" ", "Review", 5);
    performPost(invalidRequest).andExpect(status().isBadRequest());
  }

  @Test
  @WithMockUser
  void shouldReturnBadRequestWhenReviewIsNull() throws Exception {
    ReviewRequestDto invalidRequest = new ReviewRequestDto("Title", null, 5);
    performPost(invalidRequest).andExpect(status().isBadRequest());
  }

  @Test
  @WithMockUser
  void shouldReturnBadRequestWhenReviewIsBlank() throws Exception {
    ReviewRequestDto invalidRequest = new ReviewRequestDto("Title", " ", 5);
    performPost(invalidRequest).andExpect(status().isBadRequest());
  }

  @Test
  @WithMockUser
  void shouldReturnBadRequestWhenStarsIsZero() throws Exception {
    ReviewRequestDto invalidRequest = new ReviewRequestDto("Title", "Review", 0);
    performPost(invalidRequest).andExpect(status().isBadRequest());
  }

  @Test
  @WithMockUser
  void shouldReturnBadRequestWhenStarsIsAboveFive() throws Exception {
    ReviewRequestDto invalidRequest = new ReviewRequestDto("Title", "Review", 6);
    performPost(invalidRequest).andExpect(status().isBadRequest());
  }

  private org.springframework.test.web.servlet.ResultActions performPost(ReviewRequestDto request) throws Exception {
    return mockMvc.perform(post("/reviews")
        .param("requestedServiceId", "1")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request)));
  }
}
