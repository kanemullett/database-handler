package com.kanemullett.model;

import org.immutables.value.Value.Immutable;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

@Immutable
@JsonSerialize(as=ImmutableConstantValue.class)
@JsonDeserialize(as=ImmutableConstantValue.class)
public interface ConstantValue extends StoreField {

    Object getValue();
}
