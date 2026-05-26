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
    void v18AddsSellPricePercentageDefaultAndConstraint() throws Exception {
        String jdbcUrl = h2JdbcUrl();
        migrateTo(jdbcUrl, "17");

        try (Connection connection = connect(jdbcUrl)) {
            insertMarketItem(connection, "wheat");
        }

        migrateTo(jdbcUrl, null);

        try (Connection connection = connect(jdbcUrl)) {
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
                )
                VALUES (?, 'farming', 'Farming', ?, 'wheat', 100, 100, 'COINS', 0, 0.00, FALSE, TRUE, ?, 0, 100, 50, 300, 50, 0.0800, 1, 60, 0, NULL, NULL)
                """
            )
        ) {
            statement.setString(1, itemId);
            statement.setString(2, itemId);
            statement.setObject(3, Instant.parse("2026-01-01T00:00:00Z"));
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
                    max_net_position,
                    sell_price_percentage
                )
                VALUES ('invalid', 'farming', 'Farming', 'Invalid', 'wheat', 100, 100, 'COINS', 0, 0.00, FALSE, TRUE, ?, 0, 100, 50, 300, 50, 0.0800, 1, 60, 0, NULL, NULL, 1.0000)
                """
            )
        ) {
            statement.setObject(1, Instant.parse("2026-01-01T00:00:00Z"));
            statement.executeUpdate();
        }
    }
}
