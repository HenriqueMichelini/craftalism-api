package io.github.HenriqueMichelini.craftalism.api.market.application.admin;

import io.github.HenriqueMichelini.craftalism.api.dto.MarketEventAdminCancelRequestDTO;
import io.github.HenriqueMichelini.craftalism.api.dto.MarketEventAdminCreateRequestDTO;
import io.github.HenriqueMichelini.craftalism.api.dto.MarketEventAdminResponseDTO;
import io.github.HenriqueMichelini.craftalism.api.dto.MarketEventAdminUpdateRequestDTO;
import io.github.HenriqueMichelini.craftalism.api.exceptions.MarketEventTemplateValidationException;
import io.github.HenriqueMichelini.craftalism.api.market.domain.event.MarketEventLifecycleService;
import io.github.HenriqueMichelini.craftalism.api.model.MarketEventEndReason;
import io.github.HenriqueMichelini.craftalism.api.model.MarketEventInstance;
import io.github.HenriqueMichelini.craftalism.api.model.MarketEventScope;
import io.github.HenriqueMichelini.craftalism.api.model.MarketEventSource;
import io.github.HenriqueMichelini.craftalism.api.model.MarketEventStatus;
import io.github.HenriqueMichelini.craftalism.api.model.MarketEventTemplate;
import io.github.HenriqueMichelini.craftalism.api.repository.MarketEventInstanceRepository;
import io.github.HenriqueMichelini.craftalism.api.repository.MarketEventTemplateRepository;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class MarketEventAdminService {

    private final MarketEventInstanceRepository eventRepository;
    private final MarketEventTemplateRepository templateRepository;
    private final MarketEventLifecycleService lifecycleService;

    public MarketEventAdminService(
        MarketEventInstanceRepository eventRepository,
        MarketEventTemplateRepository templateRepository,
        MarketEventLifecycleService lifecycleService
    ) {
        this.eventRepository = eventRepository;
        this.templateRepository = templateRepository;
        this.lifecycleService = lifecycleService;
    }

    @Transactional
    public List<MarketEventAdminResponseDTO> listEvents() {
        reconcileExpiredEvents(Instant.now());
        return eventRepository
            .findAll()
            .stream()
            .sorted(
                Comparator
                    .comparing(MarketEventInstance::getCreatedAt)
                    .reversed()
            )
            .map(this::toResponse)
            .toList();
    }

    @Transactional
    public MarketEventAdminResponseDTO startEvent(
        MarketEventAdminCreateRequestDTO request,
        String actor
    ) {
        MarketEventTemplate template = template(request.templateId());
        Instant now = Instant.now();
        reconcileExpiredEvents(now);
        return startEvent(request, actor, template, now);
    }

    private MarketEventAdminResponseDTO startEvent(
        MarketEventAdminCreateRequestDTO request,
        String actor,
        MarketEventTemplate template,
        Instant now
    ) {
        MarketEventInstance event = eventFromRequest(request, template, actor, now);
        return toResponse(lifecycleService.start(event));
    }

    @Transactional
    public MarketEventAdminResponseDTO supersedeActiveEvent(
        MarketEventAdminCreateRequestDTO request,
        String actor
    ) {
        MarketEventTemplate template = template(request.templateId());
        Instant now = Instant.now();
        reconcileExpiredEvents(now);
        lifecycleService
            .effectiveActiveEvent(now)
            .ifPresent(active ->
                lifecycleService.end(
                    active,
                    MarketEventStatus.SUPERSEDED,
                    MarketEventEndReason.SUPERSEDED,
                    now
                )
            );
        return startEvent(request, actor, template, now);
    }

    @Transactional
    public MarketEventAdminResponseDTO updateEvent(
        Long id,
        MarketEventAdminUpdateRequestDTO request,
        String actor
    ) {
        Instant now = Instant.now();
        reconcileExpiredEvents(now);
        MarketEventInstance event = event(id);
        String before = auditValues(event);
        if (request.effectBasisPoints() != null) {
            event.setEffectBasisPoints(request.effectBasisPoints());
            event.setEffectVersion(event.getEffectVersion() + 1);
        }
        if (request.blocking() != null) {
            event.setBlocking(request.blocking());
        }
        if (request.endsAt() != null) {
            event.setEndsAt(request.endsAt());
        } else if (request.durationSeconds() != null) {
            event.setEndsAt(event.getStartedAt().plusSeconds(request.durationSeconds()));
        }
        event.setActor(actor);
        event.setUpdatedAt(now);
        event.setAuditMetadata(
            audit("update", actor, request.reason(), before, auditValues(event))
        );
        eventRepository.saveAndFlush(event);
        reconcileExpiredEvents(now);
        return toResponse(event(id));
    }

    @Transactional
    public MarketEventAdminResponseDTO cancelEvent(
        Long id,
        MarketEventAdminCancelRequestDTO request,
        String actor
    ) {
        Instant now = Instant.now();
        reconcileExpiredEvents(now);
        MarketEventInstance event = event(id);
        event.setActor(actor);
        event.setAuditMetadata(
            audit("cancel", actor, request.reason(), auditValues(event), null)
        );
        return toResponse(
            lifecycleService.end(
                event,
                MarketEventStatus.CANCELLED,
                MarketEventEndReason.CANCELLED,
                now
            )
        );
    }

    private void reconcileExpiredEvents(Instant now) {
        lifecycleService.expireElapsedActiveEvents(now);
    }

    private MarketEventInstance eventFromRequest(
        MarketEventAdminCreateRequestDTO request,
        MarketEventTemplate template,
        String actor,
        Instant now
    ) {
        MarketEventScope scope = request.scope() == null
            ? template.getScope()
            : request.scope();
        long durationSeconds = request.durationSeconds() == null
            ? template.getMinDurationSeconds()
            : request.durationSeconds();
        MarketEventInstance event = new MarketEventInstance();
        event.setTemplateId(template.getTemplateId());
        event.setSource(MarketEventSource.ADMIN);
        event.setRarity(template.getRarity());
        event.setScope(scope);
        event.setSelectedCategoryId(request.selectedCategoryId());
        event.setSelectedItemIds(request.selectedItemIds());
        event.setEffectBasisPoints(
            request.effectBasisPoints() == null
                ? template.getMinEffectBasisPoints()
                : request.effectBasisPoints()
        );
        event.setEffectVersion(1);
        event.setBlocking(
            request.blocking() == null
                ? template.isBlockingAllowed()
                : request.blocking()
        );
        event.setStartedAt(now);
        event.setEndsAt(now.plusSeconds(durationSeconds));
        event.setStatus(MarketEventStatus.SCHEDULED);
        event.setActor(actor);
        event.setAuditMetadata(
            audit("start", actor, request.reason(), null, auditValues(event))
        );
        event.setCreatedAt(now);
        event.setUpdatedAt(now);
        return event;
    }

    private MarketEventTemplate template(String templateId) {
        return templateRepository
            .findById(templateId)
            .orElseThrow(() ->
                new MarketEventTemplateValidationException(
                    "Market event template does not exist."
                )
            );
    }

    private MarketEventInstance event(Long id) {
        return eventRepository
            .findById(id)
            .orElseThrow(() ->
                new IllegalArgumentException("Market event does not exist.")
            );
    }

    private MarketEventAdminResponseDTO toResponse(MarketEventInstance event) {
        return new MarketEventAdminResponseDTO(
            event.getId(),
            event.getTemplateId(),
            event.getSource(),
            event.getRarity(),
            event.getScope(),
            event.getSelectedCategoryId(),
            event.getSelectedItemIds(),
            event.getEffectBasisPoints(),
            event.getEffectVersion(),
            event.isBlocking(),
            event.getStartedAt(),
            event.getEndsAt(),
            event.getStatus(),
            event.getEndReason(),
            event.getActor(),
            event.getAuditMetadata(),
            event.getCreatedAt(),
            event.getUpdatedAt()
        );
    }

    private String audit(
        String action,
        String actor,
        String reason,
        String before,
        String after
    ) {
        return "{\"action\":\"" + safe(action) +
        "\",\"actor\":\"" + safe(actor) +
        "\",\"reason\":\"" + safe(reason) +
        "\",\"before\":" + jsonStringOrNull(before) +
        ",\"after\":" + jsonStringOrNull(after) +
        "}";
    }

    private String auditValues(MarketEventInstance event) {
        return "status=" + event.getStatus() +
        ",effectBasisPoints=" + event.getEffectBasisPoints() +
        ",effectVersion=" + event.getEffectVersion() +
        ",blocking=" + event.isBlocking() +
        ",endsAt=" + event.getEndsAt();
    }

    private String jsonStringOrNull(String value) {
        return value == null ? "null" : "\"" + safe(value) + "\"";
    }

    private String safe(String value) {
        return value == null
            ? ""
            : value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
