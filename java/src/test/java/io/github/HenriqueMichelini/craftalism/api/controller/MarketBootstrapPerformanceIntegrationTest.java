package io.github.HenriqueMichelini.craftalism.api.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.HenriqueMichelini.craftalism.api.repository.MarketItemRepository;
import io.github.HenriqueMichelini.craftalism.api.repository.MarketSegmentRepository;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(
    properties = {
        "spring.jpa.properties.hibernate.generate_statistics=true"
    }
)
@AutoConfigureMockMvc
@ActiveProfiles("local")
class MarketBootstrapPerformanceIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MarketItemRepository marketItemRepository;

    @Autowired
    private MarketSegmentRepository marketSegmentRepository;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    @BeforeEach
    void clearStatistics() {
        statistics().clear();
    }

    @Test
    void startup_bootstrapsCatalogBeforeAnyMarketRequest() {
        assertEquals(3L, marketItemRepository.count());
        assertEquals(79L, marketSegmentRepository.count());
    }

    @Test
    void firstSnapshotAfterBootstrap_isReadOnlyAndAvoidsCollectionFetchFanOut() throws Exception {
        mockMvc.perform(get("/api/market/snapshot")).andExpect(status().isOk());

        Statistics statistics = statistics();
        assertEquals(0L, statistics.getEntityInsertCount());
        assertEquals(0L, statistics.getEntityUpdateCount());
        assertEquals(0L, statistics.getCollectionFetchCount());
        assertTrue(statistics.getPrepareStatementCount() <= 2L);
    }

    @Test
    void repeatedSnapshotsAfterBootstrap_remainReadOnlyAndBounded() throws Exception {
        mockMvc.perform(get("/api/market/snapshot")).andExpect(status().isOk());

        Statistics statistics = statistics();
        statistics.clear();

        mockMvc.perform(get("/api/market/snapshot")).andExpect(status().isOk());

        assertEquals(0L, statistics.getEntityInsertCount());
        assertEquals(0L, statistics.getEntityUpdateCount());
        assertEquals(0L, statistics.getCollectionFetchCount());
        assertTrue(statistics.getPrepareStatementCount() <= 2L);
    }

    private Statistics statistics() {
        SessionFactory sessionFactory = entityManagerFactory.unwrap(SessionFactory.class);
        Statistics statistics = sessionFactory.getStatistics();
        statistics.setStatisticsEnabled(true);
        return statistics;
    }
}
