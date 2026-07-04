package com.kanemullett.model;

import java.util.Map;
import java.util.UUID;

import org.immutables.value.Value.Default;
import org.immutables.value.Value.Immutable;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

/**
 * Base interface for all database records managed by the Database Handler
 * module.
 *
 * <p>All domain record types used with {@link QueryRequest} and
 * {@link UpdateRequest} should extend this interface to ensure they carry
 * a unique identifier.
 *
 * <p>The {@code id} field defaults to a randomly generated UUID string if
 * not explicitly provided, mirroring the behaviour of the Python
 * {@code uuid4} default factory.
 */
@Immutable
@JsonSerialize(as=ImmutableDatabaseRecord.class)
@JsonDeserialize(as=ImmutableDatabaseRecord.class)
public interface DatabaseRecord {

    /**
     * Returns the unique identifier for this record. Defaults to a randomly
     * generated UUID string if not explicitly set.
     *
     * @return the record's unique identifier.
     */
    @Default
    default String getId() {
        return UUID.randomUUID().toString();
    };

    @JsonAnyGetter
    Map<String, Object> getData();
}
