package com.kanemullett.model;

import java.util.List;

import org.immutables.value.Value.Immutable;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

/**
 * Represents a SQL function call, extending {@link Column} to carry a
 * function name (via {@link #getParts()}) and an optional alias, with
 * additional support for function arguments.
 *
 * <p>The {@link #getParts()} field is used to define the function name,
 * optionally schema-qualified. A single part represents an unqualified
 * function name (e.g. {@code AGG_FUNCTION}), while two parts represent a
 * schema-qualified name (e.g. {@code ["my_schema", "my_function"]}).
 *
 * <p>Arguments may be scalar values, {@link Column} references, or nested
 * {@code Function} instances to support composed function calls.
 *
 * <p>Example usage:
 * <pre>
 * ImmutableFunction.builder()
 *     .addParts("AGG_FUNCTION")
 *     .args(List.of(
 *         ImmutableFunction.builder()
 *             .addParts("my_schema", "my_function")
 *             .args(List.of(Column.of("my_table", "my_column")))
 *             .build()
 *     ))
 *     .alias("result")
 *     .build();
 * </pre>
 */
@Immutable
@JsonSerialize(as=ImmutableFunction.class)
@JsonDeserialize(as=ImmutableFunction.class)
public interface Function extends Column {

    /**
     * Returns the arguments to be passed to this function. Arguments may be
     * scalar values, {@link Column} references, or nested {@link Function}
     * instances.
     *
     * @return the list of function arguments.
     */
    List<Object> getArgs();
}
