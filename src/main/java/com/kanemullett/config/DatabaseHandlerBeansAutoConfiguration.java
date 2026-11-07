package com.kanemullett.config;

import org.jooq.DSLContext;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.jooq.JooqAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.support.ResourcePatternResolver;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kanemullett.function.QueryBuilderFunction;
import com.kanemullett.function.TableBuilderFunction;
import com.kanemullett.function.UpdateBuilderFunction;
import com.kanemullett.initialiser.TableInitialiser;
import com.kanemullett.mapper.DatabaseRecordMapper;
import com.kanemullett.service.DatabaseQueryService;
import com.kanemullett.service.DatabaseSchemaService;
import com.kanemullett.service.DatabaseTableService;

/**
 * Registers the Database Handler's beans once jOOQ's {@link DSLContext} is
 * available.
 *
 * <p>Registered via
 * {@code META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports},
 * so consuming applications get these beans without component-scanning
 * {@code com.kanemullett}. It is kept separate from
 * {@link DatabaseHandlerAutoConfiguration}, which has to run <i>before</i>
 * the DataSource, Flyway and jOOQ auto-configurations so that its property
 * defaults are in place, whereas these beans need the {@link DSLContext} and
 * so have to run <i>after</i> jOOQ's.
 *
 * <p>Every bean backs off ({@link ConditionalOnMissingBean}) if the consuming
 * application defines its own. The {@link TableInitialiser} can be switched
 * off with {@code database-handler.tables.enabled=false}.
 */
@AutoConfiguration(after = JooqAutoConfiguration.class)
@ConditionalOnBean(DSLContext.class)
public class DatabaseHandlerBeansAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public QueryBuilderFunction queryBuilderFunction(DSLContext dsl) {
        return new QueryBuilderFunction(dsl);
    }

    @Bean
    @ConditionalOnMissingBean
    public UpdateBuilderFunction updateBuilderFunction(DSLContext dsl) {
        return new UpdateBuilderFunction(dsl);
    }

    @Bean
    @ConditionalOnMissingBean
    public TableBuilderFunction tableBuilderFunction(DSLContext dsl) {
        return new TableBuilderFunction(dsl);
    }

    @Bean
    @ConditionalOnMissingBean
    public DatabaseRecordMapper databaseRecordMapper() {
        return new DatabaseRecordMapper();
    }

    @Bean
    @ConditionalOnMissingBean
    public DatabaseQueryService databaseQueryService(
            DSLContext dsl,
            QueryBuilderFunction queryBuilderFunction,
            UpdateBuilderFunction updateBuilderFunction,
            DatabaseRecordMapper recordMapper) {
        return new DatabaseQueryService(dsl, queryBuilderFunction, updateBuilderFunction, recordMapper);
    }

    @Bean
    @ConditionalOnMissingBean
    public DatabaseTableService databaseTableService(DSLContext dsl, TableBuilderFunction tableBuilderFunction) {
        return new DatabaseTableService(dsl, tableBuilderFunction);
    }

    @Bean
    @ConditionalOnMissingBean
    public DatabaseSchemaService databaseSchemaService(DSLContext dsl) {
        return new DatabaseSchemaService(dsl);
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "database-handler.tables", name = "enabled", matchIfMissing = true)
    public TableInitialiser tableInitialiser(
            DatabaseSchemaService schemaService,
            DatabaseTableService tableService,
            ResourcePatternResolver resourceResolver,
            ObjectProvider<ObjectMapper> objectMapper) {
        return new TableInitialiser(
            schemaService,
            tableService,
            resourceResolver,
            objectMapper.getIfAvailable(ObjectMapper::new)
        );
    }
}
