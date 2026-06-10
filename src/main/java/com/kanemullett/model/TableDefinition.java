package com.kanemullett.model;

import java.util.List;

import org.immutables.value.Value.Immutable;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

@Immutable
@JsonSerialize(as=ImmutableTableDefinition.class)
@JsonDeserialize(as=ImmutableTableDefinition.class)
public interface TableDefinition {

    String getSchema();

    String getTable();

    List<ColumnDefinition> getColumns();
}
