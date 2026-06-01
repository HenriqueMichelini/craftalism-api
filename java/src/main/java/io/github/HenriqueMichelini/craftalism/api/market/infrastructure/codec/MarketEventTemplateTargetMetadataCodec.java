package io.github.HenriqueMichelini.craftalism.api.market.infrastructure.codec;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.HenriqueMichelini.craftalism.api.model.MarketEventTemplate;

public final class MarketEventTemplateTargetMetadataCodec {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    public MarketEventTemplateTargetMetadataCodec() {}

    public String firstCategoryId(MarketEventTemplate template) {
        return firstValue(template, "categoryIds");
    }

    public String firstItemId(MarketEventTemplate template) {
        return firstValue(template, "itemIds");
    }

    private String firstValue(MarketEventTemplate template, String key) {
        String metadata = template.getEligibleTargetMetadata();
        if (metadata == null || metadata.isBlank()) {
            return null;
        }

        try {
            JsonNode values = OBJECT_MAPPER.readTree(metadata).path(key);
            if (!values.isArray() || values.isEmpty()) {
                return null;
            }
            JsonNode firstValue = values.get(0);
            return firstValue.isTextual() ? firstValue.textValue() : null;
        } catch (JsonProcessingException exception) {
            return null;
        }
    }
}
