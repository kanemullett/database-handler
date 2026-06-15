package com.kanemullett.model;

import org.immutables.value.Value.Default;
import org.immutables.value.Value.Immutable;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.kanemullett.model.type.SqlDataType;

/**
 * Represents the definition of a database column, including its name, data
 * type, and whether it is a primary key.
 *
 * <p>Used as part of a {@link TableDefinition} to define the structure of a
 * database table.
 *
 * <p>Instances are created via the {@link #of(String, SqlDataType)} factory
 * method:
 * <pre>
 * ColumnDefinition.of("id", SqlDataType.VARCHAR);
 * </pre>
 */
@Immutable
@JsonSerialize(as=ImmutableColumnDefinition.class)
@JsonDeserialize(as=ImmutableColumnDefinition.class)
public interface ColumnDefinition {

    /**
     * Returns the name of the column.
     *
     * @return the column name.
     */
    String getName();

    /**
     * Returns the SQL data type of the column.
     *
     * @return the {@link SqlDataType}.
     */
    SqlDataType getDataType();

    /**
     * Returns whether this column is a primary key. Defaults to {@code false}.
     *
     * @return {@code true} if this column is a primary key, {@code false}
     *         otherwise.
     */
    @Default
    default boolean getPrimaryKey() {
        return false;
    }

    /**
     * Creates a new {@code ColumnDefinition} with the given name and data
     * type, with {@code primaryKey} defaulting to {@code false}.
     *
     * @param name     the name of the column.
     * @param dataType the SQL data type of the column.
     * @return the constructed {@code ColumnDefinition}.
     */
    static ColumnDefinition of(String name, SqlDataType dataType) {
        return ImmutableColumnDefinition.builder()
            .name(name)
            .dataType(dataType)
            .build();
    }
}
