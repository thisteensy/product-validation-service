package com.productcatalog.application.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.productcatalog.application.kafka.dtos.TrackEventDto;
import com.productcatalog.application.kafka.mappers.TrackEventMapper;
import com.productcatalog.domain.model.ContributorRole;
import com.productcatalog.domain.model.Track;
import com.productcatalog.domain.model.TrackStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TrackEventMapperTest {

    private TrackEventMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new TrackEventMapper(new ObjectMapper());
    }

    private TrackEventDto.TrackRow validRow() {
        TrackEventDto.TrackRow row = new TrackEventDto.TrackRow();
        row.setId("00000000-0000-0000-0000-000000000002");
        row.setProductId("00000000-0000-0000-0000-000000000001");
        row.setIsrc(" usrc17607839 ");
        row.setTitle(" Thriller ");
        row.setTrackNumber(1);
        row.setAudioFileUri(" s3://audio/thriller.wav ");
        row.setDuration(358);
        row.setExplicit(0);
        row.setContributors("[{\"name\":\" Michael Jackson \",\"role\":\"MAIN_ARTIST\"}]");
        row.setOwnershipSplits("[{\"rightsHolder\":\" MJ Estate \",\"percentage\":100.0}]");
        row.setStatus("PENDING");
        return row;
    }

    @Test
    void shouldMapTrackCorrectly_whenRowIsValid() {
        Track track = mapper.toTrackFromTrackRow(validRow());

        assertThat(track.getId().toString()).isEqualTo("00000000-0000-0000-0000-000000000002");
        assertThat(track.getStatus()).isEqualTo(TrackStatus.PENDING);
    }

    @Test
    void shouldUppercaseIsrc_whenIsrcIsLowercase() {
        Track track = mapper.toTrackFromTrackRow(validRow());
        assertThat(track.getIsrc()).isEqualTo("USRC17607839");
    }

    @Test
    void shouldStripTitle_whenTitleHasLeadingAndTrailingSpaces() {
        Track track = mapper.toTrackFromTrackRow(validRow());
        assertThat(track.getTitle()).isEqualTo("Thriller");
    }

    @Test
    void shouldStripAudioFileUri_whenUriHasSpaces() {
        Track track = mapper.toTrackFromTrackRow(validRow());
        assertThat(track.getAudioFileUri()).isEqualTo("s3://audio/thriller.wav");
    }

    @Test
    void shouldSetExplicitFalse_whenExplicitIsZero() {
        Track track = mapper.toTrackFromTrackRow(validRow());
        assertThat(track.isExplicit()).isFalse();
    }

    @Test
    void shouldSetExplicitTrue_whenExplicitIsOne() {
        TrackEventDto.TrackRow row = validRow();
        row.setExplicit(1);
        Track track = mapper.toTrackFromTrackRow(row);
        assertThat(track.isExplicit()).isTrue();
    }

    @Test
    void shouldSetExplicitFalse_whenExplicitIsNull() {
        TrackEventDto.TrackRow row = validRow();
        row.setExplicit(null);
        Track track = mapper.toTrackFromTrackRow(row);
        assertThat(track.isExplicit()).isFalse();
    }

    @Test
    void shouldDeserializeContributors_whenContributorsJsonIsValid() {
        Track track = mapper.toTrackFromTrackRow(validRow());
        assertThat(track.getContributors()).hasSize(1);
        assertThat(track.getContributors().get(0).getName()).isEqualTo("Michael Jackson");
        assertThat(track.getContributors().get(0).getRole()).isEqualTo(ContributorRole.MAIN_ARTIST);
    }

    @Test
    void shouldStripContributorName_whenNameHasSpaces() {
        Track track = mapper.toTrackFromTrackRow(validRow());
        assertThat(track.getContributors().get(0).getName()).isEqualTo("Michael Jackson");
    }

    @Test
    void shouldDeserializeOwnershipSplits_whenOwnershipSplitsJsonIsValid() {
        Track track = mapper.toTrackFromTrackRow(validRow());
        assertThat(track.getOwnershipSplits()).hasSize(1);
        assertThat(track.getOwnershipSplits().get(0).getRightsHolder()).isEqualTo("MJ Estate");
        assertThat(track.getOwnershipSplits().get(0).getPercentage()).isEqualTo(100.0);
    }

    @Test
    void shouldReturnZeroTrackNumber_whenTrackNumberIsNull() {
        TrackEventDto.TrackRow row = validRow();
        row.setTrackNumber(null);
        Track track = mapper.toTrackFromTrackRow(row);
        assertThat(track.getTrackNumber()).isZero();
    }

    @Test
    void shouldReturnZeroDuration_whenDurationIsNull() {
        TrackEventDto.TrackRow row = validRow();
        row.setDuration(null);
        Track track = mapper.toTrackFromTrackRow(row);
        assertThat(track.getDuration()).isZero();
    }

    @Test
    void shouldReturnNullContributors_whenContributorsIsNull() {
        TrackEventDto.TrackRow row = validRow();
        row.setContributors(null);
        Track track = mapper.toTrackFromTrackRow(row);
        assertThat(track.getContributors()).isNull();
    }

    @Test
    void shouldReturnNullOwnershipSplits_whenOwnershipSplitsIsNull() {
        TrackEventDto.TrackRow row = validRow();
        row.setOwnershipSplits(null);
        Track track = mapper.toTrackFromTrackRow(row);
        assertThat(track.getOwnershipSplits()).isNull();
    }

    @Test
    void shouldThrowRuntimeException_whenDtoIsUnparseable() {
        assertThatThrownBy(() -> mapper.toDto("not valid json"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to deserialize track event");
    }

    @Test
    void shouldDeserializeDto_whenMessageIsValidJson() {
        String message = """
                {
                  "payload": {
                    "op": "c",
                    "after": {
                      "id": "00000000-0000-0000-0000-000000000002",
                      "product_id": "00000000-0000-0000-0000-000000000001",
                      "status": "PENDING"
                    }
                  }
                }
                """;

        TrackEventDto dto = mapper.toDto(message);
        assertThat(dto.getPayload().getOp()).isEqualTo("c");
        assertThat(dto.getPayload().getAfter().getStatus()).isEqualTo("PENDING");
    }
}