package com.kanemullett.config;

import javax.sql.DataSource;

import org.flywaydb.core.Flyway;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;

/**
 * Auto-configuration for the Database Handler module.
 *
 * <p>Automatically configures Flyway for database migration management when
 * this module is included as a dependency. Consuming applications should
 * provide their migration scripts under {@code classpath:db/migration} using
 * Flyway's versioned naming convention (e.g. {@code V1__create_schema.sql}).
 *
 * <p>This configuration is registered via
 * {@code META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports}
 * and requires a {@link DataSource} bean to be present in the application
 * context, typically configured via {@code application.properties}:
 *
 * <pre>
 * spring.datasource.url=${DB_URL}
 * spring.datasource.username=${DB_USERNAME}
 * spring.datasource.password=${DB_PASSWORD}
 * </pre>
 */
@AutoConfiguration
public class DatabaseHandlerAutoConfiguration {

    /**
     * Configures and initialises Flyway for database migration management.
     *
     * <p>Migrations are loaded from {@code classpath:db/migration} and
     * {@code baselineOnMigrate} is enabled to support applying migrations to
     * existing databases.
     *
     * @param dataSource the {@link DataSource} to use for database connections.
     * @return the configured {@link Flyway} instance.
     */
    @Bean
    public Flyway flyway(DataSource dataSource) {
        return Flyway.configure()
            .dataSource(dataSource)
            .locations("classpath:db/migration")
            .baselineOnMigrate(true)
            .load();
    }
}
