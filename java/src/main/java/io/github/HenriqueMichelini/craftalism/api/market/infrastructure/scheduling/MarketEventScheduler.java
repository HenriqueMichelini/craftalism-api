package io.github.HenriqueMichelini.craftalism.api.market.infrastructure.scheduling;

import io.github.HenriqueMichelini.craftalism.api.market.domain.event.MarketEventLifecycleService;
import io.github.HenriqueMichelini.craftalism.api.market.domain.event.MarketEventSelectionPolicy;
import io.github.HenriqueMichelini.craftalism.api.market.infrastructure.codec.MarketEventTemplateTargetMetadataCodec;
import io.github.HenriqueMichelini.craftalism.api.model.MarketEventInstance;
import io.github.HenriqueMichelini.craftalism.api.model.MarketEventScope;
import io.github.HenriqueMichelini.craftalism.api.model.MarketEventSource;
import io.github.HenriqueMichelini.craftalism.api.model.MarketEventStatus;
import io.github.HenriqueMichelini.craftalism.api.model.MarketEventTemplate;
import io.github.HenriqueMichelini.craftalism.api.repository.MarketEventInstanceRepository;
import io.github.HenriqueMichelini.craftalism.api.repository.MarketEventSchedulerLockRepository;
import io.github.HenriqueMichelini.craftalism.api.repository.MarketEventTemplateRepository;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionOperations;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class MarketEventScheduler {

    private static final String LOCK_NAME = "market_event_scheduler";

    private final MarketEventTemplateTargetMetadataCodec targetMetadataCodec =
        new MarketEventTemplateTargetMetadataCodec();
    private final MarketEventSelectionPolicy selectionPolicy =
        new MarketEventSelectionPolicy();
    private final MarketEventTemplateRepository templateRepository;
    private final MarketEventInstanceRepository eventRepository;
    private final MarketEventSchedulerLockRepository lockRepository;
    private final MarketEventLifecycleService lifecycleService;
    private final Clock clock;
    private final Random random;
    private final TransactionOperations transactionOperations;
    private final String owner;
    private final boolean schedulerEnabled;
    private final boolean marketEnabled;
    private final long startChanceBasisPoints;
    private final Duration leaseDuration;
    private final Duration eventWindowInterval;
    private final Duration eventWindowJitter;
    private Instant nextWindowAt;

    @Autowired
    public MarketEventScheduler(
        MarketEventTemplateRepository templateRepository,
        MarketEventInstanceRepository eventRepository,
        MarketEventSchedulerLockRepository lockRepository,
        MarketEventLifecycleService lifecycleService,
        @Value("${craftalism.market-events.scheduler.enabled:true}") boolean schedulerEnabled,
        @Value("${craftalism.market.enabled:true}") boolean marketEnabled,
        @Value("${craftalism.market-events.scheduler.start-chance-basis-points:2500}") long startChanceBasisPoints,
        @Value("${craftalism.market-events.scheduler.lease-seconds:60}") long leaseSeconds,
        @Value("${craftalism.market-events.scheduler.window-interval-seconds:7200}") long eventWindowIntervalSeconds,
        @Value("${craftalism.market-events.scheduler.window-jitter-seconds:1800}") long eventWindowJitterSeconds,
        org.springframework.transaction.PlatformTransactionManager transactionManager
    ) {
        this(
            templateRepository,
            eventRepository,
            lockRepository,
            lifecycleService,
            Clock.systemUTC(),
            new SecureRandom(),
            schedulerEnabled,
            marketEnabled,
            startChanceBasisPoints,
            Duration.ofSeconds(leaseSeconds),
            Duration.ofSeconds(eventWindowIntervalSeconds),
            Duration.ofSeconds(eventWindowJitterSeconds),
            new TransactionTemplate(transactionManager),
            UUID.randomUUID().toString()
        );
    }

    MarketEventScheduler(
        MarketEventTemplateRepository templateRepository,
        MarketEventInstanceRepository eventRepository,
        MarketEventSchedulerLockRepository lockRepository,
        MarketEventLifecycleService lifecycleService,
        Clock clock,
        Random random,
        boolean schedulerEnabled,
        boolean marketEnabled,
        long startChanceBasisPoints,
        Duration leaseDuration,
        Duration eventWindowInterval,
        Duration eventWindowJitter,
        String owner
    ) {
        this(
            templateRepository,
            eventRepository,
            lockRepository,
            lifecycleService,
            clock,
            random,
            schedulerEnabled,
            marketEnabled,
            startChanceBasisPoints,
            leaseDuration,
            eventWindowInterval,
            eventWindowJitter,
            directTransactionOperations(),
            owner
        );
    }

    MarketEventScheduler(
        MarketEventTemplateRepository templateRepository,
        MarketEventInstanceRepository eventRepository,
        MarketEventSchedulerLockRepository lockRepository,
        MarketEventLifecycleService lifecycleService,
        Clock clock,
        Random random,
        boolean schedulerEnabled,
        boolean marketEnabled,
        long startChanceBasisPoints,
        Duration leaseDuration,
        Duration eventWindowInterval,
        Duration eventWindowJitter,
        TransactionOperations transactionOperations,
        String owner
    ) {
        this.templateRepository = templateRepository;
        this.eventRepository = eventRepository;
        this.lockRepository = lockRepository;
        this.lifecycleService = lifecycleService;
        this.clock = clock;
        this.random = random;
        this.transactionOperations = transactionOperations;
        this.schedulerEnabled = schedulerEnabled;
        this.marketEnabled = marketEnabled;
        this.startChanceBasisPoints = startChanceBasisPoints;
        this.leaseDuration = leaseDuration;
        this.eventWindowInterval = eventWindowInterval;
        this.eventWindowJitter = eventWindowJitter;
        this.owner = owner;
    }

    @Scheduled(
        fixedDelayString = "${craftalism.market-events.scheduler.check-delay-ms:300000}",
        initialDelayString = "${craftalism.market-events.scheduler.initial-delay-ms:300000}"
    )
    void scheduledRoll() {
        transactionOperations.execute(status -> rollWindow());
    }

    private static TransactionOperations directTransactionOperations() {
        return new TransactionOperations() {
            @Override
            public <T> T execute(TransactionCallback<T> action) {
                return action.doInTransaction(null);
            }
        };
    }

    @Transactional
    SchedulerDecision rollWindow() {
        Instant now = Instant.now(clock);
        if (!schedulerEnabled) {
            return SchedulerDecision.skipped("scheduler_disabled");
        }
        if (!marketEnabled) {
            return SchedulerDecision.skipped("market_closed");
        }
        if (!eventWindowDue(now)) {
            return SchedulerDecision.skipped("window_not_due");
        }
        if (!acquireLease(now)) {
            return SchedulerDecision.skipped("lease_unavailable");
        }

        try {
            lifecycleService.expireElapsedActiveEvents(now);
            if (lifecycleService.effectiveActiveEvent(now).isPresent()) {
                return SchedulerDecision.skipped("active_event_exists");
            }

            long startRoll = random.nextLong(10_000L);
            if (startRoll >= startChanceBasisPoints) {
                return SchedulerDecision.skipped("no_event_roll");
            }

            List<MarketEventTemplate> eligibleTemplates = eligibleTemplates(now);
            if (eligibleTemplates.isEmpty()) {
                return SchedulerDecision.skipped("no_eligible_templates");
            }

            MarketEventTemplate template = selectionPolicy.chooseWeightedTemplate(
                eligibleTemplates,
                random
            );
            MarketEventInstance event = buildEvent(template, now, startRoll);
            MarketEventInstance started = lifecycleService.start(event);
            return SchedulerDecision.started(
                started.getId(),
                template.getTemplateId()
            );
        } finally {
            nextWindowAt = nextWindowAfter(now);
        }
    }

    private boolean eventWindowDue(Instant now) {
        if (nextWindowAt == null) {
            nextWindowAt = now;
        }
        return !now.isBefore(nextWindowAt);
    }

    private Instant nextWindowAfter(Instant now) {
        return now.plus(eventWindowInterval).plus(randomJitter());
    }

    private Duration randomJitter() {
        long jitterSeconds = eventWindowJitter.getSeconds();
        if (jitterSeconds <= 0L) {
            return Duration.ZERO;
        }
        return Duration.ofSeconds(random.nextLong(jitterSeconds + 1L));
    }

    private boolean acquireLease(Instant now) {
        return (
            lockRepository.acquireExpiredLease(
                LOCK_NAME,
                owner,
                now,
                now.plus(leaseDuration)
            ) == 1
        );
    }

    private List<MarketEventTemplate> eligibleTemplates(Instant now) {
        List<MarketEventTemplate> candidates =
            selectionPolicy.automaticCandidates(templateRepository.findAll());
        if (candidates.isEmpty()) {
            return List.of();
        }

        long longestCooldownSeconds = selectionPolicy.longestCooldownSeconds(
            candidates
        );
        List<MarketEventInstance> recentEvents = eventRepository
            .findByCreatedAtAfter(now.minusSeconds(longestCooldownSeconds));
        return selectionPolicy.excludeCoolingDown(
            candidates,
            now,
            recentEvents,
            targetMetadataCodec::firstCategoryId,
            targetMetadataCodec::firstItemId
        );
    }

    private MarketEventInstance buildEvent(
        MarketEventTemplate template,
        Instant now,
        long startRoll
    ) {
        long durationSeconds = randomBetween(
            template.getMinDurationSeconds(),
            template.getMaxDurationSeconds()
        );
        int effectBasisPoints = (int) randomBetween(
            template.getMinEffectBasisPoints(),
            template.getMaxEffectBasisPoints()
        );

        MarketEventInstance event = new MarketEventInstance();
        event.setTemplateId(template.getTemplateId());
        event.setSource(MarketEventSource.SCHEDULER);
        event.setScope(template.getScope());
        event.setSelectedCategoryId(
            template.getScope() == MarketEventScope.CATEGORY
                ? targetMetadataCodec.firstCategoryId(template)
                : null
        );
        event.setSelectedItemIds(
            template.getScope() == MarketEventScope.ITEM ||
                template.getScope() == MarketEventScope.ITEM_SET
                ? targetMetadataCodec.firstItemId(template)
                : null
        );
        event.setEffectBasisPoints(effectBasisPoints);
        event.setEffectVersion(1);
        event.setBlocking(template.isBlockingAllowed());
        event.setStartedAt(now);
        event.setEndsAt(now.plusSeconds(durationSeconds));
        event.setStatus(MarketEventStatus.SCHEDULED);
        event.setAuditMetadata(
            "{\"startRoll\":" + startRoll +
            ",\"durationSeconds\":" + durationSeconds +
            ",\"effectBasisPoints\":" + effectBasisPoints +
            "}"
        );
        event.setCreatedAt(now);
        event.setUpdatedAt(now);
        return event;
    }

    private long randomBetween(long minimum, long maximum) {
        if (minimum == maximum) {
            return minimum;
        }
        return minimum + random.nextLong((maximum - minimum) + 1L);
    }

    record SchedulerDecision(
        boolean started,
        Long eventId,
        String templateId,
        String reason
    ) {
        static SchedulerDecision started(Long eventId, String templateId) {
            return new SchedulerDecision(true, eventId, templateId, null);
        }

        static SchedulerDecision skipped(String reason) {
            return new SchedulerDecision(false, null, null, reason);
        }
    }
}
