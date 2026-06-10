package com.kanemullett.model;

import java.util.List;

import org.immutables.value.Value.Immutable;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import jakarta.annotation.Nullable;

@Immutable
@JsonSerialize(as=ImmutableQueryResponse.class)
@JsonDeserialize(as=ImmutableQueryResponse.class)
public interface QueryResponse<T extends DatabaseRecord> {

    String getReferenceId();

    int getRecordCount();

    @Nullable
    List<T> getRecords();
}
