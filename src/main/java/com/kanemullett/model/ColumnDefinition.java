package com.kanemullett.model;

import org.immutables.value.Value.Default;
import org.immutables.value.Value.Immutable;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.kanemullett.model.type.SqlDataType;

@Immutable
@JsonSerialize(as=ImmutableColumnDefinition.class)
@JsonDeserialize(as=ImmutableColumnDefinition.class)
public interface ColumnDefinition {

    String getName();

    SqlDataType getDataType();

    @Default
    default boolean getPrimaryKey() {
        return false;
    }

    static ColumnDefinition of(String name, SqlDataType dataType) {
        return ImmutableColumnDefinition.builder()
            .name(name)
            .dataType(dataType)
            .build();
    }
}
