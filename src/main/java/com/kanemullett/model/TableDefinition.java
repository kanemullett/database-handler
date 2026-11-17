package com.kanemullett.model;

import java.util.List;
import java.util.stream.Stream;

import org.immutables.value.Value.Check;
import org.immutables.value.Value.Immutable;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.kanemullett.model.type.SqlDataType;

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
 * <p>Every table has an {@value #ID_COLUMN} column. If the columns given
 * when building do not include one, a {@code VARCHAR} primary key column
 * named {@value #ID_COLUMN} is added as the first column. A column named
 * {@value #ID_COLUMN} that is supplied explicitly is kept as given.
 *
 * <p>Example usage:
 * <pre>
 * ImmutableTableDefinition.builder()
 *     .schema("my_schema")
 *     .table("my_table")
 *     .addColumns(ColumnDefinition.of("name", SqlDataType.VARCHAR))
 *     .build();
 * // columns: id (VARCHAR, primary key), name (VARCHAR)
 * </pre>
 */
@Immutable
@JsonSerialize(as=ImmutableTableDefinition.class)
@JsonDeserialize(as=ImmutableTableDefinition.class)
public interface TableDefinition {

    /** Name of the id column every table has. */
    String ID_COLUMN = "id";

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

    /**
     * Ensures the definition has an {@value #ID_COLUMN} column, adding a
     * {@code VARCHAR} primary key column of that name as the first column if
     * none was provided.
     *
     * <p>Called by Immutables when the definition is built, including when it
     * is deserialised from JSON, so every {@code TableDefinition} has an
     * {@value #ID_COLUMN} column.
     *
     * @return this definition if it already has an {@value #ID_COLUMN}
     *         column, otherwise a copy with one added.
     */
    @Check
    default TableDefinition withIdColumn() {
        final boolean hasIdColumn = getColumns().stream()
            .anyMatch(column -> column.getName().equals(ID_COLUMN));

        if (hasIdColumn) {
            return this;
        }

        final ColumnDefinition idColumn = ImmutableColumnDefinition.builder()
            .name(ID_COLUMN)
            .dataType(SqlDataType.VARCHAR)
            .primaryKey(true)
            .build();

        return ImmutableTableDefinition.builder()
            .from(this)
            .columns(Stream.concat(Stream.of(idColumn), getColumns().stream())::iterator)
            .build();
    }
}
