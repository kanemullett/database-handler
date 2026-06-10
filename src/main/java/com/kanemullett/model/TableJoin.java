package com.kanemullett.model;

import org.immutables.value.Value.Immutable;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.kanemullett.model.type.JoinType;

@Immutable
@JsonSerialize(as=ImmutableTableJoin.class)
@JsonDeserialize(as=ImmutableTableJoin.class)
public interface TableJoin extends Join {

    Table getTable();

    static TableJoin of(Table table, QueryCondition joinCondition, JoinType joinType) {
        return ImmutableTableJoin.builder()
            .table(table)
            .joinCondition(joinCondition)
            .joinType(joinType)
            .build();
    }
}
