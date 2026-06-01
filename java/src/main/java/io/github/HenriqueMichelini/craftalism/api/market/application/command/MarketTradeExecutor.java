package io.github.HenriqueMichelini.craftalism.api.market.application.command;

import io.github.HenriqueMichelini.craftalism.api.dto.MarketSide;
import io.github.HenriqueMichelini.craftalism.api.exceptions.MarketRejectionCode;
import io.github.HenriqueMichelini.craftalism.api.exceptions.MarketRejectionException;
import io.github.HenriqueMichelini.craftalism.api.market.infrastructure.store.MarketQuoteStore;
import io.github.HenriqueMichelini.craftalism.api.market.domain.trade.MarketTradePlanner;
import io.github.HenriqueMichelini.craftalism.api.model.Balance;
import io.github.HenriqueMichelini.craftalism.api.model.MarketItem;
import io.github.HenriqueMichelini.craftalism.api.model.MarketTradeHistory;
import io.github.HenriqueMichelini.craftalism.api.repository.BalanceRepository;
import io.github.HenriqueMichelini.craftalism.api.repository.MarketItemRepository;
import io.github.HenriqueMichelini.craftalism.api.repository.MarketTradeHistoryRepository;
import java.time.Instant;
import java.util.UUID;
import java.util.function.Supplier;
import org.springframework.http.HttpStatus;

public final class MarketTradeExecutor {

    private final BalanceRepository balanceRepository;
    private final MarketItemRepository marketItemRepository;
    private final MarketTradeHistoryRepository marketTradeHistoryRepository;
    private final MarketTradePlanner tradePlanner;

    public MarketTradeExecutor(
        BalanceRepository balanceRepository,
        MarketItemRepository marketItemRepository,
        MarketTradeHistoryRepository marketTradeHistoryRepository,
        MarketTradePlanner tradePlanner
    ) {
        this.balanceRepository = balanceRepository;
        this.marketItemRepository = marketItemRepository;
        this.marketTradeHistoryRepository = marketTradeHistoryRepository;
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

        return applySell(
            playerUuid,
            item,
            quote,
            snapshotVersion,
            currentSnapshotVersion
        );
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
        if (balance.getAmount() < quote.totalPrice()) {
            throw insufficientFunds(currentSnapshotVersion);
        }
        balance.setAmount(balance.getAmount() - quote.totalPrice());
        balanceRepository.save(balance);
        item.setNetPosition(
            Math.addExact(item.getNetPosition(), plan.executedQuantity())
        );
        item.setLastUpdatedAt(Instant.now());
        tradePlanner.recomputeDerivedProjections(item);
        marketItemRepository.save(item);
        saveTradeHistory(playerUuid, item, quote, plan.executedQuantity());
        return new AppliedTrade(
            plan.executedQuantity(),
            quote.unitPrice(),
            quote.totalPrice()
        );
    }

    private AppliedTrade applySell(
        UUID playerUuid,
        MarketItem item,
        MarketQuoteStore.StoredQuote quote,
        String snapshotVersion,
        Supplier<String> currentSnapshotVersion
    ) {
        MarketTradePlanner.TradePlan plan = requireFullSellPlan(
            item,
            quote.quantity(),
            snapshotVersion
        );
        Balance balance = balanceRepository
            .findForUpdate(playerUuid)
            .orElseGet(() -> new Balance(playerUuid, 0L));
        long creditedBalanceAmount = addBalanceAmounts(
            balance.getAmount(),
            quote.totalPrice(),
            currentSnapshotVersion
        );
        balance.setUuid(playerUuid);
        balance.setAmount(creditedBalanceAmount);
        balanceRepository.save(balance);
        item.setNetPosition(
            Math.subtractExact(item.getNetPosition(), plan.executedQuantity())
        );
        item.setLastUpdatedAt(Instant.now());
        tradePlanner.recomputeDerivedProjections(item);
        marketItemRepository.save(item);
        saveTradeHistory(playerUuid, item, quote, plan.executedQuantity());
        return new AppliedTrade(
            plan.executedQuantity(),
            quote.unitPrice(),
            quote.totalPrice()
        );
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

    private void saveTradeHistory(
        UUID playerUuid,
        MarketItem item,
        MarketQuoteStore.StoredQuote quote,
        long executedQuantity
    ) {
        MarketTradeHistory history = new MarketTradeHistory();
        history.setPlayerUuid(playerUuid);
        history.setItemId(item.getItemId());
        history.setSide(quote.side());
        history.setQuantity(executedQuantity);
        history.setUnitPrice(quote.unitPrice());
        history.setTotalPrice(quote.totalPrice());
        history.setCurrency(item.getCurrency());
        history.setSnapshotVersion(quote.snapshotVersion());
        history.setExecutedAt(Instant.now());
        marketTradeHistoryRepository.save(history);
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

    private long addBalanceAmounts(
        long currentAmount,
        long amount,
        Supplier<String> currentSnapshotVersion
    ) {
        try {
            return Math.addExact(currentAmount, amount);
        } catch (ArithmeticException ex) {
            throw rejection(
                MarketRejectionCode.BALANCE_OVERFLOW,
                "Balance amount exceeds supported range.",
                HttpStatus.UNPROCESSABLE_ENTITY,
                currentSnapshotVersion.get()
            );
        }
    }

    record AppliedTrade(
        long executedQuantity,
        long unitPrice,
        long totalPrice
    ) {}
}
