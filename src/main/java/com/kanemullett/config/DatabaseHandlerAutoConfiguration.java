package com.kanemullett.config;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jooq.JooqAutoConfiguration;
import org.springframework.context.annotation.PropertySource;

/**
 * Auto-configuration for the Database Handler module.
 *
 * <p>Registered via
 * {@code META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports},
 * so it applies automatically when this module is on the classpath. It
 * contributes the module's defaults from {@code database-handler.properties}
 * at the lowest precedence, so a consuming application can override any
 * individual setting (e.g. in its own {@code application.properties})
 * without losing the rest.
 *
 * <h2>Consumer contract</h2>
 * <ul>
 *   <li><b>Database:</b> PostgreSQL only. The PostgreSQL JDBC driver is
 *       provided by this module and jOOQ's dialect is fixed to
 *       {@code POSTGRES}.</li>
 *   <li><b>Connection:</b> the datasource is built from the following
 *       environment variables (or any other Spring property source):
 *       <ul>
 *         <li>{@code POSTGRES_HOST} (required)</li>
 *         <li>{@code POSTGRES_PORT} (default {@code 5432})</li>
 *         <li>{@code POSTGRES_DB} (required)</li>
 *         <li>{@code POSTGRES_USER} (required)</li>
 *         <li>{@code POSTGRES_PASSWORD} (required)</li>
 *         <li>{@code POSTGRES_SSLMODE} (default {@code prefer})</li>
 *       </ul>
 *   </li>
 *   <li><b>Migrations:</b> Flyway migrations are loaded from
 *       {@code classpath:db/migration} (e.g. {@code V1__create_schema.sql})
 *       and run on startup by Spring Boot's Flyway auto-configuration,
 *       before jOOQ's {@code DSLContext} is available. Baseline-on-migrate
 *       is disabled: a non-empty schema with no Flyway history table fails
 *       startup rather than silently skipping migrations.</li>
 * </ul>
 *
 * <p>This class is ordered before the DataSource, Flyway and jOOQ
 * auto-configurations so that its property source is in place when their
 * conditions are evaluated.
 */
@AutoConfiguration(before = {
    DataSourceAutoConfiguration.class,
    FlywayAutoConfiguration.class,
    JooqAutoConfiguration.class
})
@PropertySource("classpath:database-handler.properties")
public class DatabaseHandlerAutoConfiguration {
}
