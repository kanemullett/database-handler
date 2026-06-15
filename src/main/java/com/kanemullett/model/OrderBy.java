package com.kanemullett.model;

import org.immutables.value.Value.Default;
import org.immutables.value.Value.Immutable;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.kanemullett.model.type.OrderDirection;

/**
 * Represents a SQL ORDER BY clause, specifying a {@link Column} to sort by
 * and an {@link OrderDirection}.
 *
 * <p>The direction defaults to {@link OrderDirection#ASCENDING} if not
 * explicitly provided.
 *
 * <p>Instances are created via the {@link #of(Column)} factory method:
 * <pre>
 * OrderBy.of(Column.of("my_table", "my_column"));
 * </pre>
 */
@Immutable
@JsonSerialize(as=ImmutableOrderBy.class)
@JsonDeserialize(as=ImmutableOrderBy.class)
public interface OrderBy {

    /**
     * Returns the column to sort results by.
     *
     * @return the {@link Column} to order by.
     */
    Column getColumn();

    /**
     * Returns the direction to sort results in. Defaults to
     * {@link OrderDirection#ASCENDING} if not explicitly set.
     *
     * @return the {@link OrderDirection}.
     */
    @Default
    default OrderDirection getDirection() {
        return OrderDirection.ASCENDING;
    }

    /**
     * Creates a new {@code OrderBy} for the given column with the default
     * ascending direction.
     *
     * @param column the column to order by.
     * @return the constructed {@code OrderBy}.
     */
    static OrderBy of(Column column) {
        return ImmutableOrderBy.builder()
            .column(column)
            .build();
    }
}
