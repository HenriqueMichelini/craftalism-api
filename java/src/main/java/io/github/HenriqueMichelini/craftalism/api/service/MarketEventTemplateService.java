package io.github.HenriqueMichelini.craftalism.api.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.HenriqueMichelini.craftalism.api.dto.MarketEventTemplateCreateRequestDTO;
import io.github.HenriqueMichelini.craftalism.api.dto.MarketEventTemplateResponseDTO;
import io.github.HenriqueMichelini.craftalism.api.exceptions.MarketEventTemplateValidationException;
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

    public MarketEventTemplateService(
        MarketEventTemplateRepository templateRepository,
        ObjectMapper objectMapper
    ) {
        this.templateRepository = templateRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public void seedInitialTemplatesIfEmpty() {
        if (templateRepository.count() > 0L) {
            return;
        }
        templateRepository.saveAll(initialTemplates(Instant.now()));
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

    List<MarketEventTemplate> initialTemplates(Instant now) {
        return List.of(
            new MarketEventTemplateBuilder()
                .templateId("farming_bumper_crop")
                .rarity(MarketEventRarity.MEDIUM)
                .scope(MarketEventScope.CATEGORY)
                .automaticWeight(80)
                .automaticEnabled(true)
                .blockingAllowed(false)
                .minDurationSeconds(1_800L)
                .maxDurationSeconds(3_600L)
                .minEffectBasisPoints(9_200)
                .maxEffectBasisPoints(9_700)
                .effectDirection("DOWN")
                .cooldownSeconds(7_200L)
                .playerFacingName("Bumper Crop")
                .playerFacingDescription(
                    "Farms are overflowing, softening prices for a while."
                )
                .broadScopeHint("Farming goods")
                .eligibleTargetMetadata("{\"categoryIds\":[\"farming\"]}")
                .timestamps(now)
                .build(),
            new MarketEventTemplateBuilder()
                .templateId("mining_tool_shortage")
                .rarity(MarketEventRarity.MEDIUM)
                .scope(MarketEventScope.CATEGORY)
                .automaticWeight(70)
                .automaticEnabled(true)
                .blockingAllowed(false)
                .minDurationSeconds(1_800L)
                .maxDurationSeconds(3_600L)
                .minEffectBasisPoints(10_300)
                .maxEffectBasisPoints(10_800)
                .effectDirection("UP")
                .cooldownSeconds(7_200L)
                .playerFacingName("Tool Shortage")
                .playerFacingDescription(
                    "Mining supplies are tight, lifting mineral prices."
                )
                .broadScopeHint("Mining goods")
                .eligibleTargetMetadata("{\"categoryIds\":[\"minerals\"]}")
                .timestamps(now)
                .build(),
            new MarketEventTemplateBuilder()
                .templateId("rare_customs_hold")
                .rarity(MarketEventRarity.RARE)
                .scope(MarketEventScope.ITEM)
                .automaticWeight(0)
                .automaticEnabled(false)
                .blockingAllowed(true)
                .minDurationSeconds(900L)
                .maxDurationSeconds(1_800L)
                .minEffectBasisPoints(10_000)
                .maxEffectBasisPoints(10_000)
                .effectDirection("BLOCK")
                .cooldownSeconds(21_600L)
                .playerFacingName("Customs Hold")
                .playerFacingDescription(
                    "A specific good is temporarily held from trade."
                )
                .broadScopeHint("One item")
                .eligibleTargetMetadata("{\"manualOnly\":true}")
                .timestamps(now)
                .build(),
            new MarketEventTemplateBuilder()
                .templateId("extra_rare_market_alarm")
                .rarity(MarketEventRarity.EXTRA_RARE)
                .scope(MarketEventScope.MARKET_WIDE)
                .automaticWeight(0)
                .automaticEnabled(false)
                .blockingAllowed(false)
                .minDurationSeconds(600L)
                .maxDurationSeconds(1_200L)
                .minEffectBasisPoints(11_000)
                .maxEffectBasisPoints(11_500)
                .effectDirection("UP")
                .cooldownSeconds(86_400L)
                .playerFacingName("Market Alarm")
                .playerFacingDescription(
                    "Market reviews are slowing supply, lifting prices across the board."
                )
                .broadScopeHint("World market")
                .eligibleTargetMetadata(
                    "{\"manualOnly\":true,\"automaticDisabled\":true}"
                )
                .timestamps(now)
                .build()
        );
    }

    private void validate(MarketEventTemplateCreateRequestDTO request) {
        if (request.maxDurationSeconds() < request.minDurationSeconds()) {
            throw validation(
                "Maximum duration seconds must be greater than or equal to minimum duration seconds."
            );
        }
        if (request.maxEffectBasisPoints() < request.minEffectBasisPoints()) {
            throw validation(
                "Maximum effect basis points must be greater than or equal to minimum effect basis points."
            );
        }
        if (request.automaticEnabled() && request.automaticWeight() == 0) {
            throw validation(
                "Automatically enabled templates must have a positive automatic weight."
            );
        }
        if (
            request.rarity() == MarketEventRarity.EXTRA_RARE &&
            request.automaticEnabled()
        ) {
            throw validation(
                "Extra-rare templates cannot be enabled for automatic selection."
            );
        }

        String effectDirection = request.effectDirection().trim().toUpperCase();
        switch (effectDirection) {
            case "UP" -> validateUpEffect(request);
            case "DOWN" -> validateDownEffect(request);
            case "BLOCK" -> validateBlockEffect(request);
            default -> throw validation(
                "Effect direction must be UP, DOWN, or BLOCK."
            );
        }

        try {
            objectMapper.readTree(request.eligibleTargetMetadata().trim());
        } catch (JsonProcessingException ex) {
            throw validation("Eligible target metadata must be valid JSON.");
        }
    }

    private void validateUpEffect(MarketEventTemplateCreateRequestDTO request) {
        if (request.minEffectBasisPoints() <= 10_000) {
            throw validation("UP effects must be greater than 10000 basis points.");
        }
        validateNonBlockingEffect(request);
    }

    private void validateDownEffect(
        MarketEventTemplateCreateRequestDTO request
    ) {
        if (request.maxEffectBasisPoints() >= 10_000) {
            throw validation("DOWN effects must be less than 10000 basis points.");
        }
        validateNonBlockingEffect(request);
    }

    private void validateNonBlockingEffect(
        MarketEventTemplateCreateRequestDTO request
    ) {
        if (request.blockingAllowed()) {
            throw validation("Price-effect templates cannot allow blocking.");
        }
    }

    private void validateBlockEffect(
        MarketEventTemplateCreateRequestDTO request
    ) {
        if (
            !request.blockingAllowed() ||
            request.scope() != MarketEventScope.ITEM ||
            request.automaticEnabled() ||
            request.rarity() == MarketEventRarity.MEDIUM ||
            request.minEffectBasisPoints() != 10_000 ||
            request.maxEffectBasisPoints() != 10_000
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
