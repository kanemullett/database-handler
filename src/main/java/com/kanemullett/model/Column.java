package com.kanemullett.model;

import java.util.List;

import org.immutables.value.Value.Immutable;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import jakarta.annotation.Nullable;

@Immutable
@JsonSerialize(
    as=ImmutableColumn.class
)
@JsonDeserialize(
    as=ImmutableColumn.class
)
public interface Column {

    List<String> getParts();

    @Nullable
    String getAlias();

    static Column of(String... parts) {
        return ImmutableColumn.builder()
            .addParts(parts)
            .build();
    }
}
