package com.kanemullett.model;

import org.immutables.value.Value.Immutable;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.kanemullett.model.type.ConditionOperator;

import static com.kanemullett.model.type.ConditionOperator.EQUAL;

import jakarta.annotation.Nullable;

@Immutable
@JsonSerialize(as=ImmutableQueryCondition.class)
@JsonDeserialize(as=ImmutableQueryCondition.class)
public interface QueryCondition {

    Column getColumn();

    ConditionOperator getOperator();

    @Nullable
    Object getValue();

    static QueryCondition of(Column column, Object value) {
        return ImmutableQueryCondition.builder()
            .column(column)
            .operator(EQUAL)
            .value(value)
            .build();
    }
}
