package com.kanemullett.model;

import org.immutables.value.Value.Default;
import org.immutables.value.Value.Immutable;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.kanemullett.model.type.OrderDirection;

@Immutable
@JsonSerialize(as=ImmutableOrderBy.class)
@JsonDeserialize(as=ImmutableOrderBy.class)
public interface OrderBy {

    Column getColumn();

    @Default
    default OrderDirection getDirection() {
        return OrderDirection.ASCENDING;
    }

    static OrderBy of(Column column) {
        return ImmutableOrderBy.builder()
            .column(column)
            .build();
    }
}
