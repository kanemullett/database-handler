package com.kanemullett.initialiser;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.groups.Tuple.tuple;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kanemullett.model.ColumnDefinition;
import com.kanemullett.model.TableDefinition;
import com.kanemullett.model.type.SqlDataType;
import com.kanemullett.service.DatabaseSchemaService;
import com.kanemullett.service.DatabaseTableService;

class TableInitialiserTest {

    private static final String USERS = """
        {
          "schema": "app",
          "table": "users",
          "columns": [
            { "name": "id", "dataType": "VARCHAR", "primaryKey": true },
            { "name": "email", "dataType": "VARCHAR" },
            { "name": "created", "dataType": "TIMESTAMP_WITHOUT_TIME_ZONE" }
          ]
        }
        """;

    private static final String ORDERS = """
        {
          "schema": "app",
          "table": "orders",
          "columns": [
            { "name": "id", "dataType": "VARCHAR", "primaryKey": true },
            { "name": "items", "dataType": "JSONB" }
          ]
        }
        """;

    private final DatabaseSchemaService schemaService = mock(DatabaseSchemaService.class);
    private final DatabaseTableService tableService = mock(DatabaseTableService.class);
    private final ResourcePatternResolver resourceResolver = mock(ResourcePatternResolver.class);

    private TableInitialiser initialiser;

    @BeforeEach
    void setUp() {
        initialiser = new TableInitialiser(schemaService, tableService, resourceResolver, new ObjectMapper());
    }

    @Test
    void shouldScanTheDefinitionsFolderOnTheWholeClasspath() throws IOException {
        // Given
        givenResources();

        // When
        initialiser.afterPropertiesSet();

        // Then
        verify(resourceResolver).getResources("classpath*:definitions/tables/**/*.json");
    }

    @Test
    void shouldDoNothingWhenThereAreNoDefinitions() throws IOException {
        // Given
        givenResources();

        // When
        initialiser.afterPropertiesSet();

        // Then
        verifyNoInteractions(schemaService, tableService);
    }

    @Test
    void shouldCreateTheSchemaOnceBeforeCreatingEachTable() throws IOException {
        // Given
        givenResources(json("users.json", USERS), json("orders.json", ORDERS));

        // When
        initialiser.afterPropertiesSet();

        // Then
        final ArgumentCaptor<TableDefinition> tables = ArgumentCaptor.forClass(TableDefinition.class);
        final InOrder order = inOrder(schemaService, tableService);

        order.verify(schemaService).createSchema("app");
        order.verify(tableService, times(2)).createTable(tables.capture());
        verify(schemaService, times(1)).createSchema(anyString());

        assertThat(tables.getAllValues())
            .extracting(TableDefinition::getTable)
            .containsExactly("users", "orders");
    }

    @Test
    void shouldReadEveryFieldOfADefinition() throws IOException {
        // Given
        givenResources(json("users.json", USERS));

        // When
        initialiser.afterPropertiesSet();

        // Then
        final ArgumentCaptor<TableDefinition> captor = ArgumentCaptor.forClass(TableDefinition.class);
        verify(tableService).createTable(captor.capture());

        final TableDefinition users = captor.getValue();
        assertThat(users.getSchema()).isEqualTo("app");
        assertThat(users.getTable()).isEqualTo("users");

        final List<ColumnDefinition> columns = users.getColumns();
        assertThat(columns)
            .extracting(ColumnDefinition::getName, ColumnDefinition::getDataType, ColumnDefinition::getPrimaryKey)
            .containsExactly(
                tuple("id", SqlDataType.VARCHAR, true),
                tuple("email", SqlDataType.VARCHAR, false),
                tuple("created", SqlDataType.TIMESTAMP_WITHOUT_TIME_ZONE, false)
            );
    }

    @Test
    void shouldFailWhenADefinitionIsNotAJsonObject() throws IOException {
        // Given
        givenResources(json("list.json", "[]"));

        // When / Then
        assertThatThrownBy(initialiser::afterPropertiesSet)
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("must be a JSON object")
            .hasMessageContaining("list.json");

        verifyNoInteractions(schemaService, tableService);
    }

    @Test
    void shouldFailWhenADefinitionIsEmpty() throws IOException {
        // Given
        givenResources(json("empty.json", ""));

        // When / Then
        assertThatThrownBy(initialiser::afterPropertiesSet)
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("empty.json");
    }

    @Test
    void shouldFailWhenADefinitionIsInvalid() throws IOException {
        // Given
        final String badType = USERS.replace("\"VARCHAR\", \"primaryKey\"", "\"TEXT\", \"primaryKey\"");
        givenResources(json("bad-type.json", badType));

        // When / Then
        assertThatThrownBy(initialiser::afterPropertiesSet)
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Failed to read table definition")
            .hasMessageContaining("bad-type.json")
            .hasCauseInstanceOf(IOException.class);
    }

    @Test
    void shouldNotCreateAnythingWhenAnyDefinitionIsInvalid() throws IOException {
        // Given a valid definition followed by a broken one
        givenResources(json("users.json", USERS), json("broken.json", "{ \"table\": "));

        // When / Then: every file is parsed before the database is touched
        assertThatThrownBy(initialiser::afterPropertiesSet)
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("broken.json");

        verifyNoInteractions(schemaService, tableService);
    }

    @Test
    void shouldFailWhenTheClasspathCannotBeScanned() throws IOException {
        // Given
        when(resourceResolver.getResources(anyString())).thenThrow(new IOException("boom"));

        // When / Then
        assertThatThrownBy(initialiser::afterPropertiesSet)
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Failed to scan for table definitions")
            .hasCauseInstanceOf(IOException.class);

        verifyNoInteractions(schemaService, tableService);
    }

    private void givenResources(Resource... resources) throws IOException {
        when(resourceResolver.getResources(anyString())).thenReturn(resources);
    }

    private static Resource json(String name, String content) {
        return new ByteArrayResource(content.getBytes(StandardCharsets.UTF_8), name);
    }
}
