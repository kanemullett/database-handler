package com.kanemullett.model;

import java.util.List;

import org.immutables.value.Value.Immutable;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.kanemullett.model.type.SqlOperator;

import jakarta.annotation.Nullable;

/**
 * Represents a SQL write operation request, encapsulating all the components
 * required to build and execute an INSERT, UPDATE, or DELETE statement via
 * the {@link com.kanemullett.function.UpdateBuilderFunction}.
 *
 * <p>The {@link #getRecordClass()} field is used by the
 * {@link com.kanemullett.service.DatabaseQueryService} to map jOOQ results
 * back into typed {@link DatabaseRecord} instances where applicable.
 *
 * <p>Example usage:
 * <pre>
 * ImmutableUpdateRequest.&lt;MyRecord&gt;builder()
 *     .operation(SqlOperator.INSERT)
 *     .table(Table.of("my_schema", "my_table"))
 *     .records(List.of(myRecord))
 *     .recordClass(MyRecord.class)
 *     .build();
 * </pre>
 *
 * @param <T> the type of {@link DatabaseRecord} the request operates on.
 */
@Immutable
@JsonSerialize(as=ImmutableUpdateRequest.class)
@JsonDeserialize(as=ImmutableUpdateRequest.class)
public interface UpdateRequest<T extends DatabaseRecord> {

    /**
     * Returns the SQL operation to perform.
     *
     * @return the {@link SqlOperator}.
     */
    SqlOperator getOperation();

    /**
     * Returns the table to perform the operation on.
     *
     * @return the {@link Table}.
     */
    Table getTable();

    /**
     * Returns the records to insert or update, or {@code null} for DELETE
     * operations.
     *
     * @return the list of {@link DatabaseRecord} instances, or {@code null}.
     */
    @Nullable
    List<T> getRecords();

    /**
     * Returns the class of the {@link DatabaseRecord} type that results
     * should be mapped to.
     *
     * @return the record class.
     */
    @Nullable
    Class<T> getRecordClass();

    /**
     * Returns the condition group used to build the WHERE clause for DELETE
     * operations, or {@code null} if not required.
     *
     * @return the {@link QueryConditionGroup}, or {@code null}.
     */
    @Nullable
    QueryConditionGroup getConditionGroup();
}
