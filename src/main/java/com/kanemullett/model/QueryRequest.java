package com.kanemullett.model;

import java.util.List;

import org.immutables.value.Value.Default;
import org.immutables.value.Value.Immutable;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import jakarta.annotation.Nullable;

@Immutable
@JsonSerialize(as=ImmutableQueryRequest.class)
@JsonDeserialize(as=ImmutableQueryRequest.class)
public interface QueryRequest<T extends DatabaseRecord> {

    @Default
    default boolean getDistinct() {
        return false;
    }

    Table getTable();

    @Nullable
    List<Column> getColumns();

    @Nullable
    List<Join> getJoins();

    @Nullable
    QueryConditionGroup getConditionGroup();

    @Nullable
    GroupBy getGroupBy();

    @Nullable
    OrderBy getOrderBy();

    Class<T> getRecordClass();
}
