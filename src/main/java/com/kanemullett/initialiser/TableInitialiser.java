package com.kanemullett.initialiser;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.kanemullett.model.TableDefinition;
import com.kanemullett.service.DatabaseSchemaService;
import com.kanemullett.service.DatabaseTableService;

/**
 * Creates the tables described by JSON table definitions on the classpath when
 * the application starts, if they don't already exist.
 *
 * <p>Definitions are read from {@code classpath*:definitions/tables/**}{@code /*.json},
 * i.e. {@code src/main/resources/definitions/tables/} in the consuming
 * application (subfolders included), plus the same folder in any jar on the
 * classpath. Each file holds one {@link TableDefinition}:
 * <pre>
 * {
 *   "schema": "app",
 *   "table": "users",
 *   "columns": [
 *     { "name": "id", "dataType": "VARCHAR", "primaryKey": true },
 *     { "name": "email", "dataType": "VARCHAR" }
 *   ]
 * }
 * </pre>
 *
 * <p>Every file is parsed before anything is created, so a broken file fails
 * startup without creating any tables. Each distinct schema is then created
 * with {@code CREATE SCHEMA IF NOT EXISTS}, followed by each table with
 * {@code CREATE TABLE IF NOT EXISTS}. Existing tables are left untouched, so
 * changes to a table that already exists still need a Flyway migration.
 *
 * <p>Registered by
 * {@link com.kanemullett.config.DatabaseHandlerBeansAutoConfiguration} and
 * disabled with {@code database-handler.tables.enabled=false}.
 */
public class TableInitialiser implements InitializingBean {

    private final DatabaseSchemaService schemaService;
    private final DatabaseTableService tableService;
    private final ResourcePatternResolver resourceResolver;
    private final ObjectMapper objectMapper;

    private static final String TABLE_DEFINITIONS = "classpath*:definitions/tables/**/*.json";

    /**
     * Constructs a new {@code TableInitialiser}.
     *
     * @param schemaService    the service used to create schemas.
     * @param tableService     the service used to create tables.
     * @param resourceResolver the resolver used to find definition files.
     * @param objectMapper     the mapper used to parse definition files.
     */
    public TableInitialiser(
            DatabaseSchemaService schemaService,
            DatabaseTableService tableService,
            ResourcePatternResolver resourceResolver,
            ObjectMapper objectMapper) {
        this.schemaService = schemaService;
        this.tableService = tableService;
        this.resourceResolver = resourceResolver;
        this.objectMapper = objectMapper;
    }

    /**
     * Finds and parses every table definition, then creates their schemas and
     * tables. Does nothing if no definitions are found.
     *
     * @throws IllegalStateException if the classpath cannot be scanned or a
     *                               definition cannot be read; the message
     *                               names the file.
     */
    @Override
    public void afterPropertiesSet() {
        final Resource[] resources = loadDefinitions();

        if (resources.length == 0) {
            return;
        }

        final List<TableDefinition> definitions = Arrays.stream(resources)
            .map(this::readDefinition)
            .toList();

        definitions.stream()
            .map(TableDefinition::getSchema)
            .distinct()
            .forEach(schemaService::createSchema);
        definitions.forEach(tableService::createTable);
    }

    /**
     * Finds every table definition file on the classpath.
     *
     * @return the matching resources, empty if there are none.
     */
    private Resource[] loadDefinitions() {
        try {
            return resourceResolver.getResources(TABLE_DEFINITIONS);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to scan for table definitions at " + TABLE_DEFINITIONS, e);
        }
    }

    /**
     * Parses one definition file into a {@link TableDefinition}.
     *
     * @param resource the file to parse.
     * @return the parsed definition.
     */
    private TableDefinition readDefinition(Resource resource) {
        try (InputStream inputStream = resource.getInputStream()) {
            final JsonNode json = objectMapper.readTree(inputStream);

            if (!(json instanceof ObjectNode node)) {
                throw new IllegalStateException("Table definition must be a JSON object: " + resource.getDescription());
            }

            return objectMapper.treeToValue(node, TableDefinition.class);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read table definition: " + resource.getDescription(), e);
        }
    }
}
