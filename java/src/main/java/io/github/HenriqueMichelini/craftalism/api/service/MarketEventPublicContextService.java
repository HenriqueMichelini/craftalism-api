package io.github.HenriqueMichelini.craftalism.api.service;

import io.github.HenriqueMichelini.craftalism.api.dto.MarketActiveEventContextDTO;
import io.github.HenriqueMichelini.craftalism.api.model.MarketEventInstance;
import io.github.HenriqueMichelini.craftalism.api.model.MarketEventTemplate;
import io.github.HenriqueMichelini.craftalism.api.repository.MarketEventTemplateRepository;
import java.time.Instant;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MarketEventPublicContextService {

    private final MarketEventLifecycleService lifecycleService;
    private final MarketEventTemplateRepository templateRepository;

    public MarketEventPublicContextService(
        MarketEventLifecycleService lifecycleService,
        MarketEventTemplateRepository templateRepository
    ) {
        this.lifecycleService = lifecycleService;
        this.templateRepository = templateRepository;
    }

    @Transactional(readOnly = true)
    public Optional<MarketActiveEventContextDTO> activeContext(Instant now) {
        return lifecycleService
            .effectiveActiveEvent(now)
            .flatMap(this::toPublicContext);
    }

    private Optional<MarketActiveEventContextDTO> toPublicContext(
        MarketEventInstance event
    ) {
        return templateRepository
            .findById(event.getTemplateId())
            .map(this::toPublicContext);
    }

    private MarketActiveEventContextDTO toPublicContext(
        MarketEventTemplate template
    ) {
        return new MarketActiveEventContextDTO(
            template.getPlayerFacingName(),
            template.getPlayerFacingDescription(),
            template.getBroadScopeHint(),
            "active now"
        );
    }
}
