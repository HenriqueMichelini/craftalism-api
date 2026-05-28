package io.github.HenriqueMichelini.craftalism.api.migration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

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

class MarketQuoteMigrationTest {

    @Test
    void v21AddsQuotePricingContextColumnsAndDefaultsExistingRows() throws Exception {
        String jdbcUrl = h2JdbcUrl();
        migrateTo(jdbcUrl, "20");

        try (Connection connection = connect(jdbcUrl)) {
            insertQuote(connection);
        }

        migrateTo(jdbcUrl, null);

        try (Connection connection = connect(jdbcUrl)) {
            assertColumnExists(connection, "market_quotes", "pricing_context_version");
            assertColumnExists(connection, "market_quotes", "pressure_position");
            assertColumnExists(connection, "market_quotes", "drift_revision");
            assertColumnExists(connection, "market_quotes", "named_event_instance_id");
            assertColumnExists(connection, "market_quotes", "event_effect_version");

            try (
                PreparedStatement statement = connection.prepareStatement(
                    """
                    SELECT
                        pricing_context_version,
                        pressure_position,
                        drift_revision,
                        named_event_instance_id,
                        event_effect_version
                    FROM market_quotes
                    WHERE quote_token = 'quote-token'
                    """
                );
                ResultSet resultSet = statement.executeQuery()
            ) {
                resultSet.next();
                assertEquals(1, resultSet.getInt("pricing_context_version"));
                assertEquals(0L, resultSet.getLong("pressure_position"));
                assertNull(resultSet.getObject("drift_revision"));
                assertNull(resultSet.getObject("named_event_instance_id"));
                assertNull(resultSet.getObject("event_effect_version"));
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
        FluentConfiguration configuration = Flyway.configure()
            .dataSource(jdbcUrl, "sa", "")
            .locations("classpath:db/migration");
        if (targetVersion != null) {
            configuration.target(targetVersion);
        }
        configuration.load().migrate();
    }

    private static void insertQuote(Connection connection) throws SQLException {
        try (
            PreparedStatement statement = connection.prepareStatement(
                """
                INSERT INTO market_quotes (
                    quote_token,
                    player_uuid,
                    item_id,
                    side,
                    quantity,
                    unit_price,
                    total_price,
                    snapshot_version,
                    expires_at,
                    created_at,
                    status
                )
                VALUES (?, ?, 'wheat', 'BUY', 10, 5, 50, 'market:snapshot', ?, ?, 'ACTIVE')
                """
            )
        ) {
            statement.setString(1, "quote-token");
            statement.setObject(2, UUID.fromString("110e8400-e29b-41d4-a716-446655440000"));
            statement.setObject(3, Instant.parse("2026-01-01T00:01:00Z"));
            statement.setObject(4, Instant.parse("2026-01-01T00:00:00Z"));
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
}
