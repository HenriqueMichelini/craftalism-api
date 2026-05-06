package io.github.HenriqueMichelini.craftalism.api.service;

import io.github.HenriqueMichelini.craftalism.api.dto.MarketSide;
import io.github.HenriqueMichelini.craftalism.api.exceptions.MarketRejectionCode;
import io.github.HenriqueMichelini.craftalism.api.exceptions.MarketRejectionException;
import io.github.HenriqueMichelini.craftalism.api.model.Balance;
import io.github.HenriqueMichelini.craftalism.api.model.MarketItem;
import io.github.HenriqueMichelini.craftalism.api.repository.BalanceRepository;
import io.github.HenriqueMichelini.craftalism.api.repository.MarketItemRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import java.util.function.Supplier;
import org.springframework.http.HttpStatus;

final class MarketTradeExecutor {

    private final BalanceRepository balanceRepository;
    private final MarketItemRepository marketItemRepository;
    private final MarketTradePlanner tradePlanner;

    MarketTradeExecutor(
        BalanceRepository balanceRepository,
        MarketItemRepository marketItemRepository,
        MarketTradePlanner tradePlanner
    ) {
        this.balanceRepository = balanceRepository;
        this.marketItemRepository = marketItemRepository;
        this.tradePlanner = tradePlanner;
    }

    AppliedTrade applyTrade(
        UUID playerUuid,
        MarketItem item,
        MarketQuoteStore.StoredQuote quote,
        String snapshotVersion,
        Supplier<String> currentSnapshotVersion
    ) {
        tradePlanner.recomputeDerivedProjections(item);
        if (quote.side() == MarketSide.BUY) {
            return applyBuy(
                playerUuid,
                item,
                quote,
                snapshotVersion,
                currentSnapshotVersion
            );
        }

        return applySell(playerUuid, item, quote, snapshotVersion);
    }

    private AppliedTrade applyBuy(
        UUID playerUuid,
        MarketItem item,
        MarketQuoteStore.StoredQuote quote,
        String snapshotVersion,
        Supplier<String> currentSnapshotVersion
    ) {
        MarketTradePlanner.TradePlan plan = requireFullBuyPlan(
            item,
            quote.quantity(),
            snapshotVersion
        );
        Balance balance = balanceRepository
            .findForUpdate(playerUuid)
            .orElseThrow(() -> insufficientFunds(currentSnapshotVersion));
        verifyQuotedExecution(
            plan,
            quote,
            "Quoted buy execution no longer matches the authoritative segment traversal."
        );
        if (balance.getAmount() < plan.totalPrice()) {
            throw insufficientFunds(currentSnapshotVersion);
        }
        balance.setAmount(balance.getAmount() - plan.totalPrice());
        balanceRepository.save(balance);
        tradePlanner.applyConsumption(plan);
        item.setVariationPercent(
            item.getVariationPercent().add(BigDecimal.valueOf(0.6))
        );
        item.setLastUpdatedAt(Instant.now());
        tradePlanner.recomputeDerivedProjections(item);
        marketItemRepository.save(item);
        return new AppliedTrade(
            plan.executedQuantity(),
            plan.unitPrice(),
            plan.totalPrice()
        );
    }

    private AppliedTrade applySell(
        UUID playerUuid,
        MarketItem item,
        MarketQuoteStore.StoredQuote quote,
        String snapshotVersion
    ) {
        MarketTradePlanner.TradePlan plan = requireFullSellPlan(
            item,
            quote.quantity(),
            snapshotVersion
        );
        Balance balance = balanceRepository
            .findForUpdate(playerUuid)
            .orElseGet(() -> new Balance(playerUuid, 0L));
        verifyQuotedExecution(
            plan,
            quote,
            "Quoted sell execution no longer matches the authoritative segment traversal."
        );
        balance.setUuid(playerUuid);
        balance.setAmount(balance.getAmount() + plan.totalPrice());
        balanceRepository.save(balance);
        tradePlanner.applyRestoration(plan);
        item.setVariationPercent(
            item.getVariationPercent().subtract(BigDecimal.valueOf(0.6))
        );
        item.setLastUpdatedAt(Instant.now());
        tradePlanner.recomputeDerivedProjections(item);
        marketItemRepository.save(item);
        return new AppliedTrade(
            plan.executedQuantity(),
            plan.unitPrice(),
            plan.totalPrice()
        );
    }

    private void verifyQuotedExecution(
        MarketTradePlanner.TradePlan plan,
        MarketQuoteStore.StoredQuote quote,
        String message
    ) {
        if (
            plan.totalPrice() != quote.totalPrice() ||
            plan.unitPrice() != quote.unitPrice()
        ) {
            throw new IllegalStateException(message);
        }
    }

    private MarketTradePlanner.TradePlan requireFullBuyPlan(
        MarketItem item,
        long requestedQuantity,
        String snapshotVersion
    ) {
        MarketTradePlanner.TradePlan plan = tradePlanner.buyPlan(
            item,
            requestedQuantity
        );
        if (plan.executedQuantity() != requestedQuantity) {
            throw rejection(
                MarketRejectionCode.INSUFFICIENT_STOCK,
                "Requested quantity exceeds configured pressure bounds.",
                HttpStatus.UNPROCESSABLE_ENTITY,
                snapshotVersion
            );
        }
        return plan;
    }

    private MarketTradePlanner.TradePlan requireFullSellPlan(
        MarketItem item,
        long requestedQuantity,
        String snapshotVersion
    ) {
        MarketTradePlanner.TradePlan plan = tradePlanner.sellPlan(
            item,
            requestedQuantity
        );
        if (plan.executedQuantity() != requestedQuantity) {
            throw rejection(
                MarketRejectionCode.INSUFFICIENT_STOCK,
                "Requested quantity exceeds configured pressure bounds.",
                HttpStatus.UNPROCESSABLE_ENTITY,
                snapshotVersion
            );
        }
        return plan;
    }

    private MarketRejectionException insufficientFunds(
        Supplier<String> currentSnapshotVersion
    ) {
        return rejection(
            MarketRejectionCode.INSUFFICIENT_FUNDS,
            "Player does not have enough funds.",
            HttpStatus.UNPROCESSABLE_ENTITY,
            currentSnapshotVersion.get()
        );
    }

    private MarketRejectionException rejection(
        MarketRejectionCode code,
        String message,
        HttpStatus status,
        String snapshotVersion
    ) {
        return new MarketRejectionException(
            code,
            message,
            status,
            snapshotVersion
        );
    }

    record AppliedTrade(
        long executedQuantity,
        long unitPrice,
        long totalPrice
    ) {}
}
