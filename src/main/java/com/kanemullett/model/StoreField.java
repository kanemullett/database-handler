package com.kanemullett.model;

import org.immutables.value.Value.Immutable;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import jakarta.annotation.Nullable;

@Immutable
@JsonSerialize(as=ImmutableStoreField.class)
@JsonDeserialize(as=ImmutableStoreField.class)
public interface StoreField {

    @Nullable
    String getAlias();
}
