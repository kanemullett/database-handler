package com.kanemullett.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

import org.flywaydb.core.api.FlywayException;
import org.flywaydb.core.api.MigrationVersion;
import org.flywaydb.core.api.migration.Context;
import org.flywaydb.core.api.migration.JavaMigration;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jooq.JooqAutoConfiguration;
import org.springframework.boot.autoconfigure.transaction.TransactionAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Starts the module's auto-configuration against a real PostgreSQL instance,
 * configured only through the POSTGRES_* variables, and checks that Flyway
 * migrations actually run on startup.
 *
 * <p>The migration under test is registered as a {@link JavaMigration} bean,
 * which Spring Boot's Flyway auto-configuration applies alongside any found
 * in {@code classpath:db/migration}, so no migration files are needed.
 */
@Testcontainers
class DatabaseHandlerAutoConfigurationTest {

    @Container
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    private ApplicationContextRunner contextRunner(String database) {
        return new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                DatabaseHandlerAutoConfiguration.class,
                DataSourceAutoConfiguration.class,
                TransactionAutoConfiguration.class,
                FlywayAutoConfiguration.class,
                JooqAutoConfiguration.class
            ))
            .withBean(JavaMigration.class, MigrationCheck::new)
            .withPropertyValues(
                "POSTGRES_HOST=" + POSTGRES.getHost(),
                "POSTGRES_PORT=" + POSTGRES.getFirstMappedPort(),
                "POSTGRES_DB=" + database,
                "POSTGRES_USER=" + POSTGRES.getUsername(),
                "POSTGRES_PASSWORD=" + POSTGRES.getPassword(),
                "POSTGRES_SSLMODE=disable"
            );
    }

    @Test
    void shouldRunFlywayMigrationsOnStartup() {
        contextRunner(POSTGRES.getDatabaseName()).run(context -> {
            assertThat(context).hasNotFailed();

            final DSLContext dsl = context.getBean(DSLContext.class);

            assertThat(dsl.fetchValue(
                "select count(*) from flyway_schema_history where version = '1' and success"
            )).isEqualTo(1L);
            assertThat(dsl.fetchValue("select id from migration_check")).isEqualTo("v1");
        });
    }

    @Test
    void shouldUsePostgresDialect() {
        contextRunner(POSTGRES.getDatabaseName()).run(context ->
            assertThat(context.getBean(DSLContext.class).dialect()).isEqualTo(SQLDialect.POSTGRES)
        );
    }

    @Test
    void shouldFailRatherThanBaselineANonEmptySchema() throws SQLException {
        // Given a database that already has tables but no Flyway history
        try (Connection connection = DriverManager.getConnection(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
             Statement statement = connection.createStatement()) {
            statement.execute("CREATE DATABASE non_empty");
        }
        final String nonEmptyUrl = POSTGRES.getJdbcUrl().replace("/" + POSTGRES.getDatabaseName(), "/non_empty");
        try (Connection connection = DriverManager.getConnection(
                nonEmptyUrl, POSTGRES.getUsername(), POSTGRES.getPassword())) {
            DSL.using(connection, SQLDialect.POSTGRES).execute("CREATE TABLE existing (id VARCHAR)");
        }

        // When / Then: startup fails instead of baselining and skipping V1
        contextRunner("non_empty").run(context -> {
            assertThat(context).hasFailed();
            assertThat(context.getStartupFailure()).hasRootCauseInstanceOf(FlywayException.class);
        });
    }

    /**
     * Test migration that creates a table with one row, so a test can check
     * it was applied.
     */
    private static final class MigrationCheck implements JavaMigration {

        @Override
        public MigrationVersion getVersion() {
            return MigrationVersion.fromVersion("1");
        }

        @Override
        public String getDescription() {
            return "create migration check";
        }

        @Override
        public Integer getChecksum() {
            return null;
        }

        @Override
        public boolean canExecuteInTransaction() {
            return true;
        }

        @Override
        public void migrate(Context context) throws SQLException {
            try (Statement statement = context.getConnection().createStatement()) {
                statement.execute("CREATE TABLE migration_check (id VARCHAR PRIMARY KEY)");
                statement.execute("INSERT INTO migration_check (id) VALUES ('v1')");
            }
        }
    }
}
