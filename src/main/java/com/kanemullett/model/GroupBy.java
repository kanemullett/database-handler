package com.kanemullett.model;

import java.util.List;

import org.immutables.value.Value.Immutable;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

@Immutable
@JsonSerialize(as=ImmutableGroupBy.class)
@JsonDeserialize(as=ImmutableGroupBy.class)
public interface GroupBy {

    List<Column> getColumns();

    static GroupBy of(Column... parts) {
        return ImmutableGroupBy.builder()
            .addColumns(parts)
            .build();
    }
}
