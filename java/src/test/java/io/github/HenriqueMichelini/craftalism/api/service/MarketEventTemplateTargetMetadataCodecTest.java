package io.github.HenriqueMichelini.craftalism.api.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import io.github.HenriqueMichelini.craftalism.api.model.MarketEventTemplate;
import org.junit.jupiter.api.Test;

class MarketEventTemplateTargetMetadataCodecTest {

    private final MarketEventTemplateTargetMetadataCodec codec =
        new MarketEventTemplateTargetMetadataCodec();

    @Test
    void firstCategoryId_returnsFirstConfiguredCategory() {
        assertEquals(
            "farming",
            codec.firstCategoryId(template("{\"categoryIds\":[\"farming\",\"minerals\"]}"))
        );
    }

    @Test
    void firstItemId_returnsFirstConfiguredItem() {
        assertEquals(
            "wheat",
            codec.firstItemId(template("{\"itemIds\":[\"wheat\",\"iron_ore\"]}"))
        );
    }

    @Test
    void firstCategoryId_handlesWhitespaceFormattedJson() {
        assertEquals(
            "farming",
            codec.firstCategoryId(
                template(
                    """
                    {
                      "categoryIds": [ "farming", "minerals" ]
                    }
                    """
                )
            )
        );
    }

    @Test
    void firstCategoryId_returnsNullWhenKeyIsMissing() {
        assertNull(codec.firstCategoryId(template("{\"itemIds\":[\"wheat\"]}")));
    }

    @Test
    void firstCategoryId_returnsNullWhenValuesAreEmpty() {
        assertNull(codec.firstCategoryId(template("{\"categoryIds\":[]}")));
    }

    @Test
    void firstCategoryId_returnsNullWhenMetadataIsMalformed() {
        assertNull(codec.firstCategoryId(template("{\"categoryIds\":[\"farming\"")));
    }

    @Test
    void firstCategoryId_returnsNullWhenMetadataIsMissing() {
        assertNull(codec.firstCategoryId(template(null)));
    }

    private MarketEventTemplate template(String eligibleTargetMetadata) {
        MarketEventTemplate template = new MarketEventTemplate();
        template.setEligibleTargetMetadata(eligibleTargetMetadata);
        return template;
    }
}
