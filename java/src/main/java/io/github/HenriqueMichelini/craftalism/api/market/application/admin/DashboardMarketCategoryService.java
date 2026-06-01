package io.github.HenriqueMichelini.craftalism.api.market.application.admin;

import io.github.HenriqueMichelini.craftalism.api.dto.MarketCategoryCreateRequestDTO;
import io.github.HenriqueMichelini.craftalism.api.dto.MarketCategoryUpdateRequestDTO;
import io.github.HenriqueMichelini.craftalism.api.exceptions.MarketCategoryAlreadyExistsException;
import io.github.HenriqueMichelini.craftalism.api.exceptions.MarketCategoryInUseException;
import io.github.HenriqueMichelini.craftalism.api.exceptions.MarketCategoryNotFoundException;
import io.github.HenriqueMichelini.craftalism.api.model.MarketCategory;
import io.github.HenriqueMichelini.craftalism.api.repository.MarketCategoryRepository;
import io.github.HenriqueMichelini.craftalism.api.repository.MarketItemRepository;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class DashboardMarketCategoryService {

    private final MarketCategoryRepository marketCategoryRepository;
    private final MarketItemRepository marketItemRepository;

    public DashboardMarketCategoryService(
        MarketCategoryRepository marketCategoryRepository,
        MarketItemRepository marketItemRepository
    ) {
        this.marketCategoryRepository = marketCategoryRepository;
        this.marketItemRepository = marketItemRepository;
    }

    public List<MarketCategory> getAllMarketCategories() {
        return marketCategoryRepository.findAllForMarketRead();
    }

    @Transactional
    public MarketCategory createMarketCategory(
        MarketCategoryCreateRequestDTO request
    ) {
        String categoryId = request.categoryId().trim();
        if (
            marketCategoryRepository.existsById(categoryId)
        ) throw new MarketCategoryAlreadyExistsException(categoryId);

        Instant now = Instant.now();
        MarketCategory category = new MarketCategory();
        category.setCategoryId(categoryId);
        category.setDisplayName(request.displayName().trim());
        category.setIconKey(request.iconKey().trim());
        category.setDisplayOrder(request.displayOrder());
        category.setCreatedAt(now);
        category.setUpdatedAt(now);
        return marketCategoryRepository.save(category);
    }

    @Transactional
    public MarketCategory updateMarketCategory(
        String categoryId,
        MarketCategoryUpdateRequestDTO request
    ) {
        MarketCategory category = getMarketCategory(categoryId);
        category.setDisplayName(request.displayName().trim());
        category.setIconKey(request.iconKey().trim());
        category.setDisplayOrder(request.displayOrder());
        category.setUpdatedAt(Instant.now());
        return marketCategoryRepository.save(category);
    }

    @Transactional
    public void deleteMarketCategory(String categoryId) {
        MarketCategory category = getMarketCategory(categoryId);
        if (
            marketItemRepository.existsByCategoryId(categoryId)
        ) throw new MarketCategoryInUseException(categoryId);

        marketCategoryRepository.delete(category);
    }

    MarketCategory getMarketCategory(String categoryId) {
        return marketCategoryRepository
            .findByCategoryId(categoryId)
            .orElseThrow(() -> new MarketCategoryNotFoundException(categoryId));
    }
}
