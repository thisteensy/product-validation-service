package com.productvalidation.application.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.productvalidation.ValidationBuilders;
import com.productvalidation.domain.model.Product;
import com.productvalidation.domain.model.ProductStatus;
import com.productvalidation.domain.ports.ProductRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ReviewController.class)
class ReviewControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ProductRepository productRepository;

    @Test
    void shouldReturnPendingReviewsWhenProductsNeedReview() throws Exception {
        Product product = ValidationBuilders.validProduct();
        when(productRepository.findByStatus(ProductStatus.NEEDS_REVIEW))
                .thenReturn(List.of(product));

        mockMvc.perform(get("/reviews/pending"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(product.getId().toString()));
    }

    @Test
    void shouldReturnEmptyListWhenNoPendingReviews() throws Exception {
        when(productRepository.findByStatus(ProductStatus.NEEDS_REVIEW))
                .thenReturn(List.of());

        mockMvc.perform(get("/reviews/pending"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void shouldUpdateStatusWhenDecisionIsValidated() throws Exception {
        UUID id = UUID.randomUUID();
        ReviewDecisionParams decision = new ReviewDecisionParams();
        decision.setStatus(ProductStatus.VALIDATED);
        decision.setNotes("Looks good");

        mockMvc.perform(patch("/reviews/{id}/decision", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(decision)))
                .andExpect(status().isOk());

        verify(productRepository).updateStatus(eq(id), eq(ProductStatus.VALIDATED), any());
    }

    @Test
    void shouldUpdateStatusWhenDecisionIsValidationFailed() throws Exception {
        UUID id = UUID.randomUUID();
        ReviewDecisionParams decision = new ReviewDecisionParams();
        decision.setStatus(ProductStatus.VALIDATION_FAILED);
        decision.setNotes("Missing ISRC");

        mockMvc.perform(patch("/reviews/{id}/decision", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(decision)))
                .andExpect(status().isOk());

        verify(productRepository).updateStatus(eq(id), eq(ProductStatus.VALIDATION_FAILED), any());
    }

    @Test
    void shouldRejectDecisionWhenStatusIsMissing() throws Exception {
        UUID id = UUID.randomUUID();
        ReviewDecisionParams decision = new ReviewDecisionParams();

        mockMvc.perform(patch("/reviews/{id}/decision", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(decision)))
                .andExpect(status().isBadRequest());

        verify(productRepository, never()).updateStatus(any(), any(), any());
    }

    @Test
    void shouldRejectDecisionWhenStatusIsNotReviewable() throws Exception {
        UUID id = UUID.randomUUID();
        ReviewDecisionParams decision = new ReviewDecisionParams();
        decision.setStatus(ProductStatus.PUBLISHED);

        mockMvc.perform(patch("/reviews/{id}/decision", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(decision)))
                .andExpect(status().isBadRequest());

        verify(productRepository, never()).updateStatus(any(), any(), any());
    }
}