package com.kanemullett.model;

import org.immutables.value.Value.Immutable;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

@Immutable
@JsonSerialize(as=ImmutableDatabaseRecord.class)
@JsonDeserialize(as=ImmutableDatabaseRecord.class)
public interface DatabaseRecord {

    String getId();
}
