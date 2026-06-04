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
import org.flywaydb.core.api.configuration.FluentConfiguration;
import org.junit.jupiter.api.Test;

class MarketPressureStateMigrationTest {

    @Test
    void cleanMigrationChainCreatesCurrentPressureStateWithoutMarketSegments() throws Exception {
        String jdbcUrl = h2JdbcUrl();
        migrateTo(jdbcUrl, null);

        try (Connection connection = connect(jdbcUrl)) {
            assertColumnExists(connection, "market_items", "market_momentum");
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

            insertCategory(connection, "farming");
            insertMinimalMarketItem(connection, "wheat");

            try (
                PreparedStatement statement = connection.prepareStatement(
                    """
                    SELECT
                        market_momentum,
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
                    assertEquals(0L, resultSet.getLong("market_momentum"));
                    assertEquals(1L, resultSet.getLong("base_unit_price"));
                    assertEquals(1L, resultSet.getLong("min_unit_price"));
                    assertEquals(1L, resultSet.getLong("max_unit_price"));
                    assertEquals(50L, resultSet.getLong("segment_size"));
                    assertEquals(0, new BigDecimal("0.0800").compareTo(resultSet.getBigDecimal("price_sensitivity")));
                    assertEquals(1L, resultSet.getLong("base_regen_quantity"));
                    assertEquals(60L, resultSet.getLong("regen_interval_seconds"));
                    assertEquals(0L, resultSet.getLong("net_position"));
                    assertNull(resultSet.getObject("min_net_position"));
                    assertNull(resultSet.getObject("max_net_position"));
                }
            }

            assertThrows(
                SQLException.class,
                () -> insertInvalidPressureBounds(connection)
            );
        }
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

    private static void insertCategory(Connection connection, String categoryId)
        throws SQLException {
        try (
            PreparedStatement statement = connection.prepareStatement(
                """
                INSERT INTO market_categories (
                    category_id,
                    display_name,
                    display_order,
                    icon_key,
                    created_at,
                    updated_at
                )
                VALUES (?, 'Farming', 0, 'WHEAT', ?, ?)
                """
            )
        ) {
            statement.setString(1, categoryId);
            statement.setObject(2, Instant.parse("2026-01-01T00:00:00Z"));
            statement.setObject(3, Instant.parse("2026-01-01T00:00:00Z"));
            statement.executeUpdate();
        }
    }

    private static void insertMinimalMarketItem(Connection connection, String itemId)
        throws SQLException {
        try (
            PreparedStatement statement = connection.prepareStatement(
                """
                INSERT INTO market_items (
                    item_id,
                    category_id,
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
                    drift_evaluated_at
                )
                VALUES (?, 'farming', ?, 'WHEAT', 100, 70, 'COINS', 0, 0.00, FALSE, TRUE, ?, ?)
                """
            )
        ) {
            Instant timestamp = Instant.parse("2026-01-01T00:00:00Z");
            statement.setString(1, itemId);
            statement.setString(2, itemId);
            statement.setObject(3, timestamp);
            statement.setObject(4, timestamp);
            statement.executeUpdate();
        }
    }

    private static void insertInvalidPressureBounds(Connection connection)
        throws SQLException {
        try (
            PreparedStatement statement = connection.prepareStatement(
                """
                INSERT INTO market_items (
                    item_id,
                    category_id,
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
                    min_net_position,
                    max_net_position,
                    drift_evaluated_at
                )
                VALUES ('invalid', 'farming', 'Invalid', 'WHEAT', 100, 70, 'COINS', 0, 0.00, FALSE, TRUE, ?, 5, 4, ?)
                """
            )
        ) {
            Instant timestamp = Instant.parse("2026-01-01T00:00:00Z");
            statement.setObject(1, timestamp);
            statement.setObject(2, timestamp);
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
