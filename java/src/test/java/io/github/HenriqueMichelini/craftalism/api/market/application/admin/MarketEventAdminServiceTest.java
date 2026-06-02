package io.github.HenriqueMichelini.craftalism.api.market.application.admin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.HenriqueMichelini.craftalism.api.dto.MarketEventAdminCancelRequestDTO;
import io.github.HenriqueMichelini.craftalism.api.dto.MarketEventAdminCreateRequestDTO;
import io.github.HenriqueMichelini.craftalism.api.dto.MarketEventAdminResponseDTO;
import io.github.HenriqueMichelini.craftalism.api.dto.MarketEventAdminUpdateRequestDTO;
import io.github.HenriqueMichelini.craftalism.api.exceptions.MarketEventTemplateValidationException;
import io.github.HenriqueMichelini.craftalism.api.market.domain.event.MarketEventLifecycleService;
import io.github.HenriqueMichelini.craftalism.api.model.MarketEventEndReason;
import io.github.HenriqueMichelini.craftalism.api.model.MarketEventInstance;
import io.github.HenriqueMichelini.craftalism.api.model.MarketEventRarity;
import io.github.HenriqueMichelini.craftalism.api.model.MarketEventScope;
import io.github.HenriqueMichelini.craftalism.api.model.MarketEventSource;
import io.github.HenriqueMichelini.craftalism.api.model.MarketEventStatus;
import io.github.HenriqueMichelini.craftalism.api.model.MarketEventTemplate;
import io.github.HenriqueMichelini.craftalism.api.repository.MarketEventInstanceRepository;
import io.github.HenriqueMichelini.craftalism.api.repository.MarketEventTemplateRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MarketEventAdminServiceTest {

    @Mock
    private MarketEventInstanceRepository eventRepository;

    @Mock
    private MarketEventTemplateRepository templateRepository;

    @Mock
    private MarketEventLifecycleService lifecycleService;

    @Test
    void startEventCreatesManualAdminEventWithAuditMetadata() {
        when(templateRepository.findById("rare_customs_hold")).thenReturn(
            Optional.of(template())
        );
        when(lifecycleService.start(any())).thenAnswer(invocation -> {
            MarketEventInstance event = invocation.getArgument(0);
            event.setId(10L);
            event.setStatus(MarketEventStatus.ACTIVE);
            return event;
        });

        MarketEventAdminResponseDTO response = service().startEvent(
            new MarketEventAdminCreateRequestDTO(
                "rare_customs_hold",
                MarketEventScope.ITEM,
                null,
                "wheat",
                10_000,
                true,
                900L,
                "manual hold"
            ),
            "admin-user"
        );

        assertEquals(10L, response.id());
        assertEquals(MarketEventSource.ADMIN, response.source());
        assertEquals("admin-user", response.actor());
        assertTrue(response.auditMetadata().contains("manual hold"));
        assertEquals("wheat", response.selectedItemIds());
    }

    @Test
    void startEventRejectsEffectBelowTemplateRange() {
        when(templateRepository.findById("farming_bumper_crop")).thenReturn(
            Optional.of(downTemplate())
        );

        MarketEventTemplateValidationException exception = assertThrows(
            MarketEventTemplateValidationException.class,
            () ->
                service()
                    .startEvent(
                        new MarketEventAdminCreateRequestDTO(
                            "farming_bumper_crop",
                            MarketEventScope.CATEGORY,
                            "farming",
                            null,
                            1_000,
                            false,
                            900L,
                            "too strong"
                        ),
                        "admin-user"
                    )
        );

        assertEquals(
            "Effect basis points must be between 9200 and 9700 for template farming_bumper_crop.",
            exception.getMessage()
        );
        verify(lifecycleService, never()).start(any());
    }

    @Test
    void startEventRejectsEffectAboveTemplateRange() {
        when(templateRepository.findById("farming_bumper_crop")).thenReturn(
            Optional.of(downTemplate())
        );

        assertThrows(
            MarketEventTemplateValidationException.class,
            () ->
                service()
                    .startEvent(
                        new MarketEventAdminCreateRequestDTO(
                            "farming_bumper_crop",
                            MarketEventScope.CATEGORY,
                            "farming",
                            null,
                            10_000,
                            false,
                            900L,
                            "wrong direction"
                        ),
                        "admin-user"
                    )
        );
        verify(lifecycleService, never()).start(any());
    }

    @Test
    void startEventDefaultsOmittedEffectToTemplateMinimum() {
        when(templateRepository.findById("farming_bumper_crop")).thenReturn(
            Optional.of(downTemplate())
        );
        when(lifecycleService.start(any())).thenAnswer(invocation -> {
            MarketEventInstance event = invocation.getArgument(0);
            event.setId(12L);
            event.setStatus(MarketEventStatus.ACTIVE);
            return event;
        });

        MarketEventAdminResponseDTO response = service().startEvent(
            new MarketEventAdminCreateRequestDTO(
                "farming_bumper_crop",
                MarketEventScope.CATEGORY,
                "farming",
                null,
                null,
                false,
                900L,
                "default effect"
            ),
            "admin-user"
        );

        assertEquals(9_200, response.effectBasisPoints());
    }

    @Test
    void cancelEventEndsActiveEventWithCancelledReason() {
        MarketEventInstance event = activeEvent();
        when(eventRepository.findById(10L)).thenReturn(Optional.of(event));
        when(lifecycleService.end(
            any(),
            any(MarketEventStatus.class),
            any(MarketEventEndReason.class),
            any()
        )).thenAnswer(invocation -> {
            MarketEventInstance ended = invocation.getArgument(0);
            ended.setStatus(invocation.getArgument(1));
            ended.setEndReason(invocation.getArgument(2));
            return ended;
        });

        MarketEventAdminResponseDTO response = service().cancelEvent(
            10L,
            new MarketEventAdminCancelRequestDTO("bad setup"),
            "admin-user"
        );

        assertEquals(MarketEventStatus.CANCELLED, response.status());
        assertEquals(MarketEventEndReason.CANCELLED, response.endReason());
        assertEquals("admin-user", response.actor());
    }

    @Test
    void supersedeEndsCurrentActiveEventBeforeStartingReplacement() {
        MarketEventInstance active = activeEvent();
        when(lifecycleService.effectiveActiveEvent(any())).thenReturn(
            Optional.of(active)
        );
        when(templateRepository.findById("rare_customs_hold")).thenReturn(
            Optional.of(template())
        );
        when(lifecycleService.start(any())).thenAnswer(invocation -> {
            MarketEventInstance event = invocation.getArgument(0);
            event.setId(11L);
            event.setStatus(MarketEventStatus.ACTIVE);
            return event;
        });

        MarketEventAdminResponseDTO response = service().supersedeActiveEvent(
            new MarketEventAdminCreateRequestDTO(
                "rare_customs_hold",
                MarketEventScope.ITEM,
                null,
                "carrot",
                10_000,
                true,
                900L,
                "replacement"
            ),
            "admin-user"
        );

        assertEquals(11L, response.id());
        verify(lifecycleService).end(
            eq(active),
            eq(MarketEventStatus.SUPERSEDED),
            eq(MarketEventEndReason.SUPERSEDED),
            any()
        );
    }

    @Test
    void supersedeRejectsInvalidEffectBeforeEndingActiveEvent() {
        MarketEventInstance active = activeEvent();
        when(templateRepository.findById("farming_bumper_crop")).thenReturn(
            Optional.of(downTemplate())
        );

        assertThrows(
            MarketEventTemplateValidationException.class,
            () ->
                service()
                    .supersedeActiveEvent(
                        new MarketEventAdminCreateRequestDTO(
                            "farming_bumper_crop",
                            MarketEventScope.CATEGORY,
                            "farming",
                            null,
                            1_000,
                            false,
                            900L,
                            "invalid replacement"
                        ),
                        "admin-user"
                    )
        );

        verify(lifecycleService, never()).end(
            eq(active),
            eq(MarketEventStatus.SUPERSEDED),
            eq(MarketEventEndReason.SUPERSEDED),
            any()
        );
        verify(lifecycleService, never()).start(any());
    }

    @Test
    void updateEventRecordsBeforeAndAfterAuditAndBumpsEffectVersion() {
        MarketEventInstance event = activeEvent();
        when(eventRepository.findById(10L)).thenReturn(Optional.of(event));
        when(templateRepository.findById("rare_customs_hold")).thenReturn(
            Optional.of(template())
        );
        when(eventRepository.saveAndFlush(any())).thenAnswer(invocation ->
            invocation.getArgument(0)
        );

        MarketEventAdminResponseDTO response = service().updateEvent(
            10L,
            new MarketEventAdminUpdateRequestDTO(
                10_000,
                false,
                null,
                Instant.parse("2026-04-12T19:30:00Z"),
                "soften effect"
            ),
            "admin-user"
        );

        assertEquals(10_000, response.effectBasisPoints());
        assertEquals(2, response.effectVersion());
        assertTrue(response.auditMetadata().contains("before"));
        assertTrue(response.auditMetadata().contains("after"));
    }

    @Test
    void updateEventRejectsInvalidEffectBeforeMutatingEvent() {
        MarketEventInstance event = activeEvent();
        when(eventRepository.findById(10L)).thenReturn(Optional.of(event));
        when(templateRepository.findById("rare_customs_hold")).thenReturn(
            Optional.of(template())
        );

        assertThrows(
            MarketEventTemplateValidationException.class,
            () ->
                service()
                    .updateEvent(
                        10L,
                        new MarketEventAdminUpdateRequestDTO(
                            9_999,
                            false,
                            null,
                            Instant.parse("2026-04-12T19:30:00Z"),
                            "invalid effect"
                        ),
                        "admin-user"
                    )
        );

        assertEquals(10_000, event.getEffectBasisPoints());
        assertEquals(1, event.getEffectVersion());
        assertTrue(event.isBlocking());
        verify(eventRepository, never()).saveAndFlush(any());
    }

    @Test
    void listEventsReturnsInternalMetadata() {
        when(eventRepository.findAll()).thenReturn(List.of(activeEvent()));

        List<MarketEventAdminResponseDTO> events = service().listEvents();

        assertEquals(1, events.size());
        assertEquals("{\"action\":\"seed\"}", events.get(0).auditMetadata());
    }

    private MarketEventAdminService service() {
        return new MarketEventAdminService(
            eventRepository,
            templateRepository,
            lifecycleService
        );
    }

    private MarketEventTemplate template() {
        MarketEventTemplate template = new MarketEventTemplate();
        template.setTemplateId("rare_customs_hold");
        template.setRarity(MarketEventRarity.RARE);
        template.setScope(MarketEventScope.ITEM);
        template.setAutomaticWeight(0);
        template.setAutomaticEnabled(false);
        template.setBlockingAllowed(true);
        template.setMinDurationSeconds(900L);
        template.setMaxDurationSeconds(1_800L);
        template.setMinEffectBasisPoints(10_000);
        template.setMaxEffectBasisPoints(10_000);
        template.setEffectDirection("BLOCK");
        template.setCooldownSeconds(21_600L);
        template.setPlayerFacingName("Customs Hold");
        template.setPlayerFacingDescription("A specific good is temporarily held.");
        template.setBroadScopeHint("One item");
        template.setEligibleTargetMetadata("{\"manualOnly\":true}");
        template.setCreatedAt(Instant.parse("2026-01-01T00:00:00Z"));
        template.setUpdatedAt(Instant.parse("2026-01-01T00:00:00Z"));
        return template;
    }

    private MarketEventTemplate downTemplate() {
        MarketEventTemplate template = new MarketEventTemplate();
        template.setTemplateId("farming_bumper_crop");
        template.setRarity(MarketEventRarity.MEDIUM);
        template.setScope(MarketEventScope.CATEGORY);
        template.setAutomaticWeight(80);
        template.setAutomaticEnabled(true);
        template.setBlockingAllowed(false);
        template.setMinDurationSeconds(1_800L);
        template.setMaxDurationSeconds(3_600L);
        template.setMinEffectBasisPoints(9_200);
        template.setMaxEffectBasisPoints(9_700);
        template.setEffectDirection("DOWN");
        template.setCooldownSeconds(7_200L);
        template.setPlayerFacingName("Bumper Crop");
        template.setPlayerFacingDescription("Farms are overflowing.");
        template.setBroadScopeHint("Farming goods");
        template.setEligibleTargetMetadata("{\"categoryIds\":[\"farming\"]}");
        template.setCreatedAt(Instant.parse("2026-01-01T00:00:00Z"));
        template.setUpdatedAt(Instant.parse("2026-01-01T00:00:00Z"));
        return template;
    }

    private MarketEventInstance activeEvent() {
        MarketEventInstance event = new MarketEventInstance();
        event.setId(10L);
        event.setTemplateId("rare_customs_hold");
        event.setSource(MarketEventSource.ADMIN);
        event.setRarity(MarketEventRarity.RARE);
        event.setScope(MarketEventScope.ITEM);
        event.setSelectedItemIds("wheat");
        event.setEffectBasisPoints(10_000);
        event.setEffectVersion(1);
        event.setBlocking(true);
        event.setStartedAt(Instant.parse("2026-04-12T18:00:00Z"));
        event.setEndsAt(Instant.parse("2026-04-12T18:30:00Z"));
        event.setStatus(MarketEventStatus.ACTIVE);
        event.setActor("admin-user");
        event.setAuditMetadata("{\"action\":\"seed\"}");
        event.setCreatedAt(Instant.parse("2026-04-12T18:00:00Z"));
        event.setUpdatedAt(Instant.parse("2026-04-12T18:00:00Z"));
        return event;
    }
}
