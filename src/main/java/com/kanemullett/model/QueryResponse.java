package com.kanemullett.model;

import java.util.List;

import org.immutables.value.Value.Immutable;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import jakarta.annotation.Nullable;

/**
 * Represents the response from a database query or update operation,
 * returned by the {@link com.kanemullett.service.DatabaseQueryService}.
 *
 * <p>All CRUD operations return a {@code QueryResponse} carrying a reference
 * ID for audit purposes, the number of records affected or retrieved, and
 * optionally the records themselves.
 *
 * <p>The {@code referenceId} is currently a randomly generated UUID, intended
 * to be replaced with a proper audit trail once audit logging is implemented.
 *
 * @param <T> the type of {@link DatabaseRecord} contained in the response.
 */
@Immutable
@JsonSerialize(as=ImmutableQueryResponse.class)
@JsonDeserialize(as=ImmutableQueryResponse.class)
public interface QueryResponse<T extends DatabaseRecord> {

    /**
     * Returns the reference ID for this response, used for audit trail
     * purposes.
     *
     * @return the reference ID string.
     */
    String getReferenceId();

    /**
     * Returns the number of records affected or retrieved by the operation.
     *
     * @return the record count.
     */
    int getRecordCount();

    /**
     * Returns the records retrieved by the operation, or {@code null} for
     * operations that do not return records such as INSERT, UPDATE, or DELETE.
     *
     * @return the list of {@link DatabaseRecord} instances, or {@code null}.
     */
    @Nullable
    List<T> getRecords();
}
