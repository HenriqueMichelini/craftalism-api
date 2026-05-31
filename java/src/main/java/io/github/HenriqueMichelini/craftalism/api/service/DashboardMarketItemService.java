package io.github.HenriqueMichelini.craftalism.api.service;

import io.github.HenriqueMichelini.craftalism.api.dto.MarketItemCreateRequestDTO;
import io.github.HenriqueMichelini.craftalism.api.dto.MarketItemUpdateRequestDTO;
import io.github.HenriqueMichelini.craftalism.api.exceptions.MarketItemAlreadyExistsException;
import io.github.HenriqueMichelini.craftalism.api.exceptions.MarketItemInUseException;
import io.github.HenriqueMichelini.craftalism.api.exceptions.MarketItemManagedByCatalogException;
import io.github.HenriqueMichelini.craftalism.api.exceptions.MarketItemNotFoundException;
import io.github.HenriqueMichelini.craftalism.api.model.MarketCategory;
import io.github.HenriqueMichelini.craftalism.api.model.MarketItem;
import io.github.HenriqueMichelini.craftalism.api.repository.MarketCategoryRepository;
import io.github.HenriqueMichelini.craftalism.api.repository.MarketItemRepository;
import io.github.HenriqueMichelini.craftalism.api.repository.MarketQuoteRepository;
import io.github.HenriqueMichelini.craftalism.api.repository.MarketTradeHistoryRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class DashboardMarketItemService {

    private static final long DEFAULT_BASE_UNIT_PRICE = 1L;
    private static final long DEFAULT_MIN_UNIT_PRICE = 1L;
    private static final long DEFAULT_MAX_UNIT_PRICE = 1L;
    private static final long DEFAULT_SEGMENT_SIZE = 50L;
    private static final BigDecimal DEFAULT_PRICE_SENSITIVITY = new BigDecimal(
        "0.0800"
    );
    private static final BigDecimal DEFAULT_SELL_PRICE_PERCENTAGE =
        new BigDecimal("0.7000");
    private static final long DEFAULT_BASE_REGEN_QUANTITY = 1L;
    private static final long DEFAULT_REGEN_INTERVAL_SECONDS = 60L;
    private static final long DEFAULT_NET_POSITION = 0L;

    private final MarketItemRepository marketItemRepository;
    private final MarketCategoryRepository marketCategoryRepository;
    private final MarketQuoteRepository marketQuoteRepository;
    private final MarketTradeHistoryRepository marketTradeHistoryRepository;
    private final Set<String> defaultCatalogItemIds;
    private final MarketItemConfigurationValidator configurationValidator =
        new MarketItemConfigurationValidator();
    private final MarketTradePlanner tradePlanner = new MarketTradePlanner();

    public DashboardMarketItemService(
        MarketItemRepository marketItemRepository,
        MarketCategoryRepository marketCategoryRepository,
        MarketQuoteRepository marketQuoteRepository,
        MarketTradeHistoryRepository marketTradeHistoryRepository,
        DefaultMarketCatalog defaultMarketCatalog
    ) {
        this.marketItemRepository = marketItemRepository;
        this.marketCategoryRepository = marketCategoryRepository;
        this.marketQuoteRepository = marketQuoteRepository;
        this.marketTradeHistoryRepository = marketTradeHistoryRepository;
        this.defaultCatalogItemIds = defaultMarketCatalog
            .items()
            .stream()
            .map(MarketSeedItem::itemId)
            .collect(Collectors.toUnmodifiableSet());
    }

    public List<MarketItem> getAllMarketItems() {
        List<MarketItem> items = marketItemRepository.findAllForMarketRead();
        items.forEach(tradePlanner::recomputeDerivedProjections);
        return items;
    }

    @Transactional
    public MarketItem createMarketItem(MarketItemCreateRequestDTO request) {
        String itemId = request.itemId().trim();
        if (
            marketItemRepository.existsById(itemId)
        ) throw new MarketItemAlreadyExistsException(itemId);

        MarketItem item = new MarketItem();
        item.setItemId(itemId);
        item.setCategory(getMarketCategory(request.categoryId().trim()));
        item.setDisplayName(request.displayName().trim());
        applyCreateValues(item, request);
        return marketItemRepository.save(item);
    }

    @Transactional
    public MarketItem updateMarketItem(
        String itemId,
        MarketItemUpdateRequestDTO request
    ) {
        MarketItem item = getMarketItem(itemId);
        applyUpdateValues(item, request);
        return marketItemRepository.save(item);
    }

    @Transactional
    public void deleteMarketItem(String itemId) {
        MarketItem item = getMarketItem(itemId);
        if (
            defaultCatalogItemIds.contains(item.getItemId())
        ) throw new MarketItemManagedByCatalogException(itemId);
        if (
            marketQuoteRepository.existsByItemId(itemId) ||
            marketTradeHistoryRepository.existsByItemId(itemId)
        ) throw new MarketItemInUseException(itemId);

        marketItemRepository.delete(item);
    }

    private MarketItem getMarketItem(String itemId) {
        return marketItemRepository
            .findByItemId(itemId)
            .orElseThrow(() -> new MarketItemNotFoundException(itemId));
    }

    private MarketCategory getMarketCategory(String categoryId) {
        return marketCategoryRepository
            .findByCategoryId(categoryId)
            .orElseThrow(() ->
                new io.github.HenriqueMichelini.craftalism.api.exceptions.MarketCategoryNotFoundException(
                    categoryId
                )
            );
    }

    private void applyCreateValues(
        MarketItem item,
        MarketItemCreateRequestDTO request
    ) {
        item.setIconKey(request.iconKey().trim());
        item.setCurrency(request.currency().trim());
        item.setBlocked(request.blocked());
        item.setOperating(request.operating());
        item.setBaseUnitPrice(valueOrDefault(
            request.baseUnitPrice(),
            DEFAULT_BASE_UNIT_PRICE
        ));
        item.setMinUnitPrice(valueOrDefault(
            request.minUnitPrice(),
            DEFAULT_MIN_UNIT_PRICE
        ));
        item.setMaxUnitPrice(valueOrDefault(
            request.maxUnitPrice(),
            DEFAULT_MAX_UNIT_PRICE
        ));
        item.setSegmentSize(valueOrDefault(
            request.segmentSize(),
            DEFAULT_SEGMENT_SIZE
        ));
        item.setPriceSensitivity(valueOrDefault(
            request.priceSensitivity(),
            DEFAULT_PRICE_SENSITIVITY
        ));
        item.setSellPricePercentage(valueOrDefault(
            request.sellPricePercentage(),
            DEFAULT_SELL_PRICE_PERCENTAGE
        ));
        item.setBaseRegenQuantity(valueOrDefault(
            request.baseRegenQuantity(),
            DEFAULT_BASE_REGEN_QUANTITY
        ));
        item.setRegenIntervalSeconds(valueOrDefault(
            request.regenIntervalSeconds(),
            DEFAULT_REGEN_INTERVAL_SECONDS
        ));
        item.setNetPosition(valueOrDefault(
            request.netPosition(),
            DEFAULT_NET_POSITION
        ));
        item.setMinNetPosition(request.minNetPosition());
        item.setMaxNetPosition(request.maxNetPosition());
        finishMutation(item);
    }

    private void applyUpdateValues(
        MarketItem item,
        MarketItemUpdateRequestDTO request
    ) {
        item.setIconKey(request.iconKey().trim());
        item.setCurrency(request.currency().trim());
        item.setBlocked(request.blocked());
        item.setOperating(request.operating());
        item.setBaseUnitPrice(request.baseUnitPrice());
        item.setMinUnitPrice(request.minUnitPrice());
        item.setMaxUnitPrice(request.maxUnitPrice());
        item.setSegmentSize(request.segmentSize());
        item.setPriceSensitivity(request.priceSensitivity());
        item.setSellPricePercentage(request.sellPricePercentage());
        item.setBaseRegenQuantity(request.baseRegenQuantity());
        item.setRegenIntervalSeconds(request.regenIntervalSeconds());
        item.setNetPosition(request.netPosition());
        item.setMinNetPosition(request.minNetPosition());
        item.setMaxNetPosition(request.maxNetPosition());
        finishMutation(item);
    }

    private void finishMutation(MarketItem item) {
        configurationValidator.validate(item);
        Instant now = Instant.now();
        item.setLastUpdatedAt(now);
        if (item.getDriftMultiplierBasisPoints() <= 0L) {
            item.setDriftMultiplierBasisPoints(10_000L);
        }
        if (item.getDriftEvaluatedAt() == null) {
            item.setDriftEvaluatedAt(now);
        }
        tradePlanner.recomputeDerivedProjections(item);
    }

    private static long valueOrDefault(Long value, long defaultValue) {
        return value == null ? defaultValue : value;
    }

    private static BigDecimal valueOrDefault(
        BigDecimal value,
        BigDecimal defaultValue
    ) {
        return value == null ? defaultValue : value;
    }
}
