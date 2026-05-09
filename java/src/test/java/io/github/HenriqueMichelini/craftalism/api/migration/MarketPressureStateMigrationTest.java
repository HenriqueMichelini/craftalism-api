package io.github.HenriqueMichelini.craftalism.api.migration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.FlywayException;
import org.flywaydb.core.api.configuration.FluentConfiguration;
import org.junit.jupiter.api.Test;

class MarketPressureStateMigrationTest {

    @Test
    void v15AddsPressureColumnsAndBackfillsNetPositionFromLegacySegments() throws Exception {
        String jdbcUrl = h2JdbcUrl();
        migrateTo(jdbcUrl, "14");

        try (Connection connection = connect(jdbcUrl)) {
            insertLegacyMarketItem(connection, "wheat", 70L, 10L, 5L);
            insertMarketSegment(connection, "wheat", 0L, 50L, 30L, 5L);
            insertMarketSegment(connection, "wheat", 1L, 50L, 40L, 6L);
        }

        migrateTo(jdbcUrl, null);

        try (Connection connection = connect(jdbcUrl)) {
            assertColumnExists(connection, "market_items", "base_unit_price");
            assertColumnExists(connection, "market_items", "min_unit_price");
            assertColumnExists(connection, "market_items", "max_unit_price");
            assertColumnExists(connection, "market_items", "segment_size");
            assertColumnExists(connection, "market_items", "price_sensitivity");
            assertColumnExists(connection, "market_items", "base_regen_quantity");
            assertColumnExists(connection, "market_items", "regen_interval_seconds");
            assertColumnExists(connection, "market_items", "net_position");
            assertColumnExists(connection, "market_items", "min_net_position");
            assertColumnExists(connection, "market_items", "max_net_position");
            assertTableMissing(connection, "market_segments");

            try (
                PreparedStatement statement = connection.prepareStatement(
                    """
                    SELECT
                        base_unit_price,
                        min_unit_price,
                        max_unit_price,
                        segment_size,
                        price_sensitivity,
                        base_regen_quantity,
                        regen_interval_seconds,
                        net_position,
                        min_net_position,
                        max_net_position
                    FROM market_items
                    WHERE item_id = ?
                    """
                )
            ) {
                statement.setString(1, "wheat");

                try (ResultSet resultSet = statement.executeQuery()) {
                    resultSet.next();
                    assertEquals(5L, resultSet.getLong("base_unit_price"));
                    assertEquals(3L, resultSet.getLong("min_unit_price"));
                    assertEquals(15L, resultSet.getLong("max_unit_price"));
                    assertEquals(50L, resultSet.getLong("segment_size"));
                    assertEquals(0, new BigDecimal("0.0800").compareTo(resultSet.getBigDecimal("price_sensitivity")));
                    assertEquals(1L, resultSet.getLong("base_regen_quantity"));
                    assertEquals(60L, resultSet.getLong("regen_interval_seconds"));
                    assertEquals(30L, resultSet.getLong("net_position"));
                    assertNull(resultSet.getObject("min_net_position"));
                    assertNull(resultSet.getObject("max_net_position"));
                }
            }
        }
    }

    @Test
    void v15RejectsLegacyStateWhenCurrentStockDoesNotMatchRemainingSegmentCapacity() throws Exception {
        String jdbcUrl = h2JdbcUrl();
        migrateTo(jdbcUrl, "14");

        try (Connection connection = connect(jdbcUrl)) {
            insertLegacyMarketItem(connection, "wheat", 70L, 10L, 5L);
            insertMarketSegment(connection, "wheat", 0L, 50L, 30L, 5L);
            insertMarketSegment(connection, "wheat", 1L, 50L, 39L, 6L);
        }

        assertThrows(FlywayException.class, () -> migrateTo(jdbcUrl, null));
    }

    private static String h2JdbcUrl() {
        return "jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=PostgreSQL;DATABASE_TO_UPPER=false;DB_CLOSE_DELAY=-1";
    }

    private static Connection connect(String jdbcUrl) throws SQLException {
        return DriverManager.getConnection(jdbcUrl, "sa", "");
    }

    private static void migrateTo(String jdbcUrl, String targetVersion) {
        FluentConfiguration configuration = Flyway.configure()
            .dataSource(jdbcUrl, "sa", "")
            .locations("classpath:db/migration");
        if (targetVersion != null) {
            configuration.target(targetVersion);
        }
        configuration.load().migrate();
    }

    private static void insertLegacyMarketItem(
        Connection connection,
        String itemId,
        long currentStock,
        long marketMomentum,
        long buyUnitEstimate
    ) throws SQLException {
        try (
            PreparedStatement statement = connection.prepareStatement(
                """
                INSERT INTO market_items (
                    item_id,
                    category_id,
                    category_display_name,
                    display_name,
                    icon_key,
                    buy_unit_estimate,
                    sell_unit_estimate,
                    currency,
                    current_stock,
                    variation_percent,
                    blocked,
                    operating,
                    last_updated_at,
                    market_momentum
                )
                VALUES (?, 'farming', 'Farming', ?, 'wheat', ?, ?, 'COINS', ?, 0.00, FALSE, TRUE, ?, ?)
                """
            )
        ) {
            statement.setString(1, itemId);
            statement.setString(2, itemId);
            statement.setLong(3, buyUnitEstimate);
            statement.setLong(4, buyUnitEstimate);
            statement.setLong(5, currentStock);
            statement.setObject(6, Instant.parse("2026-01-01T00:00:00Z"));
            statement.setLong(7, marketMomentum);
            statement.executeUpdate();
        }
    }

    private static void insertMarketSegment(
        Connection connection,
        String itemId,
        long segmentIndex,
        long maxCapacity,
        long remainingCapacity,
        long unitPrice
    ) throws SQLException {
        try (
            PreparedStatement statement = connection.prepareStatement(
                """
                INSERT INTO market_segments (
                    item_id,
                    segment_index,
                    max_capacity,
                    remaining_capacity,
                    unit_price
                )
                VALUES (?, ?, ?, ?, ?)
                """
            )
        ) {
            statement.setString(1, itemId);
            statement.setLong(2, segmentIndex);
            statement.setLong(3, maxCapacity);
            statement.setLong(4, remainingCapacity);
            statement.setLong(5, unitPrice);
            statement.executeUpdate();
        }
    }

    private static void assertColumnExists(Connection connection, String tableName, String columnName)
        throws SQLException {
        try (
            ResultSet columns = connection.getMetaData().getColumns(null, null, tableName, columnName)
        ) {
            assertNotNull(columns);
            if (columns.next()) {
                return;
            }
        }

        throw new AssertionError("Missing column " + tableName + "." + columnName);
    }

    private static void assertTableMissing(Connection connection, String tableName)
        throws SQLException {
        try (
            ResultSet tables = connection.getMetaData().getTables(null, null, tableName, null)
        ) {
            if (tables.next()) {
                throw new AssertionError("Unexpected table " + tableName);
            }
        }
    }
}
