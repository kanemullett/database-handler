package com.kanemullett.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kanemullett.model.type.SqlDataType;

class TableDefinitionTest {

    @Test
    void shouldAddIdPrimaryKeyColumnFirstWhenNotProvided() {
        // When
        final TableDefinition tableDefinition = ImmutableTableDefinition.builder()
            .schema("test-schema")
            .table("test-table")
            .addColumns(ColumnDefinition.of("name", SqlDataType.VARCHAR))
            .addColumns(ColumnDefinition.of("age", SqlDataType.INTEGER))
            .build();

        // Then
        assertThat(tableDefinition.getColumns())
            .extracting(
                ColumnDefinition::getName,
                ColumnDefinition::getDataType,
                ColumnDefinition::getPrimaryKey,
                ColumnDefinition::isNullable
            )
            .containsExactly(
                tuple("id", SqlDataType.VARCHAR, true, false),
                tuple("name", SqlDataType.VARCHAR, false, false),
                tuple("age", SqlDataType.INTEGER, false, false)
            );
    }

    @Test
    void shouldAddIdPrimaryKeyColumnWhenThereAreNoColumns() {
        // When
        final TableDefinition tableDefinition = ImmutableTableDefinition.builder()
            .schema("test-schema")
            .table("test-table")
            .build();

        // Then
        assertThat(tableDefinition.getColumns())
            .extracting(ColumnDefinition::getName, ColumnDefinition::getPrimaryKey)
            .containsExactly(tuple("id", true));
    }

    @Test
    void shouldKeepIdColumnWhenProvided() {
        // Given
        final ColumnDefinition providedId = ImmutableColumnDefinition.builder()
            .name("id")
            .dataType(SqlDataType.INTEGER)
            .primaryKey(true)
            .build();

        // When
        final TableDefinition tableDefinition = ImmutableTableDefinition.builder()
            .schema("test-schema")
            .table("test-table")
            .addColumns(ColumnDefinition.of("name", SqlDataType.VARCHAR))
            .addColumns(providedId)
            .build();

        // Then
        assertThat(tableDefinition.getColumns())
            .containsExactly(ColumnDefinition.of("name", SqlDataType.VARCHAR), providedId);
    }

    @Test
    void shouldAddIdPrimaryKeyColumnWhenDeserialisedFromJson() throws Exception {
        // Given
        final String json = """
            {
              "schema": "app",
              "table": "users",
              "columns": [
                { "name": "email", "dataType": "VARCHAR" }
              ]
            }
            """;

        // When
        final TableDefinition tableDefinition = new ObjectMapper().readValue(json, TableDefinition.class);

        // Then
        assertThat(tableDefinition.getColumns())
            .extracting(ColumnDefinition::getName, ColumnDefinition::getPrimaryKey)
            .containsExactly(tuple("id", true), tuple("email", false));
    }
}
