package com.kanemullett.util;

import java.util.List;
import java.util.stream.Collectors;

import org.jooq.Condition;
import org.jooq.Field;
import org.jooq.impl.DSL;

import com.kanemullett.model.Column;
import com.kanemullett.model.QueryCondition;
import com.kanemullett.model.QueryConditionGroup;

/**
 * Utility class providing shared methods for building jOOQ {@link Condition}
 * objects from query model types.
 *
 * <p>Used by both {@link com.kanemullett.function.QueryBuilderFunction} and
 * {@link com.kanemullett.function.UpdateBuilderFunction} to avoid duplication
 * of condition building logic.
 */
public class BuilderUtils {

    /**
     * Builds a jOOQ {@link Condition} from a {@link QueryConditionGroup},
     * combining all conditions in the group using the group's
     * {@link com.kanemullett.model.type.ConditionJoin} operator.
     *
     * @param group the condition group to build from.
     * @return the constructed {@link Condition}.
     */
    public static Condition buildConditionGroup(QueryConditionGroup group) {
        final List<Condition> conditions = group.getConditions().stream()
            .map(con -> buildCondition(con))
            .collect(Collectors.toList());

        return switch (group.getJoin()) {
            case AND -> DSL.and(conditions);
            case OR -> DSL.or(conditions);
        };
    }

    /**
     * Builds a jOOQ {@link Condition} from a single {@link QueryCondition}.
     *
     * <p>If the condition's value is a {@link Column} reference, it is
     * treated as a field reference rather than a scalar value, allowing
     * column-to-column comparisons in join conditions and WHERE clauses.
     *
     * @param queryCondition the condition to build from.
     * @return the constructed {@link Condition}.
     */
    public static Condition buildCondition(QueryCondition queryCondition) {
        final Field<Object> field = DSL.field(DSL.name(
            queryCondition.getColumn().getParts().toArray(String[]::new)
        ));

        final Object value = queryCondition.getValue();

        final Field<Object> valueField = value instanceof Column col
            ? DSL.field(DSL.name(col.getParts().toArray(String[]::new)))
            : null;

        return switch (queryCondition.getOperator()) {
            case EQUAL -> valueField != null ? field.eq(valueField) : field.eq(value);
            case LESS_THAN -> valueField != null ? field.lt(valueField) : field.lt(value);
            case GREATER_THAN -> valueField != null ? field.gt(valueField) : field.gt(value);
            case IN -> field.in(value);
        };
    }
}
