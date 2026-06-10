package com.kanemullett.model;

import java.util.List;

import org.immutables.value.Value.Immutable;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

@Immutable
@JsonSerialize(as=ImmutableFunction.class)
@JsonDeserialize(as=ImmutableFunction.class)
public interface Function extends Column {

    List<Object> getArgs();
}
