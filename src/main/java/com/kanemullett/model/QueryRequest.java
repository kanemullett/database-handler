package com.kanemullett.model;

import java.util.List;

import org.immutables.value.Value.Default;
import org.immutables.value.Value.Immutable;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import jakarta.annotation.Nullable;

/**
 * Represents a SQL SELECT query request, encapsulating all the components
 * required to build and execute a SELECT statement via the
 * {@link com.kanemullett.function.QueryBuilderFunction}.
 *
 * <p>The {@link #getRecordClass()} field is used by the
 * {@link com.kanemullett.service.DatabaseQueryService} to map jOOQ results
 * back into typed {@link DatabaseRecord} instances.
 *
 * <p>Example usage:
 * <pre>
 * ImmutableQueryRequest.builder()
 *     .table(Table.of("my_schema", "my_table"))
 *     .recordClass(MyRecord.class)
 *     .build();
 * </pre>
 *
 * @param <T> the type of {@link DatabaseRecord} the query returns.
 */
@Immutable
@JsonSerialize(as=ImmutableQueryRequest.class)
@JsonDeserialize(as=ImmutableQueryRequest.class)
public interface QueryRequest<T extends DatabaseRecord> {

    /**
     * Returns whether the query should return distinct results. Defaults to
     * {@code false}.
     *
     * @return {@code true} if distinct, {@code false} otherwise.
     */
    @Default
    default boolean getDistinct() {
        return false;
    }

    /**
     * Returns the table to query from.
     *
     * @return the {@link Table}.
     */
    Table getTable();

    /**
     * Returns the columns to select. If {@code null}, all columns are
     * selected ({@code SELECT *}).
     *
     * @return the list of {@link Column} objects, or {@code null}.
     */
    @Nullable
    List<Column> getColumns();

    /**
     * Returns the joins to apply to the query, or {@code null} if none.
     *
     * @return the list of {@link Join} objects, or {@code null}.
     */
    @Nullable
    List<Join> getJoins();

    /**
     * Returns the condition group used to build the WHERE clause, or
     * {@code null} if no conditions are required.
     *
     * @return the {@link QueryConditionGroup}, or {@code null}.
     */
    @Nullable
    QueryConditionGroup getConditionGroup();

    /**
     * Returns the GROUP BY clause, or {@code null} if not required.
     *
     * @return the {@link GroupBy}, or {@code null}.
     */
    @Nullable
    GroupBy getGroupBy();

    /**
     * Returns the ORDER BY clause, or {@code null} if not required.
     *
     * @return the {@link OrderBy}, or {@code null}.
     */
    @Nullable
    OrderBy getOrderBy();

    /**
     * Returns the class of the {@link DatabaseRecord} type that query results
     * should be mapped to.
     *
     * @return the record class.
     */
    Class<T> getRecordClass();
}
