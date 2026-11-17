package com.kanemullett.service;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.jooq.DSLContext;
import org.jooq.DropTableStep;
import org.jooq.Query;
import org.jooq.SQLDialect;
import org.jooq.exception.DataAccessException;
import org.jooq.impl.DSL;
import org.jooq.tools.jdbc.MockConnection;
import org.jooq.tools.jdbc.MockResult;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
import com.kanemullett.model.ColumnDefinition;
import com.kanemullett.model.ImmutableTableDefinition;
import com.kanemullett.model.TableDefinition;
import com.kanemullett.model.type.SqlDataType;

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

    @Test
    void shouldCreateAllTablesInOneBatch() {
        // Given
        final RecordingConnection connection = new RecordingConnection();
        final DatabaseTableService tableService = serviceUsing(connection);

        // When
        tableService.createTables(List.of(definition("users"), definition("orders")));

        // Then
        assertThat(connection.batches).containsExactly(List.of(
            "create table if not exists \"app\".\"users\" (\"id\" varchar not null, \"name\" varchar not null, primary key (\"id\"))",
            "create table if not exists \"app\".\"orders\" (\"id\" varchar not null, \"name\" varchar not null, primary key (\"id\"))"
        ));
    }

    @Test
    void shouldCreateTablesInATransactionThatIsCommitted() {
        // Given
        final RecordingConnection connection = new RecordingConnection();
        final DatabaseTableService tableService = serviceUsing(connection);

        // When
        tableService.createTables(List.of(definition("users"), definition("orders")));

        // Then
        assertThat(connection.autoCommitDisabled).isTrue();
        assertThat(connection.commits).isEqualTo(1);
        assertThat(connection.rollbacks).isZero();
    }

    @Test
    void shouldRollBackWhenCreatingATableFails() {
        // Given
        final RecordingConnection connection = new RecordingConnection();
        connection.failOnExecute = true;
        final DatabaseTableService tableService = serviceUsing(connection);
        final List<TableDefinition> definitions = List.of(definition("users"), definition("orders"));

        // When / Then
        assertThatThrownBy(() -> tableService.createTables(definitions))
            .isInstanceOf(DataAccessException.class);

        assertThat(connection.rollbacks).isEqualTo(1);
        assertThat(connection.commits).isZero();
    }

    @Test
    void shouldNotTouchTheDatabaseWhenBuildingAQueryFails() {
        // Given
        final RecordingConnection connection = new RecordingConnection();
        final TableBuilderFunction failingFunction = mock(TableBuilderFunction.class);
        final TableDefinition users = definition("users");
        final TableDefinition orders = definition("orders");

        when(failingFunction.apply(users))
            .thenReturn(new TableBuilderFunction(DSL.using(SQLDialect.POSTGRES)).apply(users));
        when(failingFunction.apply(orders))
            .thenThrow(new IllegalArgumentException("bad definition"));

        final DatabaseTableService tableService = new DatabaseTableService(
            DSL.using(connection, SQLDialect.POSTGRES), failingFunction);
        final List<TableDefinition> definitions = List.of(users, orders);

        // When / Then
        assertThatThrownBy(() -> tableService.createTables(definitions))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("bad definition");

        assertThat(connection.batches).isEmpty();
        assertThat(connection.autoCommitDisabled).isFalse();
        assertThat(connection.commits).isZero();
        assertThat(connection.rollbacks).isZero();
    }

    private static DatabaseTableService serviceUsing(RecordingConnection connection) {
        final DSLContext realDsl = DSL.using(connection, SQLDialect.POSTGRES);
        return new DatabaseTableService(realDsl, new TableBuilderFunction(realDsl));
    }

    private static TableDefinition definition(String table) {
        return ImmutableTableDefinition.builder()
            .schema("app")
            .table(table)
            .addColumns(ColumnDefinition.of("name", SqlDataType.VARCHAR))
            .build();
    }

    /**
     * A jOOQ {@link MockConnection} that records each batch of SQL it is asked
     * to execute and the transaction calls made on it, and can be told to fail
     * when executing.
     */
    private static final class RecordingConnection extends MockConnection {
        private final List<List<String>> batches;
        private boolean failOnExecute;
        private boolean autoCommit = true;
        private boolean autoCommitDisabled;
        private int commits;
        private int rollbacks;

        RecordingConnection() {
            this(new ArrayList<>());
        }

        private RecordingConnection(List<List<String>> batches) {
            super(context -> {
                batches.add(List.of(context.batchSQL()));
                return new MockResult[] { new MockResult(0, null) };
            });
            this.batches = batches;
        }

        @Override
        public java.sql.Statement createStatement() throws SQLException {
            if (failOnExecute) {
                throw new SQLException("table creation failed");
            }
            return super.createStatement();
        }

        @Override
        public boolean getAutoCommit() {
            return autoCommit;
        }

        @Override
        public void setAutoCommit(boolean autoCommit) throws SQLException {
            if (!autoCommit) {
                autoCommitDisabled = true;
            }
            this.autoCommit = autoCommit;
        }

        @Override
        public void commit() throws SQLException {
            commits++;
            super.commit();
        }

        @Override
        public void rollback() throws SQLException {
            rollbacks++;
            super.rollback();
        }
    }
}
