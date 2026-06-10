package com.kanemullett.model;

import org.immutables.value.Value.Immutable;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import jakarta.annotation.Nullable;

@Immutable
@JsonSerialize(as=ImmutableTable.class)
@JsonDeserialize(as=ImmutableTable.class)
public interface Table {

    String getSchema();

    String getTable();

    @Nullable
    String getAlias();

    static Table of(String schema, String table) {
        return ImmutableTable.builder()
            .schema(schema)
            .table(table)
            .build();
    }

    static Table of(String schema, String table, String alias) {
        return ImmutableTable.builder()
            .schema(schema)
            .table(table)
            .alias(alias)
            .build();
    }
}
