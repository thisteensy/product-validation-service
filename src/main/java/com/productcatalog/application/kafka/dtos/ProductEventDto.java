package com.productcatalog.application.kafka.dtos;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ProductEventDto {

    @JsonProperty("payload")
    private Payload payload;

    @Data
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Payload {

        @JsonProperty("before")
        private ProductRow before;

        @JsonProperty("after")
        private ProductRow after;

        @JsonProperty("op")
        private String op;
    }

    @Data
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ProductRow {

        @JsonProperty("id")
        private String id;

        @JsonProperty("upc")
        private String upc;

        @JsonProperty("isrc")
        private String isrc;

        @JsonProperty("title")
        private String title;

        @JsonProperty("contributors")
        private String contributors;

        @JsonProperty("release_date")
        private Integer releaseDate;

        @JsonProperty("genre")
        private String genre;

        @JsonProperty("explicit")
        private Integer explicit;

        @JsonProperty("language")
        private String language;

        @JsonProperty("ownership_splits")
        private String ownershipSplits;

        @JsonProperty("audio_file_uri")
        private String audioFileUri;

        @JsonProperty("artwork_uri")
        private String artworkUri;

        @JsonProperty("dsp_targets")
        private String dspTargets;

        @JsonProperty("status")
        private String status;
    }
}