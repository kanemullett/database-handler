package com.kanemullett.service;

import java.util.List;
import java.util.UUID;

import org.jooq.DSLContext;
import org.jooq.Query;
import org.jooq.SelectQuery;
import org.springframework.stereotype.Service;

import com.kanemullett.function.QueryBuilderFunction;
import com.kanemullett.function.UpdateBuilderFunction;
import com.kanemullett.model.DatabaseRecord;
import com.kanemullett.model.ImmutableQueryResponse;
import com.kanemullett.model.QueryRequest;
import com.kanemullett.model.QueryResponse;
import com.kanemullett.model.UpdateRequest;

@Service
public class DatabaseQueryService<T extends DatabaseRecord> {
    private final DSLContext dsl;
    private final QueryBuilderFunction<T> queryBuilderFunction;
    private final UpdateBuilderFunction<T> updateBuilderFunction;

    public DatabaseQueryService(
        DSLContext dsl,
        QueryBuilderFunction<T> queryBuilderFunction,
        UpdateBuilderFunction<T> updateBuilderFunction) {
        
        this.dsl = dsl;
        this.queryBuilderFunction = queryBuilderFunction;
        this.updateBuilderFunction = updateBuilderFunction;
    }

    public QueryResponse<T> retrieveRecords(QueryRequest<T> request) {
        final SelectQuery<?> query = queryBuilderFunction.apply(request);

        final List<T> records = dsl.fetch(query)
            .into(request.getRecordClass());

        return ImmutableQueryResponse.<T>builder()
            .referenceId(UUID.randomUUID().toString())
            .recordCount(records.size())
            .records(records)
            .build();
    }

    public QueryResponse<T> updateRecords(UpdateRequest<T> request) {
        final Query query = updateBuilderFunction.apply(request);

        final int recordCount = dsl.execute(query);

        return ImmutableQueryResponse.<T>builder()
            .referenceId(UUID.randomUUID().toString())
            .recordCount(recordCount)
            .build();
    }
}
