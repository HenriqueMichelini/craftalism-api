package io.github.HenriqueMichelini.craftalism.api.migration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

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

class MarketDriftMigrationTest {

    @Test
    void v23AddsMarketItemDriftColumnsAndDefaultsExistingRows() throws Exception {
        String jdbcUrl = h2JdbcUrl();
        migrateTo(jdbcUrl, "22");

        try (Connection connection = connect(jdbcUrl)) {
            insertMarketItem(connection);
        }

        migrateTo(jdbcUrl, null);

        try (Connection connection = connect(jdbcUrl)) {
            assertColumnExists(connection, "market_items", "drift_multiplier_basis_points");
            assertColumnExists(connection, "market_items", "drift_revision");
            assertColumnExists(connection, "market_items", "drift_evaluated_at");

            try (
                PreparedStatement statement = connection.prepareStatement(
                    """
                    SELECT
                        drift_multiplier_basis_points,
                        drift_revision,
                        drift_evaluated_at
                    FROM market_items
                    WHERE item_id = 'wheat'
                    """
                );
                ResultSet resultSet = statement.executeQuery()
            ) {
                resultSet.next();
                assertEquals(10_000L, resultSet.getLong("drift_multiplier_basis_points"));
                assertEquals(0L, resultSet.getLong("drift_revision"));
                assertNotNull(resultSet.getObject("drift_evaluated_at"));
            }
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

    private static void insertMarketItem(Connection connection)
        throws SQLException {
        try (
            PreparedStatement categoryStatement = connection.prepareStatement(
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
            categoryStatement.setObject(1, Instant.parse("2026-01-01T00:00:00Z"));
            categoryStatement.setObject(2, Instant.parse("2026-01-01T00:00:00Z"));
            categoryStatement.executeUpdate();
        }

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
                VALUES ('wheat', 'farming', 'Wheat', 'wheat', 100, 70, 'COINS', 0, 0.00, FALSE, TRUE, ?, 0, 100, 50, 300, 50, 0.0800, 1, 60, 0, NULL, NULL, 0.7000)
                """
            )
        ) {
            statement.setObject(1, Instant.parse("2026-01-01T00:00:00Z"));
            statement.executeUpdate();
        }
    }

    private static void assertColumnExists(
        Connection connection,
        String tableName,
        String columnName
    ) throws SQLException {
        try (
            ResultSet columns = connection
                .getMetaData()
                .getColumns(null, null, tableName, columnName)
        ) {
            assertNotNull(columns);
            if (columns.next()) {
                return;
            }
        }

        throw new AssertionError("Missing column " + tableName + "." + columnName);
    }
}
