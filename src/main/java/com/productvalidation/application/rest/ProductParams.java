package com.productvalidation.application.rest;

import com.productvalidation.domain.model.ContributorRole;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class ProductParams {

    @NotBlank(message = "UPC must not be blank")
    private String upc;

    @NotBlank(message = "ISRC must not be blank")
    @Pattern(regexp = "[A-Za-z]{2}[A-Za-z0-9]{3}[0-9]{7}", message = "ISRC must match format: two letters, three alphanumeric, seven digits")
    private String isrc;

    @NotBlank(message = "Title must not be blank")
    private String title;

    @NotNull(message = "Release date must be specified")
    private LocalDate releaseDate;

    @NotBlank(message = "Genre must not be blank")
    private String genre;

    private boolean explicit;

    @NotBlank(message = "Language must not be blank")
    private String language;

    @NotBlank(message = "Audio file URI must be present")
    private String audioFileUri;

    @NotBlank(message = "Artwork URI must be present")
    private String artworkUri;

    @NotEmpty(message = "At least one DSP target must be specified")
    private List<String> dspTargets;

    @NotEmpty(message = "At least one contributor must be specified")
    @Valid
    private List<ContributorParams> contributors;

    @NotEmpty(message = "At least one ownership split must be specified")
    @Valid
    private List<OwnershipSplitParams> ownershipSplits;

    @Getter
    @Setter
    @NoArgsConstructor
    public static class ContributorParams {
        @NotBlank(message = "Contributor name must not be blank")
        private String name;

        @NotNull(message = "Contributor role must not be null")
        private ContributorRole role;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static class OwnershipSplitParams {
        @NotBlank(message = "Rights holder must not be blank")
        private String rightsHolder;

        @NotNull(message = "Ownership percentage must not be null")
        @DecimalMin(value = "0.0", message = "Ownership percentage must be positive")
        @DecimalMax(value = "100.0", message = "Ownership percentage must not exceed 100")
        private Double percentage;
    }
}