package com.productcatalog.application.kafka.dtos;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class TrackEventDto {

    private Payload payload;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Payload {
        private String op;
        private TrackRow after;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class TrackRow {
        private String id;

        @JsonProperty("product_id")
        private String productId;

        private String isrc;
        private String title;

        @JsonProperty("track_number")
        private Integer trackNumber;

        @JsonProperty("audio_file_uri")
        private String audioFileUri;

        private Integer duration;
        private Integer explicit;
        private String contributors;

        @JsonProperty("ownership_splits")
        private String ownershipSplits;

        private String status;
    }
}