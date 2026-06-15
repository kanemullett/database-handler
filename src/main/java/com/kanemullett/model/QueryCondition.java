package com.kanemullett.model;

import org.immutables.value.Value.Immutable;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.kanemullett.model.type.ConditionOperator;
import static com.kanemullett.model.type.ConditionOperator.EQUAL;

import jakarta.annotation.Nullable;

/**
 * Represents a single SQL condition, consisting of a {@link Column}, a
 * {@link ConditionOperator}, and an optional value.
 *
 * <p>Conditions are used within a {@link QueryConditionGroup} to build SQL
 * WHERE clauses. The value may be a scalar, a {@link Column} reference, or a
 * collection for use with the {@link ConditionOperator#IN} operator.
 *
 * <p>The {@link #of(Column, Object)} factory method creates an equality
 * condition by default:
 * <pre>
 * QueryCondition.of(Column.of("my_table", "my_column"), "my_value");
 * </pre>
 */
@Immutable
@JsonSerialize(as=ImmutableQueryCondition.class)
@JsonDeserialize(as=ImmutableQueryCondition.class)
public interface QueryCondition {

    /**
     * Returns the column this condition applies to.
     *
     * @return the {@link Column}.
     */
    Column getColumn();

    /**
     * Returns the operator used to compare the column against the value.
     *
     * @return the {@link ConditionOperator}.
     */
    ConditionOperator getOperator();

    /**
     * Returns the value to compare the column against. May be {@code null}
     * for operators that do not require a value, a scalar value, a
     * {@link Column} reference, or a collection for use with
     * {@link ConditionOperator#IN}.
     *
     * @return the condition value, or {@code null} if not set.
     */
    @Nullable
    Object getValue();

    /**
     * Creates a new {@code QueryCondition} applying an equality check between
     * the given column and value.
     *
     * @param column the column to apply the condition to.
     * @param value  the value to compare the column against.
     * @return the constructed {@code QueryCondition}.
     */
    static QueryCondition of(Column column, Object value) {
        return ImmutableQueryCondition.builder()
            .column(column)
            .operator(EQUAL)
            .value(value)
            .build();
    }
}
