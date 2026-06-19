package com.kanemullett.service;

import java.util.List;

import org.jooq.DSLContext;
import org.jooq.Query;
import org.jooq.Result;
import org.jooq.SelectQuery;
import static org.junit.Assert.assertEquals;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.kanemullett.function.QueryBuilderFunction;
import com.kanemullett.function.UpdateBuilderFunction;
import com.kanemullett.model.DatabaseRecord;
import com.kanemullett.model.QueryRequest;
import com.kanemullett.model.QueryResponse;
import com.kanemullett.model.UpdateRequest;

public class DatabaseQueryServiceTest {
    private DatabaseQueryService service;
    
    private final DSLContext dsl = mock(DSLContext.class);
    private final QueryBuilderFunction queryFunction = mock(QueryBuilderFunction.class);
    private final UpdateBuilderFunction updateFunction = mock(UpdateBuilderFunction.class);

    @BeforeEach
    void setUp() {
        this.service = new DatabaseQueryService(dsl, queryFunction, updateFunction);
    }

    @Test
    void shouldRetrieveRecords() {
        // Given
        final SelectQuery query = mock(SelectQuery.class);
        when(queryFunction.apply(any()))
            .thenReturn(query);

        final Result result = mock(Result.class);
        when(result.into(DatabaseRecord.class))
            .thenReturn(List.of(mock(DatabaseRecord.class)));

        when(dsl.fetch(query))
            .thenReturn(result);

        final QueryRequest<DatabaseRecord> request = mock(QueryRequest.class);
        when(request.getRecordClass())
            .thenReturn(DatabaseRecord.class);

        // When
        final QueryResponse<DatabaseRecord> response = service.retrieveRecords(request);

        // Then
        assertEquals(1, response.getRecordCount());
        assertEquals(1, response.getRecords().size());
    }

    @Test
    void shouldUpdateRecords() {
        // Given
        final Query query = mock(Query.class);
        when(updateFunction.apply(any()))
            .thenReturn(query);

        when(dsl.execute(query))
            .thenReturn(2);

        final UpdateRequest<DatabaseRecord> request = mock(UpdateRequest.class);

        // When
        final QueryResponse<DatabaseRecord> response = service.updateRecords(request);

        // Then
        assertEquals(2, response.getRecordCount());
    }
}
