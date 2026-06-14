package com.kanemullett.function;

import java.util.List;

import org.jooq.DSLContext;
import org.jooq.Query;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import static org.junit.Assert.assertEquals;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.kanemullett.model.ColumnDefinition;
import com.kanemullett.model.ImmutableColumnDefinition;
import com.kanemullett.model.ImmutableTableDefinition;
import com.kanemullett.model.TableDefinition;
import com.kanemullett.model.type.SqlDataType;

public class TableBuilderFunctionTest {
    private TableBuilderFunction function;

    private final DSLContext dsl = DSL.using(SQLDialect.POSTGRES);

    @BeforeEach
    void setUp() {
        this.function = new TableBuilderFunction(dsl);
    }

    @Test
    void shouldBuildTableRequest() {
        // Given
        final TableDefinition tableDefinition = ImmutableTableDefinition.builder()
            .schema("test-schema")
            .table("test-table")
            .columns(List.of(
                ImmutableColumnDefinition.builder()
                    .name("id")
                    .dataType(SqlDataType.VARCHAR)
                    .primaryKey(true)
                    .build(),
                ColumnDefinition.of("name", SqlDataType.VARCHAR),
                ColumnDefinition.of("age", SqlDataType.INTEGER),
                ColumnDefinition.of("hasPets", SqlDataType.BOOLEAN),
                ColumnDefinition.of("joinDate", SqlDataType.TIMESTAMP_WITHOUT_TIME_ZONE)
            ))
            .build();

        final String expected = "create table if not exists \"test-schema\".\"test-table\" (\"id\" varchar, \"name\" varchar, \"age\" int, \"hasPets\" boolean, \"joinDate\" timestamp, primary key (\"id\"))";

        // When
        final Query query = function.apply(tableDefinition);

        // Then
        assertEquals(expected, query.getSQL());
    }
}
