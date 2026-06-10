package com.kanemullett.model;

import java.util.List;

import org.immutables.value.Value.Immutable;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.kanemullett.model.type.SqlOperator;

import jakarta.annotation.Nullable;

@Immutable
@JsonSerialize(as=ImmutableUpdateRequest.class)
@JsonDeserialize(as=ImmutableUpdateRequest.class)
public interface UpdateRequest<T extends DatabaseRecord> {

    SqlOperator getOperation();

    Table getTable();

    @Nullable
    List<T> getRecords();

    @Nullable
    QueryConditionGroup getConditionGroup();
}
