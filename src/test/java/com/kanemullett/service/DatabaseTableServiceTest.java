package com.kanemullett.service;

import org.jooq.DSLContext;
import org.jooq.DropTableStep;
import org.jooq.Query;
import static org.junit.Assert.assertEquals;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.kanemullett.function.TableBuilderFunction;
import com.kanemullett.model.Table;
import com.kanemullett.model.TableDefinition;

public class DatabaseTableServiceTest {
    private DatabaseTableService service;

    private final DSLContext dsl = mock(DSLContext.class);
    private final TableBuilderFunction tableBuilderFunction = mock(TableBuilderFunction.class);

    @BeforeEach
    void setUp() {
        this.service = new DatabaseTableService(dsl, tableBuilderFunction);
    }

    @Test
    void shouldCreateTable() {
        // Given
        final TableDefinition tableDefinition = mock(TableDefinition.class);
        final Query query = mock(Query.class);

        when(tableBuilderFunction.apply(tableDefinition))
            .thenReturn(query);

        // When
        service.createTable(tableDefinition);

        // Then
        verify(dsl).execute(query);
    }

    @Test
    void shouldDeleteTable() {
        // Given
        final Table table = Table.of("test-schema", "test-table");
        
        when(dsl.dropTableIfExists(any(org.jooq.Table.class)))
            .thenReturn(mock(DropTableStep.class));

        // When
        service.deleteTable(table);

        // Then
        final ArgumentCaptor<org.jooq.Table> tableCaptor = ArgumentCaptor.forClass(org.jooq.Table.class);
        verify(dsl).dropTableIfExists(tableCaptor.capture());

        final org.jooq.Table capturedTable = tableCaptor.getValue();
        assertEquals("test-schema", capturedTable.getSchema().getName());
        assertEquals("test-table", capturedTable.getName());
    }
}
