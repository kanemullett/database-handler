package com.kanemullett.model;

import java.util.List;

import org.immutables.value.Value.Default;
import org.immutables.value.Value.Immutable;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.kanemullett.model.type.ConditionJoin;

@Immutable
@JsonSerialize(as=ImmutableQueryConditionGroup.class)
@JsonDeserialize(as=ImmutableQueryConditionGroup.class)
public interface QueryConditionGroup {

    List<QueryCondition> getConditions();

    @Default
    default ConditionJoin getJoin() {
        return ConditionJoin.AND;
    }

    static QueryConditionGroup of(QueryCondition... conditions) {
        return ImmutableQueryConditionGroup.builder()
            .addConditions(conditions)
            .build();
    }
}
