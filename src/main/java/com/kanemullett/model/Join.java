package com.kanemullett.model;

import org.immutables.value.Value.Immutable;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.kanemullett.model.type.JoinType;

import jakarta.annotation.Nullable;

/**
 * Base interface representing a SQL join, carrying a {@link JoinType} and an
 * optional join condition.
 *
 * <p>Extended by {@link TableJoin} for joins against a table and
 * {@link QueryJoin} for joins against a subquery.
 */
@Immutable
@JsonSerialize(as=ImmutableJoin.class)
@JsonDeserialize(as=ImmutableJoin.class)
public interface Join {

    /**
     * Returns the condition used to join the tables or subqueries. May be
     * {@code null} for join types that do not require a condition such as
     * CROSS joins.
     *
     * @return the {@link QueryCondition}, or {@code null} if not set.
     */
    @Nullable
    QueryCondition getJoinCondition();

    /**
     * Returns the type of join to perform.
     *
     * @return the {@link JoinType}.
     */
    JoinType getJoinType();
}
