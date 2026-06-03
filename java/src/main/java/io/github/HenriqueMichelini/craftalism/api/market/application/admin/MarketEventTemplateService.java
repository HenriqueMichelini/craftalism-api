package io.github.HenriqueMichelini.craftalism.api.market.application.admin;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.HenriqueMichelini.craftalism.api.dto.MarketEventTemplateCreateRequestDTO;
import io.github.HenriqueMichelini.craftalism.api.dto.MarketEventTemplateResponseDTO;
import io.github.HenriqueMichelini.craftalism.api.dto.MarketEventTemplateUpdateRequestDTO;
import io.github.HenriqueMichelini.craftalism.api.exceptions.MarketEventTemplateValidationException;
import io.github.HenriqueMichelini.craftalism.api.market.domain.event.DefaultMarketEventTemplateCatalog;
import io.github.HenriqueMichelini.craftalism.api.market.domain.event.MarketEventTemplateBuilder;
import io.github.HenriqueMichelini.craftalism.api.model.MarketEventRarity;
import io.github.HenriqueMichelini.craftalism.api.model.MarketEventScope;
import io.github.HenriqueMichelini.craftalism.api.model.MarketEventTemplate;
import io.github.HenriqueMichelini.craftalism.api.repository.MarketEventTemplateRepository;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MarketEventTemplateService {

    private final MarketEventTemplateRepository templateRepository;
    private final ObjectMapper objectMapper;
    private final DefaultMarketEventTemplateCatalog defaultTemplateCatalog;

    public MarketEventTemplateService(
        MarketEventTemplateRepository templateRepository,
        ObjectMapper objectMapper,
        DefaultMarketEventTemplateCatalog defaultTemplateCatalog
    ) {
        this.templateRepository = templateRepository;
        this.objectMapper = objectMapper;
        this.defaultTemplateCatalog = defaultTemplateCatalog;
    }

    @Transactional
    public void seedInitialTemplatesIfEmpty() {
        if (templateRepository.count() > 0L) {
            return;
        }
        templateRepository.saveAll(defaultTemplateCatalog.templates(Instant.now()));
    }

    @Transactional(readOnly = true)
    public List<MarketEventTemplateResponseDTO> listTemplates() {
        return templateRepository
            .findAll()
            .stream()
            .sorted(Comparator.comparing(MarketEventTemplate::getTemplateId))
            .map(this::toResponse)
            .toList();
    }

    @Transactional
    public MarketEventTemplateResponseDTO createTemplate(
        MarketEventTemplateCreateRequestDTO request
    ) {
        String templateId = request.templateId().trim();
        if (templateRepository.existsById(templateId)) {
            throw validation("Market event template already exists.");
        }
        validate(request);

        Instant now = Instant.now();
        MarketEventTemplate template = new MarketEventTemplateBuilder()
            .templateId(templateId)
            .rarity(request.rarity())
            .scope(request.scope())
            .automaticWeight(request.automaticWeight())
            .automaticEnabled(request.automaticEnabled())
            .blockingAllowed(request.blockingAllowed())
            .minDurationSeconds(request.minDurationSeconds())
            .maxDurationSeconds(request.maxDurationSeconds())
            .minEffectBasisPoints(request.minEffectBasisPoints())
            .maxEffectBasisPoints(request.maxEffectBasisPoints())
            .effectDirection(request.effectDirection().trim().toUpperCase())
            .cooldownSeconds(request.cooldownSeconds())
            .playerFacingName(request.playerFacingName().trim())
            .playerFacingDescription(
                request.playerFacingDescription().trim()
            )
            .broadScopeHint(request.broadScopeHint().trim())
            .eligibleTargetMetadata(request.eligibleTargetMetadata().trim())
            .timestamps(now)
            .build();
        return toResponse(templateRepository.save(template));
    }

    @Transactional
    public MarketEventTemplateResponseDTO updateTemplate(
        String templateId,
        MarketEventTemplateUpdateRequestDTO request
    ) {
        MarketEventTemplate template = templateRepository
            .findById(templateId)
            .orElseThrow(() ->
                validation("Market event template does not exist.")
            );

        validate(request);

        template.setRarity(request.rarity());
        template.setScope(request.scope());
        template.setAutomaticWeight(request.automaticWeight());
        template.setAutomaticEnabled(request.automaticEnabled());
        template.setBlockingAllowed(request.blockingAllowed());
        template.setMinDurationSeconds(request.minDurationSeconds());
        template.setMaxDurationSeconds(request.maxDurationSeconds());
        template.setMinEffectBasisPoints(request.minEffectBasisPoints());
        template.setMaxEffectBasisPoints(request.maxEffectBasisPoints());
        template.setEffectDirection(request.effectDirection().trim().toUpperCase());
        template.setCooldownSeconds(request.cooldownSeconds());
        template.setPlayerFacingName(request.playerFacingName().trim());
        template.setPlayerFacingDescription(
            request.playerFacingDescription().trim()
        );
        template.setBroadScopeHint(request.broadScopeHint().trim());
        template.setEligibleTargetMetadata(
            request.eligibleTargetMetadata().trim()
        );
        template.setUpdatedAt(Instant.now());

        return toResponse(templateRepository.save(template));
    }

    private void validate(MarketEventTemplateCreateRequestDTO request) {
        validate(
            request.rarity(),
            request.scope(),
            request.automaticWeight(),
            request.automaticEnabled(),
            request.blockingAllowed(),
            request.minDurationSeconds(),
            request.maxDurationSeconds(),
            request.minEffectBasisPoints(),
            request.maxEffectBasisPoints(),
            request.effectDirection(),
            request.eligibleTargetMetadata()
        );
    }

    private void validate(MarketEventTemplateUpdateRequestDTO request) {
        validate(
            request.rarity(),
            request.scope(),
            request.automaticWeight(),
            request.automaticEnabled(),
            request.blockingAllowed(),
            request.minDurationSeconds(),
            request.maxDurationSeconds(),
            request.minEffectBasisPoints(),
            request.maxEffectBasisPoints(),
            request.effectDirection(),
            request.eligibleTargetMetadata()
        );
    }

    private void validate(
        MarketEventRarity rarity,
        MarketEventScope scope,
        int automaticWeight,
        boolean automaticEnabled,
        boolean blockingAllowed,
        long minDurationSeconds,
        long maxDurationSeconds,
        int minEffectBasisPoints,
        int maxEffectBasisPoints,
        String effectDirectionValue,
        String eligibleTargetMetadata
    ) {
        if (maxDurationSeconds < minDurationSeconds) {
            throw validation(
                "Maximum duration seconds must be greater than or equal to minimum duration seconds."
            );
        }
        if (maxEffectBasisPoints < minEffectBasisPoints) {
            throw validation(
                "Maximum effect basis points must be greater than or equal to minimum effect basis points."
            );
        }
        if (automaticEnabled && automaticWeight == 0) {
            throw validation(
                "Automatically enabled templates must have a positive automatic weight."
            );
        }
        if (rarity == MarketEventRarity.EXTRA_RARE && automaticEnabled) {
            throw validation(
                "Extra-rare templates cannot be enabled for automatic selection."
            );
        }

        String effectDirection = effectDirectionValue.trim().toUpperCase();
        switch (effectDirection) {
            case "UP" -> validateUpEffect(
                blockingAllowed,
                minEffectBasisPoints
            );
            case "DOWN" -> validateDownEffect(
                blockingAllowed,
                maxEffectBasisPoints
            );
            case "BLOCK" -> validateBlockEffect(
                rarity,
                scope,
                automaticEnabled,
                blockingAllowed,
                minEffectBasisPoints,
                maxEffectBasisPoints
            );
            default -> throw validation(
                "Effect direction must be UP, DOWN, or BLOCK."
            );
        }

        try {
            objectMapper.readTree(eligibleTargetMetadata.trim());
        } catch (JsonProcessingException ex) {
            throw validation("Eligible target metadata must be valid JSON.");
        }
    }

    private void validateUpEffect(
        boolean blockingAllowed,
        int minEffectBasisPoints
    ) {
        if (minEffectBasisPoints <= 10_000) {
            throw validation("UP effects must be greater than 10000 basis points.");
        }
        validateNonBlockingEffect(blockingAllowed);
    }

    private void validateDownEffect(
        boolean blockingAllowed,
        int maxEffectBasisPoints
    ) {
        if (maxEffectBasisPoints >= 10_000) {
            throw validation("DOWN effects must be less than 10000 basis points.");
        }
        validateNonBlockingEffect(blockingAllowed);
    }

    private void validateNonBlockingEffect(boolean blockingAllowed) {
        if (blockingAllowed) {
            throw validation("Price-effect templates cannot allow blocking.");
        }
    }

    private void validateBlockEffect(
        MarketEventRarity rarity,
        MarketEventScope scope,
        boolean automaticEnabled,
        boolean blockingAllowed,
        int minEffectBasisPoints,
        int maxEffectBasisPoints
    ) {
        if (
            !blockingAllowed ||
            scope != MarketEventScope.ITEM ||
            automaticEnabled ||
            rarity == MarketEventRarity.MEDIUM ||
            minEffectBasisPoints != 10_000 ||
            maxEffectBasisPoints != 10_000
        ) {
            throw validation(
                "BLOCK templates must be manual rare or extra-rare item templates with neutral price basis points."
            );
        }
    }

    private MarketEventTemplateResponseDTO toResponse(
        MarketEventTemplate template
    ) {
        return new MarketEventTemplateResponseDTO(
            template.getTemplateId(),
            template.getRarity(),
            template.getScope(),
            template.getAutomaticWeight(),
            template.isAutomaticEnabled(),
            template.isBlockingAllowed(),
            template.getMinDurationSeconds(),
            template.getMaxDurationSeconds(),
            template.getMinEffectBasisPoints(),
            template.getMaxEffectBasisPoints(),
            template.getEffectDirection(),
            template.getCooldownSeconds(),
            template.getPlayerFacingName(),
            template.getPlayerFacingDescription(),
            template.getBroadScopeHint(),
            template.getEligibleTargetMetadata(),
            template.getCreatedAt(),
            template.getUpdatedAt()
        );
    }

    private MarketEventTemplateValidationException validation(String message) {
        return new MarketEventTemplateValidationException(message);
    }
}
