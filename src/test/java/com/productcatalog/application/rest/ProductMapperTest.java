package com.productcatalog.application.rest;

import com.productcatalog.application.rest.mappers.ProductMapper;
import com.productcatalog.application.rest.params.ProductParams;
import com.productcatalog.domain.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.productcatalog.ValidationBuilders.validProductParams;
import static org.assertj.core.api.Assertions.assertThat;

class ProductMapperTest {

    private ProductMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new ProductMapper();
    }

    @Test
    void shouldMapProductCorrectlyWhenParamsAreValid() {
        Product product = mapper.toProductFromProductParams(validProductParams());

        assertThat(product.getTitle()).isEqualTo("Thriller");
        assertThat(product.getStatus()).isNull();
    }

    @Test
    void shouldStripAndNormalizeUpcWhenUpcHasDashesAndSpaces() {
        ProductParams params = validProductParams();
        params.setUpc(" 012-345-678-905 ");

        Product product = mapper.toProductFromProductParams(params);

        assertThat(product.getUpc()).isEqualTo("012345678905");
    }

    @Test
    void shouldStripTitleWhenTitleHasLeadingAndTrailingSpaces() {
        ProductParams params = validProductParams();
        params.setTitle(" Thriller ");

        Product product = mapper.toProductFromProductParams(params);

        assertThat(product.getTitle()).isEqualTo("Thriller");
    }

    @Test
    void shouldLowercaseLanguageWhenLanguageIsUppercase() {
        ProductParams params = validProductParams();
        params.setLanguage("EN");

        Product product = mapper.toProductFromProductParams(params);

        assertThat(product.getLanguage()).isEqualTo("en");
    }

    @Test
    void shouldLowercaseDspTargetsWhenTargetsAreUppercase() {
        ProductParams params = validProductParams();
        params.setDspTargets(java.util.List.of("Spotify", "Apple_Music"));

        Product product = mapper.toProductFromProductParams(params);

        assertThat(product.getDspTargets()).containsExactly("spotify", "apple_music");
    }

    @Test
    void shouldStripOwnershipSplitRightsHolderWhenRightsHolderHasSpaces() {
        ProductParams params = validProductParams();
        ProductParams.OwnershipSplitParams split = new ProductParams.OwnershipSplitParams();
        split.setRightsHolder(" MJ Estate ");
        split.setPercentage(100.0);
        params.setOwnershipSplits(java.util.List.of(split));

        Product product = mapper.toProductFromProductParams(params);

        assertThat(product.getOwnershipSplits().get(0).getRightsHolder()).isEqualTo("MJ Estate");
    }

    @Test
    void shouldReturnNullUpcWhenUpcIsNull() {
        ProductParams params = validProductParams();
        params.setUpc(null);

        Product product = mapper.toProductFromProductParams(params);

        assertThat(product.getUpc()).isNull();
    }

    @Test
    void shouldReturnNullTracksWhenTracksIsNull() {
        ProductParams params = validProductParams();
        params.setTracks(null);

        Product product = mapper.toProductFromProductParams(params);

        assertThat(product.getTracks()).isNull();
    }

    @Test
    void shouldMapTrackCorrectlyWhenTrackParamsAreValid() {
        Product product = mapper.toProductFromProductParams(validProductParams());

        Track track = product.getTracks().get(0);
        assertThat(track.getTitle()).isEqualTo("Thriller");
        assertThat(track.getStatus()).isEqualTo(TrackStatus.PENDING);
        assertThat(track.getId()).isNotNull();
    }

    @Test
    void shouldUppercaseIsrcWhenIsrcIsLowercase() {
        ProductParams params = validProductParams();
        params.getTracks().get(0).setIsrc("usrc17607839");

        Product product = mapper.toProductFromProductParams(params);

        assertThat(product.getTracks().get(0).getIsrc()).isEqualTo("USRC17607839");
    }

    @Test
    void shouldSetExplicitOnTrackWhenExplicitIsTrue() {
        ProductParams params = validProductParams();
        params.getTracks().get(0).setExplicit(true);

        Product product = mapper.toProductFromProductParams(params);

        assertThat(product.getTracks().get(0).isExplicit()).isTrue();
    }

    @Test
    void shouldStripContributorNameWhenNameHasSpaces() {
        ProductParams params = validProductParams();
        ProductParams.ContributorParams contributor = new ProductParams.ContributorParams();
        contributor.setName(" Michael Jackson ");
        contributor.setRole(ContributorRole.MAIN_ARTIST);
        params.getTracks().get(0).setContributors(java.util.List.of(contributor));

        Product product = mapper.toProductFromProductParams(params);

        assertThat(product.getTracks().get(0).getContributors().get(0).getName())
                .isEqualTo("Michael Jackson");
    }

    @Test
    void shouldReturnNullContributorsWhenContributorsIsNull() {
        ProductParams params = validProductParams();
        params.getTracks().get(0).setContributors(null);

        Product product = mapper.toProductFromProductParams(params);

        assertThat(product.getTracks().get(0).getContributors()).isNull();
    }
}