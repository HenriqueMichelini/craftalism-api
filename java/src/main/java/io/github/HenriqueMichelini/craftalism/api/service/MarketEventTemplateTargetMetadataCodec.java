package io.github.HenriqueMichelini.craftalism.api.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.HenriqueMichelini.craftalism.api.model.MarketEventTemplate;

final class MarketEventTemplateTargetMetadataCodec {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    String firstCategoryId(MarketEventTemplate template) {
        return firstValue(template, "categoryIds");
    }

    String firstItemId(MarketEventTemplate template) {
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
