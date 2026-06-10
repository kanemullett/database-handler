package com.kanemullett.model;

import org.immutables.value.Value.Immutable;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.kanemullett.model.type.JoinType;

import jakarta.annotation.Nullable;

@Immutable
@JsonSerialize(as=ImmutableJoin.class)
@JsonDeserialize(as=ImmutableJoin.class)
public interface Join {

    @Nullable
    QueryCondition getJoinCondition();

    JoinType getJoinType();
}
