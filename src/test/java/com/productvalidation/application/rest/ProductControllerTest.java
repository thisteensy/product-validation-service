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

import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ProductController.class)
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ProductRepository productRepository;

    @Test
    void shouldAssignNewIdAndSubmittedStatusWhenCreatingProduct() throws Exception {
        Product product = ValidationBuilders.validProduct();
        when(productRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        mockMvc.perform(post("/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(product)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("SUBMITTED"))
                .andExpect(jsonPath("$.id").isNotEmpty());
    }

    @Test
    void shouldReturnProductWhenIdExists() throws Exception {
        Product product = ValidationBuilders.validProduct();
        when(productRepository.findById(product.getId()))
                .thenReturn(Optional.of(product));

        mockMvc.perform(get("/products/{id}", product.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(product.getId().toString()));
    }

    @Test
    void shouldReturn404WhenProductNotFound() throws Exception {
        UUID id = UUID.randomUUID();
        when(productRepository.findById(id)).thenReturn(Optional.empty());

        mockMvc.perform(get("/products/{id}", id))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldDeleteProductWhenIdExists() throws Exception {
        Product product = ValidationBuilders.validProduct();
        when(productRepository.findById(product.getId()))
                .thenReturn(Optional.of(product));

        mockMvc.perform(delete("/products/{id}", product.getId()))
                .andExpect(status().isNoContent());

        verify(productRepository).deleteById(product.getId());
    }

    @Test
    void shouldReturn404WhenDeletingNonexistentProduct() throws Exception {
        UUID id = UUID.randomUUID();
        when(productRepository.findById(id)).thenReturn(Optional.empty());

        mockMvc.perform(delete("/products/{id}", id))
                .andExpect(status().isNotFound());

        verify(productRepository, never()).deleteById(any());
    }

    @Test
    void shouldUpdateProductWhenIdExists() throws Exception {
        Product product = ValidationBuilders.validProduct();
        when(productRepository.findById(product.getId()))
                .thenReturn(Optional.of(product));

        mockMvc.perform(put("/products/{id}", product.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(product)))
                .andExpect(status().isOk());

        verify(productRepository).update(any());
    }

    @Test
    void shouldReturn404WhenUpdatingNonexistentProduct() throws Exception {
        UUID id = UUID.randomUUID();
        Product product = ValidationBuilders.validProduct();
        when(productRepository.findById(id)).thenReturn(Optional.empty());

        mockMvc.perform(put("/products/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(product)))
                .andExpect(status().isNotFound());

        verify(productRepository, never()).update(any());
    }

    @Test
    void shouldResubmitProductWhenStatusIsValidationFailed() throws Exception {
        Product product = ValidationBuilders.validProduct().toBuilder()
                .status(ProductStatus.VALIDATION_FAILED)
                .build();
        when(productRepository.findById(product.getId()))
                .thenReturn(Optional.of(product));

        mockMvc.perform(post("/products/{id}/resubmit", product.getId()))
                .andExpect(status().isOk());

        verify(productRepository).resubmit(product.getId());
    }

    @Test
    void shouldRejectResubmitWhenStatusIsNotValidationFailed() throws Exception {
        Product product = ValidationBuilders.validProduct().toBuilder()
                .status(ProductStatus.SUBMITTED)
                .build();
        when(productRepository.findById(product.getId()))
                .thenReturn(Optional.of(product));

        mockMvc.perform(post("/products/{id}/resubmit", product.getId()))
                .andExpect(status().isBadRequest());

        verify(productRepository, never()).resubmit(any());
    }

    @Test
    void shouldReturn404WhenResubmittingNonexistentProduct() throws Exception {
        UUID id = UUID.randomUUID();
        when(productRepository.findById(id)).thenReturn(Optional.empty());

        mockMvc.perform(post("/products/{id}/resubmit", id))
                .andExpect(status().isNotFound());

        verify(productRepository, never()).resubmit(any());
    }
}