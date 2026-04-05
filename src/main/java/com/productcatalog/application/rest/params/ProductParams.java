package com.productcatalog.application.rest.params;

import com.productcatalog.domain.model.ContributorRole;
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

    @NotBlank(message = "Title must not be blank")
    private String title;

    @NotNull(message = "Release date must be specified")
    private LocalDate releaseDate;

    @NotBlank(message = "Genre must not be blank")
    private String genre;

    @NotBlank(message = "Language must not be blank")
    private String language;

    @NotBlank(message = "Artwork URI must be present")
    private String artworkUri;

    @NotEmpty(message = "At least one DSP target must be specified")
    private List<String> dspTargets;

    @NotEmpty(message = "At least one ownership split must be specified")
    @Valid
    private List<OwnershipSplitParams> ownershipSplits;

    @NotEmpty(message = "At least one track must be specified")
    @Valid
    private List<TrackParams> tracks;

    @Getter
    @Setter
    @NoArgsConstructor
    public static class TrackParams {
        @NotBlank(message = "ISRC must not be blank")
        @Pattern(regexp = "[A-Za-z]{2}[A-Za-z0-9]{3}[0-9]{7}", message = "ISRC must match format: two letters, three alphanumeric, seven digits")
        private String isrc;

        @NotBlank(message = "Track title must not be blank")
        private String title;

        @Min(value = 1, message = "Track number must be at least 1")
        private int trackNumber;

        @NotBlank(message = "Audio file URI must be present")
        private String audioFileUri;

        @Min(value = 1, message = "Duration must be at least 1 second")
        private int duration;

        private boolean explicit;

        @NotEmpty(message = "At least one contributor must be specified")
        @Valid
        private List<ContributorParams> contributors;

        @NotEmpty(message = "At least one ownership split must be specified")
        @Valid
        private List<OwnershipSplitParams> ownershipSplits;
    }

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