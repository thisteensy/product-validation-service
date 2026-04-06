package com.productcatalog.application.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.productcatalog.ValidationBuilders;
import com.productcatalog.application.rest.mappers.ProductMapper;
import com.productcatalog.application.rest.params.ProductParams;
import com.productcatalog.domain.model.*;
import com.productcatalog.domain.ports.out.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
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

    @MockitoBean
    private ProductMapper productMapper;

    @BeforeEach
    void setUp() {
        when(productMapper.toProductFromProductParams(any()))
                .thenAnswer(invocation -> {
                    ProductParams params = invocation.getArgument(0);
                    return Product.builder()
                            .upc(params.getUpc())
                            .title(params.getTitle())
                            .artist(params.getArtist())
                            .label(params.getLabel())
                            .tracks(params.getTracks() == null ? null : params.getTracks().stream()
                                    .map(t -> Track.builder()
                                            .id(UUID.randomUUID())
                                            .isrc(t.getIsrc())
                                            .title(t.getTitle())
                                            .trackNumber(t.getTrackNumber())
                                            .audioFileUri(t.getAudioFileUri())
                                            .duration(t.getDuration())
                                            .explicit(t.isExplicit())
                                            .contributors(t.getContributors() == null ? null : t.getContributors().stream()
                                                    .map(c -> new ProductContributor(c.getName(), c.getRole()))
                                                    .toList())
                                            .ownershipSplits(t.getOwnershipSplits() == null ? null : t.getOwnershipSplits().stream()
                                                    .map(o -> new OwnershipSplit(o.getRightsHolder(), o.getPercentage()))
                                                    .toList())
                                            .status(TrackStatus.PENDING)
                                            .build())
                                    .toList())
                            .releaseDate(params.getReleaseDate())
                            .genre(params.getGenre())
                            .language(params.getLanguage())
                            .ownershipSplits(params.getOwnershipSplits() == null ? null : params.getOwnershipSplits().stream()
                                    .map(o -> new OwnershipSplit(o.getRightsHolder(), o.getPercentage()))
                                    .toList())
                            .artworkUri(params.getArtworkUri())
                            .dspTargets(params.getDspTargets())
                            .build();
                });
    }

    @Test
    void shouldAssignNewIdAndSubmittedStatusWhenCreatingProduct() throws Exception {
        ProductParams params = ValidationBuilders.validProductParams();
        when(productRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        mockMvc.perform(post("/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(params)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("SUBMITTED"))
                .andExpect(jsonPath("$.id").isNotEmpty());
    }

    @Test
    void shouldReturnAllProductsWhenNoFiltersProvided() throws Exception {
        Product product = ValidationBuilders.validProduct();
        when(productRepository.findByFilters(null, null, null, null))
                .thenReturn(List.of(product));

        mockMvc.perform(get("/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(product.getId().toString()));
    }

    @Test
    void shouldFilterByArtist() throws Exception {
        Product product = ValidationBuilders.validProduct();
        when(productRepository.findByFilters("Michael Jackson", null, null, null))
                .thenReturn(List.of(product));

        mockMvc.perform(get("/products").param("artist", "Michael Jackson"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(product.getId().toString()));
    }

    @Test
    void shouldFilterByLabel() throws Exception {
        Product product = ValidationBuilders.validProduct();
        when(productRepository.findByFilters(null, "Epic Records", null, null))
                .thenReturn(List.of(product));

        mockMvc.perform(get("/products").param("label", "Epic Records"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(product.getId().toString()));
    }

    @Test
    void shouldFilterByStatus() throws Exception {
        Product product = ValidationBuilders.validProduct();
        when(productRepository.findByFilters(null, null, null, "SUBMITTED"))
                .thenReturn(List.of(product));

        mockMvc.perform(get("/products").param("status", "SUBMITTED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(product.getId().toString()));
    }

    @Test
    void shouldFilterByCombinedParams() throws Exception {
        Product product = ValidationBuilders.validProduct();
        when(productRepository.findByFilters("Michael Jackson", "Epic Records", null, "SUBMITTED"))
                .thenReturn(List.of(product));

        mockMvc.perform(get("/products")
                        .param("artist", "Michael Jackson")
                        .param("label", "Epic Records")
                        .param("status", "SUBMITTED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(product.getId().toString()));
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
        ProductParams params = ValidationBuilders.validProductParams();
        when(productRepository.findById(product.getId()))
                .thenReturn(Optional.of(product));

        mockMvc.perform(put("/products/{id}", product.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(params)))
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
    void shouldRejectCreateWhenTitleIsBlank() throws Exception {
        ProductParams params = ValidationBuilders.validProductParams();
        params.setTitle("");

        mockMvc.perform(post("/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(params)))
                .andExpect(status().isBadRequest());

        verify(productRepository, never()).save(any());
    }

    @Test
    void shouldRejectCreateWhenIsrcIsInvalid() throws Exception {
        ProductParams params = ValidationBuilders.validProductParams();
        params.getTracks().get(0).setIsrc("FAKE12345");

        mockMvc.perform(post("/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(params)))
                .andExpect(status().isBadRequest());

        verify(productRepository, never()).save(any());
    }

    @Test
    void shouldRejectCreateWhenContributorsAreEmpty() throws Exception {
        ProductParams params = ValidationBuilders.validProductParams();
        params.getTracks().get(0).setContributors(List.of());

        mockMvc.perform(post("/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(params)))
                .andExpect(status().isBadRequest());

        verify(productRepository, never()).save(any());
    }

    @Test
    void shouldRejectCreateWhenOwnershipSplitsAreEmpty() throws Exception {
        ProductParams params = ValidationBuilders.validProductParams();
        params.setOwnershipSplits(List.of());

        mockMvc.perform(post("/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(params)))
                .andExpect(status().isBadRequest());

        verify(productRepository, never()).save(any());
    }

    @Test
    void shouldRejectCreateWhenDspTargetsAreEmpty() throws Exception {
        ProductParams params = ValidationBuilders.validProductParams();
        params.setDspTargets(List.of());

        mockMvc.perform(post("/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(params)))
                .andExpect(status().isBadRequest());

        verify(productRepository, never()).save(any());
    }

    @Test
    void shouldRejectCreateWhenTracksAreEmpty() throws Exception {
        ProductParams params = ValidationBuilders.validProductParams();
        params.setTracks(List.of());

        mockMvc.perform(post("/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(params)))
                .andExpect(status().isBadRequest());

        verify(productRepository, never()).save(any());
    }
}