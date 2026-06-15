package com.kanemullett.model;

import java.util.List;

import org.immutables.value.Value.Immutable;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import jakarta.annotation.Nullable;

/**
 * Represents a database column reference, consisting of one or more name
 * parts and an optional alias.
 *
 * <p>Parts are used to build qualified column names. For example, a column
 * with parts {@code ["my_table", "my_column"]} would be rendered as
 * {@code "my_table"."my_column"} in SQL.
 *
 * <p>Instances are created via the {@link #of(String...)} factory method:
 * <pre>
 * Column.of("my_table", "my_column");
 * </pre>
 */
@Immutable
@JsonSerialize(
    as=ImmutableColumn.class
)
@JsonDeserialize(
    as=ImmutableColumn.class
)
public interface Column {

    /**
     * Returns the parts of the column name, used to build a qualified
     * column reference.
     *
     * @return the list of name parts.
     */
    List<String> getParts();

    /**
     * Returns the optional alias for this column, used in SELECT clauses.
     *
     * @return the alias, or {@code null} if not set.
     */
    @Nullable
    String getAlias();

    /**
     * Creates a new {@code Column} with the given name parts.
     *
     * @param parts the parts of the column name.
     * @return the constructed {@code Column}.
     */
    static Column of(String... parts) {
        return ImmutableColumn.builder()
            .addParts(parts)
            .build();
    }
}
