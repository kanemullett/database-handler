package com.kanemullett.model;

import java.util.List;

import org.immutables.value.Value.Default;
import org.immutables.value.Value.Immutable;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.kanemullett.model.type.SqlOperator;

import jakarta.annotation.Nullable;

@Immutable
@JsonSerialize(as=ImmutableSqlQuery.class)
@JsonDeserialize(as=ImmutableSqlQuery.class)
public interface SqlQuery<T extends DatabaseRecord> {

    @Default
    default SqlOperator getOperator() {
        return SqlOperator.SELECT;
    }

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
    List<T> getRecords();

    @Nullable
    GroupBy getGroupBy();

    @Nullable
    List<OrderBy> getOrderBy();
}
