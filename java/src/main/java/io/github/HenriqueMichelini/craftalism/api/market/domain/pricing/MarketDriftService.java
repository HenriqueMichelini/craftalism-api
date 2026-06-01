package io.github.HenriqueMichelini.craftalism.api.market.domain.pricing;

import io.github.HenriqueMichelini.craftalism.api.model.MarketItem;
import java.time.Duration;
import java.time.Instant;

public final class MarketDriftService {

    private static final long EVALUATION_INTERVAL_SECONDS = 3_600L;
    private static final long NEUTRAL_MULTIPLIER_BASIS_POINTS = 10_000L;
    private static final long MAX_ABSOLUTE_DRIFT_BASIS_POINTS = 600L;
    private static final long MAX_STEP_BASIS_POINTS = 90L;

    public boolean shouldAttemptDriftEvaluation(MarketItem item, Instant now) {
        if (item.getDriftEvaluatedAt() == null) {
            return false;
        }
        if (!now.isAfter(item.getDriftEvaluatedAt())) {
            return false;
        }
        long ticks =
            Duration.between(item.getDriftEvaluatedAt(), now).getSeconds() /
            EVALUATION_INTERVAL_SECONDS;
        return ticks > 0L;
    }

    public boolean evaluateDrift(MarketItem item, Instant now) {
        if (!shouldAttemptDriftEvaluation(item, now)) {
            return false;
        }

        long ticks =
            Duration.between(item.getDriftEvaluatedAt(), now).getSeconds() /
            EVALUATION_INTERVAL_SECONDS;
        long multiplier = item.getDriftMultiplierBasisPoints() > 0L
            ? item.getDriftMultiplierBasisPoints()
            : NEUTRAL_MULTIPLIER_BASIS_POINTS;
        long revision = item.getDriftRevision();
        for (long index = 0L; index < ticks; index++) {
            long randomStep = deterministicStep(item.getItemId(), revision + 1L);
            long meanReversionStep =
                Math.round((NEUTRAL_MULTIPLIER_BASIS_POINTS - multiplier) * 0.25D);
            multiplier = clamp(
                multiplier + randomStep + meanReversionStep,
                NEUTRAL_MULTIPLIER_BASIS_POINTS - MAX_ABSOLUTE_DRIFT_BASIS_POINTS,
                NEUTRAL_MULTIPLIER_BASIS_POINTS + MAX_ABSOLUTE_DRIFT_BASIS_POINTS
            );
            revision++;
        }

        item.setDriftMultiplierBasisPoints(multiplier);
        item.setDriftRevision(revision);
        item.setDriftEvaluatedAt(
            item
                .getDriftEvaluatedAt()
                .plusSeconds(ticks * EVALUATION_INTERVAL_SECONDS)
        );
        return true;
    }

    private long deterministicStep(String itemId, long revision) {
        long hash = 1125899906842597L;
        String input = itemId + ':' + revision;
        for (int index = 0; index < input.length(); index++) {
            hash = (31L * hash) + input.charAt(index);
        }
        long range = (MAX_STEP_BASIS_POINTS * 2L) + 1L;
        return Math.floorMod(hash, range) - MAX_STEP_BASIS_POINTS;
    }

    private long clamp(long value, long minimum, long maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }
}
