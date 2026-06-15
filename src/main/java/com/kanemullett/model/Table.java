package com.kanemullett.model;

import org.immutables.value.Value.Immutable;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import jakarta.annotation.Nullable;

/**
 * Represents a database table reference, consisting of a schema name, table
 * name, and an optional alias.
 *
 * <p>Used across {@link QueryRequest}, {@link UpdateRequest}, and
 * {@link TableDefinition} to identify the target table for database
 * operations.
 *
 * <p>Instances are created via the {@link #of(String, String)} or
 * {@link #of(String, String, String)} factory methods:
 * <pre>
 * Table.of("my_schema", "my_table");
 * Table.of("my_schema", "my_table", "my_alias");
 * </pre>
 */
@Immutable
@JsonSerialize(as=ImmutableTable.class)
@JsonDeserialize(as=ImmutableTable.class)
public interface Table {

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
     * Returns the optional alias for this table, used to reference it in
     * column selections and join conditions.
     *
     * @return the alias, or {@code null} if not set.
     */
    @Nullable
    String getAlias();

    /**
     * Creates a new {@code Table} with the given schema and table name,
     * with no alias.
     *
     * @param schema the schema the table belongs to.
     * @param table  the name of the table.
     * @return the constructed {@code Table}.
     */
    static Table of(String schema, String table) {
        return ImmutableTable.builder()
            .schema(schema)
            .table(table)
            .build();
    }

    /**
     * Creates a new {@code Table} with the given schema, table name, and
     * alias.
     *
     * @param schema the schema the table belongs to.
     * @param table  the name of the table.
     * @param alias  the alias to use for this table.
     * @return the constructed {@code Table}.
     */
    static Table of(String schema, String table, String alias) {
        return ImmutableTable.builder()
            .schema(schema)
            .table(table)
            .alias(alias)
            .build();
    }
}
