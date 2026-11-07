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

public class TableInitialiser implements InitializingBean {

    private final DatabaseSchemaService schemaService;
    private final DatabaseTableService tableService;
    private final ResourcePatternResolver resourceResolver;
    private final ObjectMapper objectMapper;

    private static final String TABLE_DEFINITIONS = "classpath*:definitions/tables/**/*.json";

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

    private Resource[] loadDefinitions() {
        try {
            return resourceResolver.getResources(TABLE_DEFINITIONS);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to scan for table definitions at " + TABLE_DEFINITIONS, e);
        }
    }

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
