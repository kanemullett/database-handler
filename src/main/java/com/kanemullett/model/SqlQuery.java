package com.kanemullett.model;

import java.util.List;

import org.immutables.value.Value.Default;
import org.immutables.value.Value.Immutable;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.kanemullett.model.type.SqlOperator;

import jakarta.annotation.Nullable;

/**
 * Represents a SQL query, previously used as an intermediate object between
 * request models and the query builder.
 *
 * @deprecated This class has been superseded by {@link QueryRequest} for
 *             SELECT operations and {@link UpdateRequest} for INSERT, UPDATE,
 *             and DELETE operations, which are passed directly to the
 *             respective builder functions without an intermediate object.
 *             This class is retained only for backwards compatibility and will
 *             be removed in a future release.
 *
 * @param <T> the type of {@link DatabaseRecord} the query is built for.
 */
@Deprecated
@Immutable
@JsonSerialize(as=ImmutableSqlQuery.class)
@JsonDeserialize(as=ImmutableSqlQuery.class)
public interface SqlQuery<T extends DatabaseRecord> {

    /**
     * Returns the SQL operator for this query. Defaults to
     * {@link SqlOperator#SELECT}.
     *
     * @return the {@link SqlOperator}.
     */
    @Deprecated
    @Default
    default SqlOperator getOperator() {
        return SqlOperator.SELECT;
    }

    /**
     * Returns whether the query should return distinct results. Defaults to
     * {@code false}.
     *
     * @return {@code true} if distinct, {@code false} otherwise.
     */
    @Deprecated
    @Default
    default boolean getDistinct() {
        return false;
    }

    /**
     * Returns the table to query from.
     *
     * @return the {@link Table}.
     */
    @Deprecated
    Table getTable();

    /**
     * Returns the columns to select, or {@code null} for {@code SELECT *}.
     *
     * @return the list of {@link Column} objects, or {@code null}.
     */
    @Deprecated
    @Nullable
    List<Column> getColumns();

    /**
     * Returns the joins to apply, or {@code null} if none.
     *
     * @return the list of {@link Join} objects, or {@code null}.
     */
    @Deprecated
    @Nullable
    List<Join> getJoins();

    /**
     * Returns the condition group for the WHERE clause, or {@code null} if
     * not required.
     *
     * @return the {@link QueryConditionGroup}, or {@code null}.
     */
    @Deprecated
    @Nullable
    QueryConditionGroup getConditionGroup();

    /**
     * Returns the records for INSERT or UPDATE operations, or {@code null}
     * if not applicable.
     *
     * @return the list of records, or {@code null}.
     */
    @Deprecated
    @Nullable
    List<T> getRecords();

    /**
     * Returns the GROUP BY clause, or {@code null} if not required.
     *
     * @return the {@link GroupBy}, or {@code null}.
     */
    @Deprecated
    @Nullable
    GroupBy getGroupBy();

    /**
     * Returns the ORDER BY clauses, or {@code null} if not required.
     *
     * @return the list of {@link OrderBy} objects, or {@code null}.
     */
    @Deprecated
    @Nullable
    List<OrderBy> getOrderBy();
}
