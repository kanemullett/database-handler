package com.kanemullett.model;

import java.util.List;

import org.immutables.value.Value.Immutable;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

/**
 * Represents a SQL GROUP BY clause, containing one or more {@link Column}
 * references to group query results by.
 *
 * <p>Instances are created via the {@link #of(Column...)} factory method:
 * <pre>
 * GroupBy.of(Column.of("my_table", "my_column"));
 * </pre>
 */
@Immutable
@JsonSerialize(as=ImmutableGroupBy.class)
@JsonDeserialize(as=ImmutableGroupBy.class)
public interface GroupBy {

    /**
     * Returns the columns to group results by.
     *
     * @return the list of {@link Column} references.
     */
    List<Column> getColumns();

    /**
     * Creates a new {@code GroupBy} with the given columns.
     *
     * @param parts the columns to group by.
     * @return the constructed {@code GroupBy}.
     */
    static GroupBy of(Column... parts) {
        return ImmutableGroupBy.builder()
            .addColumns(parts)
            .build();
    }
}
