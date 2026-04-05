package com.productcatalog.application.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.productcatalog.application.kafka.dtos.ProductEventDto;
import com.productcatalog.application.kafka.mappers.ProductEventMapper;
import com.productcatalog.domain.model.Product;
import com.productcatalog.domain.model.ProductStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProductEventMapperTest {

    private ProductEventMapper mapper;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        mapper = new ProductEventMapper(objectMapper);
    }

    private ProductEventDto.ProductRow validRow() {
        ProductEventDto.ProductRow row = new ProductEventDto.ProductRow();
        row.setId("00000000-0000-0000-0000-000000000001");
        row.setUpc(" 012-345-678-905 ");
        row.setTitle(" Thriller ");
        row.setReleaseDate(20000);
        row.setGenre(" Pop ");
        row.setLanguage(" EN ");
        row.setArtworkUri(" s3://artwork/thriller.jpg ");
        row.setDspTargets("[\"Spotify\",\"Apple_Music\"]");
        row.setOwnershipSplits("[{\"rightsHolder\":\" MJ Estate \",\"percentage\":100.0}]");
        row.setStatus("SUBMITTED");
        return row;
    }

    @Test
    void shouldMapProductCorrectlyWhenRowIsValid() {
        Product product = mapper.toProductFromProductRow(validRow());

        assertThat(product.getId().toString()).isEqualTo("00000000-0000-0000-0000-000000000001");
        assertThat(product.getStatus()).isEqualTo(ProductStatus.SUBMITTED);
    }

    @Test
    void shouldStripAndNormalizeUpcWhenUpcHasDashesAndSpaces() {
        Product product = mapper.toProductFromProductRow(validRow());
        assertThat(product.getUpc()).isEqualTo("012345678905");
    }

    @Test
    void shouldStripTitleWhenTitleHasLeadingAndTrailingSpaces() {
        Product product = mapper.toProductFromProductRow(validRow());
        assertThat(product.getTitle()).isEqualTo("Thriller");
    }

    @Test
    void shouldLowercaseLanguageWhenLanguageIsUppercase() {
        Product product = mapper.toProductFromProductRow(validRow());
        assertThat(product.getLanguage()).isEqualTo("en");
    }

    @Test
    void shouldLowercaseDspTargetsWhenTargetsAreUppercase() {
        Product product = mapper.toProductFromProductRow(validRow());
        assertThat(product.getDspTargets()).containsExactly("spotify", "apple_music");
    }

    @Test
    void shouldStripOwnershipSplitRightsHolderWhenRightsHolderHasSpaces() {
        Product product = mapper.toProductFromProductRow(validRow());
        assertThat(product.getOwnershipSplits()).hasSize(1);
        assertThat(product.getOwnershipSplits().get(0).getRightsHolder()).isEqualTo("MJ Estate");
    }

    @Test
    void shouldReturnNullUpcWhenUpcIsNull() {
        ProductEventDto.ProductRow row = validRow();
        row.setUpc(null);
        Product product = mapper.toProductFromProductRow(row);
        assertThat(product.getUpc()).isNull();
    }

    @Test
    void shouldReturnNullArtworkUriWhenArtworkUriIsNull() {
        ProductEventDto.ProductRow row = validRow();
        row.setArtworkUri(null);
        Product product = mapper.toProductFromProductRow(row);
        assertThat(product.getArtworkUri()).isNull();
    }

    @Test
    void shouldReturnNullOwnershipSplitsWhenOwnershipSplitsIsNull() {
        ProductEventDto.ProductRow row = validRow();
        row.setOwnershipSplits(null);
        Product product = mapper.toProductFromProductRow(row);
        assertThat(product.getOwnershipSplits()).isNull();
    }

    @Test
    void shouldReturnNullDspTargetsWhenDspTargetsIsNull() {
        ProductEventDto.ProductRow row = validRow();
        row.setDspTargets(null);
        Product product = mapper.toProductFromProductRow(row);
        assertThat(product.getDspTargets()).isNull();
    }

    @Test
    void shouldThrowRuntimeExceptionWhenDtoIsUnparseable() {
        assertThatThrownBy(() -> mapper.toDto("not valid json"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to deserialize product event");
    }

    @Test
    void shouldDeserializeDtoWhenMessageIsValidJson() {
        String message = """
                {
                  "payload": {
                    "op": "c",
                    "after": {
                      "id": "00000000-0000-0000-0000-000000000001",
                      "status": "SUBMITTED"
                    }
                  }
                }
                """;

        ProductEventDto dto = mapper.toDto(message);
        assertThat(dto.getPayload().getOp()).isEqualTo("c");
        assertThat(dto.getPayload().getAfter().getStatus()).isEqualTo("SUBMITTED");
    }
}