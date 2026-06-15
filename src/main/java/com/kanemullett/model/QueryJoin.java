package com.kanemullett.model;

import org.immutables.value.Value.Immutable;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import jakarta.annotation.Nullable;

/**
 * Represents a SQL join against a subquery, extending {@link Join} with a
 * nested {@link QueryRequest} and an optional alias.
 *
 * <p>The nested {@link QueryRequest} is recursively built into a subquery
 * by the {@link com.kanemullett.function.QueryBuilderFunction}. UNION set
 * operations between subqueries are expressed by chaining {@code QueryJoin}
 * instances with {@link com.kanemullett.model.type.JoinType#UNION} inside
 * the nested query's joins.
 *
 * <p>Example usage:
 * <pre>
 * ImmutableQueryJoin.builder()
 *     .query(ImmutableQueryRequest.builder()
 *         .table(Table.of("my_schema", "my_table"))
 *         .recordClass(DatabaseRecord.class)
 *         .build())
 *     .alias("subquery")
 *     .joinType(JoinType.INNER)
 *     .joinCondition(QueryCondition.of(
 *         Column.of("my_table", "id"),
 *         Column.of("subquery", "id")
 *     ))
 *     .build();
 * </pre>
 *
 * @param <T> the type of {@link DatabaseRecord} the subquery is built for.
 */
@Immutable
@JsonSerialize(as=ImmutableQueryJoin.class)
@JsonDeserialize(as=ImmutableQueryJoin.class)
public interface QueryJoin<T extends DatabaseRecord> extends Join {

    /**
     * Returns the nested {@link QueryRequest} to be built into a subquery.
     *
     * @return the subquery request.
     */
    QueryRequest<T> getQuery();

    /**
     * Returns the optional alias for this subquery, used to reference it in
     * join conditions and column selections.
     *
     * @return the alias, or {@code null} if not set.
     */
    @Nullable
    String getAlias();
}
