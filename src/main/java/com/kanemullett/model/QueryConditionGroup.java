package com.kanemullett.model;

import java.util.List;

import org.immutables.value.Value.Default;
import org.immutables.value.Value.Immutable;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.kanemullett.model.type.ConditionJoin;


/**
 * Represents a group of SQL conditions combined by a logical
 * {@link ConditionJoin} operator, used to build WHERE clauses in
 * {@link QueryRequest} and DELETE {@link UpdateRequest} objects.
 *
 * <p>The join operator defaults to {@link ConditionJoin#AND} if not
 * explicitly provided.
 *
 * <p>Instances are created via the {@link #of(QueryCondition...)} factory
 * method:
 * <pre>
 * QueryConditionGroup.of(
 *     QueryCondition.of(Column.of("my_column"), "my_value"),
 *     QueryCondition.of(Column.of("other_column"), 42)
 * );
 * </pre>
 */
@Immutable
@JsonSerialize(as=ImmutableQueryConditionGroup.class)
@JsonDeserialize(as=ImmutableQueryConditionGroup.class)
public interface QueryConditionGroup {

    /**
     * Returns the list of conditions in this group.
     *
     * @return the list of {@link QueryCondition} objects.
     */
    List<QueryCondition> getConditions();

    /**
     * Returns the logical operator used to combine the conditions in this
     * group. Defaults to {@link ConditionJoin#AND} if not explicitly set.
     *
     * @return the {@link ConditionJoin}.
     */
    @Default
    default ConditionJoin getJoin() {
        return ConditionJoin.AND;
    }

    /**
     * Creates a new {@code QueryConditionGroup} with the given conditions,
     * using the default {@link ConditionJoin#AND} operator.
     *
     * @param conditions the conditions to include in the group.
     * @return the constructed {@code QueryConditionGroup}.
     */
    static QueryConditionGroup of(QueryCondition... conditions) {
        return ImmutableQueryConditionGroup.builder()
            .addConditions(conditions)
            .build();
    }
}
