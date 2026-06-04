package io.github.HenriqueMichelini.craftalism.api.migration;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

class MarketEventMigrationTest {

    @Test
    void v22CreatesCurrentMarketEventTablesAndOneActiveEventGuard() throws Exception {
        String jdbcUrl = h2JdbcUrl();
        migrateTo(jdbcUrl, "22");

        try (Connection connection = connect(jdbcUrl)) {
            assertTableExists(connection, "market_event_templates");
            assertTableExists(connection, "market_event_instances");
            assertColumnAbsent(connection, "market_event_templates", obsoleteEventColumn());
            assertColumnAbsent(connection, "market_event_instances", obsoleteEventColumn());
            assertColumnExists(connection, "market_event_instances", "source");
            assertColumnExists(connection, "market_event_instances", "scope");
            assertColumnExists(connection, "market_event_instances", "selected_category_id");
            assertColumnExists(connection, "market_event_instances", "selected_item_ids");
            assertColumnExists(connection, "market_event_instances", "effect_basis_points");
            assertColumnExists(connection, "market_event_instances", "effect_version");
            assertColumnExists(connection, "market_event_instances", "started_at");
            assertColumnExists(connection, "market_event_instances", "ends_at");
            assertColumnExists(connection, "market_event_instances", "status");
            assertColumnExists(connection, "market_event_instances", "end_reason");
            assertColumnExists(connection, "market_event_instances", "active_slot");
            assertIndexExists(connection, "market_event_instances", "uq_market_event_instances_active_slot");

            insertTemplate(connection);
            insertActiveEvent(connection, "event-one");
            assertThrows(
                SQLException.class,
                () -> insertActiveEvent(connection, "event-two")
            );
        }
    }

    @Test
    void fullMigrationChainCreatesCurrentMarketEventColumns() throws Exception {
        String jdbcUrl = h2JdbcUrl();
        migrateTo(jdbcUrl, null);

        try (Connection connection = connect(jdbcUrl)) {
            assertTableExists(connection, "market_event_templates");
            assertTableExists(connection, "market_event_instances");
            assertColumnAbsent(connection, "market_event_templates", obsoleteEventColumn());
            assertColumnAbsent(connection, "market_event_instances", obsoleteEventColumn());
            assertColumnExists(connection, "market_event_instances", "source");
            assertColumnExists(connection, "market_event_instances", "scope");
            assertColumnExists(connection, "market_event_instances", "selected_category_id");
            assertColumnExists(connection, "market_event_instances", "selected_item_ids");
            assertColumnExists(connection, "market_event_instances", "effect_basis_points");
            assertColumnExists(connection, "market_event_instances", "effect_version");
            assertColumnExists(connection, "market_event_instances", "started_at");
            assertColumnExists(connection, "market_event_instances", "ends_at");
            assertColumnExists(connection, "market_event_instances", "status");
            assertColumnExists(connection, "market_event_instances", "end_reason");
            assertColumnExists(connection, "market_event_instances", "active_slot");
            assertIndexExists(connection, "market_event_instances", "uq_market_event_instances_active_slot");

            insertTemplate(connection);
            insertActiveEvent(connection, "event-one");
            deleteTemplate(connection);
            assertMarketEventInstanceCount(connection, 1);
            assertThrows(
                SQLException.class,
                () -> insertActiveEvent(connection, "event-two")
            );
        }
    }

    private static String h2JdbcUrl() {
        return "jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=PostgreSQL;DATABASE_TO_UPPER=false;DB_CLOSE_DELAY=-1";
    }

    private static Connection connect(String jdbcUrl) throws SQLException {
        return DriverManager.getConnection(jdbcUrl, "sa", "");
    }

    private static String obsoleteEventColumn() {
        return "rar" + "ity";
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

    private static void insertTemplate(Connection connection) throws SQLException {
        try (
            PreparedStatement statement = connection.prepareStatement(
                """
                INSERT INTO market_event_templates (
                    template_id,
                    scope,
                    automatic_weight,
                    automatic_enabled,
                    blocking_allowed,
                    min_duration_seconds,
                    max_duration_seconds,
                    min_effect_basis_points,
                    max_effect_basis_points,
                    effect_direction,
                    cooldown_seconds,
                    player_facing_name,
                    player_facing_description,
                    broad_scope_hint,
                    eligible_target_metadata,
                    created_at,
                    updated_at
                )
                VALUES (
                    'template',
                    'MARKET_WIDE',
                    1,
                    TRUE,
                    FALSE,
                    60,
                    120,
                    9500,
                    10500,
                    'UP',
                    300,
                    'Template',
                    'Description',
                    'World market',
                    '{}',
                    ?,
                    ?
                )
                """
            )
        ) {
            statement.setObject(1, Instant.parse("2026-01-01T00:00:00Z"));
            statement.setObject(2, Instant.parse("2026-01-01T00:00:00Z"));
            statement.executeUpdate();
        }
    }

    private static void insertActiveEvent(
        Connection connection,
        String actor
    ) throws SQLException {
        try (
            PreparedStatement statement = connection.prepareStatement(
                """
                INSERT INTO market_event_instances (
                    template_id,
                    source,
                    scope,
                    effect_basis_points,
                    effect_version,
                    blocking,
                    started_at,
                    ends_at,
                    status,
                    active_slot,
                    actor,
                    audit_metadata,
                    created_at,
                    updated_at
                )
                VALUES (
                    'template',
                    'SYSTEM',
                    'MARKET_WIDE',
                    10000,
                    1,
                    FALSE,
                    ?,
                    ?,
                    'ACTIVE',
                    'GLOBAL',
                    ?,
                    '{}',
                    ?,
                    ?
                )
                """
            )
        ) {
            Instant now = Instant.parse("2026-01-01T00:00:00Z");
            statement.setObject(1, now);
            statement.setObject(2, now.plusSeconds(60L));
            statement.setString(3, actor);
            statement.setObject(4, now);
            statement.setObject(5, now);
            statement.executeUpdate();
        }
    }

    private static void deleteTemplate(Connection connection) throws SQLException {
        try (
            PreparedStatement statement = connection.prepareStatement(
                "DELETE FROM market_event_templates WHERE template_id = 'template'"
            )
        ) {
            statement.executeUpdate();
        }
    }

    private static void assertMarketEventInstanceCount(
        Connection connection,
        int expectedCount
    ) throws SQLException {
        try (
            PreparedStatement statement = connection.prepareStatement(
                "SELECT COUNT(*) FROM market_event_instances"
            );
            ResultSet resultSet = statement.executeQuery()
        ) {
            assertTrue(resultSet.next());
            assertTrue(
                resultSet.getInt(1) == expectedCount,
                () -> "Expected market event instance count " + expectedCount
            );
        }
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

    private static void assertColumnAbsent(Connection connection, String tableName, String columnName)
        throws SQLException {
        try (
            ResultSet columns = connection.getMetaData().getColumns(null, null, tableName, columnName)
        ) {
            assertNotNull(columns);
            assertTrue(
                !columns.next(),
                () -> "Unexpected column " + tableName + "." + columnName
            );
        }
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
