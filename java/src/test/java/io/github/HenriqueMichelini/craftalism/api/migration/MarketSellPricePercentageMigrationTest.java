package io.github.HenriqueMichelini.craftalism.api.migration;

import static org.junit.jupiter.api.Assertions.assertEquals;
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

class MarketSellPricePercentageMigrationTest {

    @Test
    void cleanMigrationChainCreatesSellPricePercentageDefaultAndConstraint() throws Exception {
        String jdbcUrl = h2JdbcUrl();
        migrateTo(jdbcUrl, null);

        try (Connection connection = connect(jdbcUrl)) {
            insertCategory(connection);
            insertMarketItem(connection, "wheat");

            assertEquals(
                0,
                new BigDecimal("0.7000").compareTo(
                    sellPricePercentage(connection, "wheat")
                )
            );
            assertThrows(
                SQLException.class,
                () -> insertInvalidSellPricePercentage(connection)
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
        FluentConfiguration configuration = Flyway
            .configure()
            .dataSource(jdbcUrl, "sa", "")
            .locations("classpath:db/migration");
        if (targetVersion != null) {
            configuration.target(targetVersion);
        }
        configuration.load().migrate();
    }

    private static void insertCategory(Connection connection) throws SQLException {
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
                VALUES ('farming', 'Farming', 0, 'WHEAT', ?, ?)
                """
            )
        ) {
            statement.setObject(1, Instant.parse("2026-01-01T00:00:00Z"));
            statement.setObject(2, Instant.parse("2026-01-01T00:00:00Z"));
            statement.executeUpdate();
        }
    }

    private static void insertMarketItem(
        Connection connection,
        String itemId
    ) throws SQLException {
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

    private static BigDecimal sellPricePercentage(
        Connection connection,
        String itemId
    ) throws SQLException {
        try (
            PreparedStatement statement = connection.prepareStatement(
                """
                SELECT sell_price_percentage
                FROM market_items
                WHERE item_id = ?
                """
            )
        ) {
            statement.setString(1, itemId);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getBigDecimal("sell_price_percentage");
            }
        }
    }

    private static void insertInvalidSellPricePercentage(Connection connection)
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
                    sell_price_percentage,
                    drift_evaluated_at
                )
                VALUES ('invalid', 'farming', 'Invalid', 'WHEAT', 100, 70, 'COINS', 0, 0.00, FALSE, TRUE, ?, 1.0000, ?)
                """
            )
        ) {
            Instant timestamp = Instant.parse("2026-01-01T00:00:00Z");
            statement.setObject(1, timestamp);
            statement.setObject(2, timestamp);
            statement.executeUpdate();
        }
    }
}
