package io.github.HenriqueMichelini.craftalism.api.migration;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.configuration.FluentConfiguration;
import org.junit.jupiter.api.Test;

class MarketTradeHistoryMigrationTest {

    @Test
    void v17CreatesMarketTradeHistoryTableAndIndexes() throws Exception {
        String jdbcUrl = h2JdbcUrl();

        migrateTo(jdbcUrl, null);

        try (Connection connection = connect(jdbcUrl)) {
            assertTableExists(connection, "market_trade_history");
            assertColumnExists(connection, "market_trade_history", "id");
            assertColumnExists(connection, "market_trade_history", "player_uuid");
            assertColumnExists(connection, "market_trade_history", "item_id");
            assertColumnExists(connection, "market_trade_history", "side");
            assertColumnExists(connection, "market_trade_history", "quantity");
            assertColumnExists(connection, "market_trade_history", "unit_price");
            assertColumnExists(connection, "market_trade_history", "total_price");
            assertColumnExists(connection, "market_trade_history", "currency");
            assertColumnExists(connection, "market_trade_history", "snapshot_version");
            assertColumnExists(connection, "market_trade_history", "executed_at");

            assertIndexExists(connection, "market_trade_history", "idx_market_trade_history_player_uuid");
            assertIndexExists(connection, "market_trade_history", "idx_market_trade_history_item_id");
            assertIndexExists(connection, "market_trade_history", "idx_market_trade_history_side");
            assertIndexExists(connection, "market_trade_history", "idx_market_trade_history_executed_at");
            assertIndexExists(connection, "market_trade_history", "idx_market_trade_history_default_order");
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

    private static void assertTableExists(Connection connection, String tableName)
        throws SQLException {
        try (
            ResultSet tables = connection.getMetaData().getTables(null, null, tableName, null)
        ) {
            if (tables.next()) {
                return;
            }
        }

        throw new AssertionError("Missing table " + tableName);
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

    private static void assertIndexExists(Connection connection, String tableName, String indexName)
        throws SQLException {
        try (
            ResultSet indexes = connection.getMetaData().getIndexInfo(null, null, tableName, false, false)
        ) {
            assertNotNull(indexes);
            while (indexes.next()) {
                if (indexName.equalsIgnoreCase(indexes.getString("INDEX_NAME"))) {
                    return;
                }
            }
        }

        throw new AssertionError("Missing index " + indexName);
    }
}
