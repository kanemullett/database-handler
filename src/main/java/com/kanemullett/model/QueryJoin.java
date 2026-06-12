package com.kanemullett.model;

import org.immutables.value.Value.Immutable;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import jakarta.annotation.Nullable;

@Immutable
@JsonSerialize(as=ImmutableQueryJoin.class)
@JsonDeserialize(as=ImmutableQueryJoin.class)
public interface QueryJoin<T extends DatabaseRecord> extends Join {

    QueryRequest<T> getQuery();

    @Nullable
    String getAlias();
}
