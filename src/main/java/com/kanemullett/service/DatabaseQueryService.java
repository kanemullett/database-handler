package com.kanemullett.service;

import java.util.List;
import java.util.UUID;

import org.jooq.DSLContext;
import org.jooq.Query;
import org.jooq.SelectQuery;
import org.springframework.stereotype.Service;

import com.kanemullett.function.QueryBuilderFunction;
import com.kanemullett.function.UpdateBuilderFunction;
import com.kanemullett.mapper.DatabaseRecordMapper;
import com.kanemullett.model.DatabaseRecord;
import com.kanemullett.model.ImmutableQueryResponse;
import com.kanemullett.model.QueryRequest;
import com.kanemullett.model.QueryResponse;
import com.kanemullett.model.UpdateRequest;

/**
 * Service for executing database query and update operations.
 *
 * <p>Acts as the primary entry point for database interactions, delegating
 * query construction to {@link QueryBuilderFunction} and
 * {@link UpdateBuilderFunction} before executing the resulting jOOQ queries
 * via the injected {@link DSLContext}.
 *
 * <p>All operations return a {@link QueryResponse} carrying a reference ID,
 * the number of records affected or retrieved, and optionally the records
 * themselves.
 *
 * @param <T> the type of {@link DatabaseRecord} this service operates on.
 */
@Service
public class DatabaseQueryService {
    private final DSLContext dsl;
    private final QueryBuilderFunction queryBuilderFunction;
    private final UpdateBuilderFunction updateBuilderFunction;
    private final DatabaseRecordMapper recordMapper;

    /**
     * Constructs a new {@code DatabaseQueryService} with the given
     * {@link DSLContext} and builder functions.
     *
     * @param dsl                  the jOOQ DSL context used to execute queries.
     * @param queryBuilderFunction the function used to build SELECT queries.
     * @param updateBuilderFunction the function used to build INSERT, UPDATE,
     *                              and DELETE queries.
     */
    public DatabaseQueryService(
        DSLContext dsl,
        QueryBuilderFunction queryBuilderFunction,
        UpdateBuilderFunction updateBuilderFunction,
        DatabaseRecordMapper recordMapper) {
        
        this.dsl = dsl;
        this.queryBuilderFunction = queryBuilderFunction;
        this.updateBuilderFunction = updateBuilderFunction;
        this.recordMapper = recordMapper;
    }

    /**
     * Retrieves records from the database based on the given
     * {@link QueryRequest}.
     *
     * <p>The request is converted to a jOOQ {@link SelectQuery} by the
     * {@link QueryBuilderFunction}, executed against the database, and the
     * results are mapped to the typed {@link DatabaseRecord} class specified
     * by {@link QueryRequest#getRecordClass()}.
     *
     * @param request the query request specifying the SELECT operation.
     * @return a {@link QueryResponse} containing the retrieved records.
     */
    public <T extends DatabaseRecord> QueryResponse<T> retrieveRecords(QueryRequest<T> request) {
        final SelectQuery<?> query = queryBuilderFunction.apply(request);

        final List<T> records = dsl.fetch(query).stream()
            .map(record -> recordMapper.map(record, request.getRecordClass()))
            .toList();

        return ImmutableQueryResponse.<T>builder()
            .referenceId(UUID.randomUUID().toString())
            .recordCount(records.size())
            .records(records)
            .build();
    }

    /**
     * Executes a write operation against the database based on the given
     * {@link UpdateRequest}.
     *
     * <p>The request is converted to a jOOQ {@link Query} by the
     * {@link UpdateBuilderFunction} and executed against the database.
     * Supports INSERT, UPDATE, and DELETE operations as defined by
     * {@link UpdateRequest#getOperation()}.
     *
     * @param request the update request specifying the write operation.
     * @return a {@link QueryResponse} containing the number of affected rows.
     */
    public <T extends DatabaseRecord> QueryResponse<T> updateRecords(UpdateRequest<T> request) {
        final Query query = updateBuilderFunction.apply(request);

        final int recordCount = dsl.execute(query);

        return ImmutableQueryResponse.<T>builder()
            .referenceId(UUID.randomUUID().toString())
            .recordCount(recordCount)
            .build();
    }
}
