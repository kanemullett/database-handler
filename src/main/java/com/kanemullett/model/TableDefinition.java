package com.kanemullett.model;

import java.util.List;

import org.immutables.value.Value.Immutable;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

/**
 * Represents the definition of a database table, including its schema, name,
 * and column definitions.
 *
 * <p>Used as input to the
 * {@link com.kanemullett.function.TableBuilderFunction} to build
 * {@code CREATE TABLE IF NOT EXISTS} statements, and to the
 * {@link com.kanemullett.service.DatabaseTableService} to create or delete
 * tables.
 *
 * <p>Example usage:
 * <pre>
 * ImmutableTableDefinition.builder()
 *     .schema("my_schema")
 *     .table("my_table")
 *     .addColumns(ColumnDefinition.of("id", SqlDataType.VARCHAR))
 *     .addColumns(ColumnDefinition.of("name", SqlDataType.VARCHAR))
 *     .build();
 * </pre>
 */
@Immutable
@JsonSerialize(as=ImmutableTableDefinition.class)
@JsonDeserialize(as=ImmutableTableDefinition.class)
public interface TableDefinition {

    /**
     * Returns the schema the table belongs to.
     *
     * @return the schema name.
     */
    String getSchema();

    /**
     * Returns the name of the table.
     *
     * @return the table name.
     */
    String getTable();

    /**
     * Returns the column definitions that make up the table's structure.
     *
     * @return the list of {@link ColumnDefinition} objects.
     */
    List<ColumnDefinition> getColumns();
}
