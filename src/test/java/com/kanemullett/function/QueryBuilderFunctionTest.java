package com.kanemullett.function;

import java.util.List;

import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.SelectQuery;
import org.jooq.conf.ParamType;
import org.jooq.impl.DSL;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.kanemullett.model.Column;
import com.kanemullett.model.ImmutableColumn;
import com.kanemullett.model.ImmutableQueryConditionGroup;
import com.kanemullett.model.ImmutableQueryRequest;
import com.kanemullett.model.QueryCondition;
import com.kanemullett.model.QueryConditionGroup;
import com.kanemullett.model.QueryRequest;
import com.kanemullett.model.Table;
import com.kanemullett.model.TableJoin;
import com.kanemullett.model.type.ConditionJoin;
import com.kanemullett.model.type.JoinType;

public class QueryBuilderFunctionTest {
    private QueryBuilderFunction function;

    private final DSLContext dsl = DSL.using(SQLDialect.POSTGRES);

    @BeforeEach
    void setUp() {
        this.function = new QueryBuilderFunction(dsl);
    }

    @Test
    void shouldBuildSelectStatementWithSchemaAndTable() {
        // Given
        final QueryRequest request = ImmutableQueryRequest.builder()
            .table(Table.of("test_schema", "test_table"))
            .build();

        final String expected = "select * from \"test_schema\".\"test_table\"";

        // When
        final SelectQuery<?> query = function.apply(request);

        // Then
        assertEquals(expected, query.getSQL());
    }

    @Test
    void shouldBuildSelectStatementWithSingleColumn() {
        // Given
        final QueryRequest request = ImmutableQueryRequest.builder()
            .table(Table.of("test_schema", "test_table"))
            .columns(List.of(
                Column.of("column1")
            ))
            .build();

        final String expected = "select \"column1\" from \"test_schema\".\"test_table\"";

        // When
        final SelectQuery<?> query = function.apply(request);

        // Then
        assertEquals(expected, query.getSQL());
    }

    @Test
    void shouldBuildSelectStatementWithMultipleColumns() {
        // Given
        final QueryRequest request = ImmutableQueryRequest.builder()
            .table(Table.of("test_schema", "test_table"))
            .columns(List.of(
                Column.of("column1"),
                Column.of("column2"),
                Column.of("column3")
            ))
            .build();

        final String expected = "select \"column1\", \"column2\", \"column3\" from \"test_schema\".\"test_table\"";

        // When
        final SelectQuery<?> query = function.apply(request);

        // Then
        assertEquals(expected, query.getSQL());
    }

    @Test
    void shouldBuildSelectStatementWithSingleCondition() {
        // Given
        final QueryRequest request = ImmutableQueryRequest.builder()
            .table(Table.of("test_schema", "test_table"))
            .conditionGroup(QueryConditionGroup.of(
                QueryCondition.of(Column.of("column1"), "test_value")
            ))
            .build();

        final String expected = "select * from \"test_schema\".\"test_table\" where \"column1\" = 'test_value'";

        // When
        final SelectQuery<?> query = function.apply(request);

        // Then
        assertEquals(expected, query.getSQL(ParamType.INLINED));
    }

    @Test
    void shouldBuildSelectStatementWithMultipleConditions() {
        // Given
        final QueryRequest request = ImmutableQueryRequest.builder()
            .table(Table.of("test_schema", "test_table"))
            .conditionGroup(ImmutableQueryConditionGroup.builder()
                .conditions(List.of(
                    QueryCondition.of(Column.of("column1"), "test_value"),
                    QueryCondition.of(Column.of("column2"), 23)
                ))
                .join(ConditionJoin.OR)
                .build()
            )
            .build();

        final String expected = "select * from \"test_schema\".\"test_table\" where (\"column1\" = 'test_value' or \"column2\" = 23)";

        // When
        final SelectQuery<?> query = function.apply(request);

        // Then
        assertEquals(expected, query.getSQL(ParamType.INLINED));
    }

    @Test
    void shouldBuildSelectStatementWithMultipleConditionsAndAlias() {
        // Given
        final QueryRequest request = ImmutableQueryRequest.builder()
            .table(Table.of("test_schema", "test_table", "my_table"))
            .columns(List.of(
                ImmutableColumn.builder()
                    .addParts("my_table", "name")
                    .alias("user_name")
                    .build()
            ))
            .conditionGroup(ImmutableQueryConditionGroup.builder()
                .conditions(List.of(
                    QueryCondition.of(
                        ImmutableColumn.builder()
                            .addParts("my_table", "column1")
                            .alias("should_ignore")
                            .build(),
                        "test_value"
                    ),
                    QueryCondition.of(
                        ImmutableColumn.builder()
                            .addParts("my_table", "column2")
                            .alias("should_ignore")
                            .build(),
                        23
                    )
                ))
                .join(ConditionJoin.OR)
                .build())
            .build();

        final String expected = "select \"my_table\".\"name\" as \"user_name\" from \"test_schema\".\"test_table\" as \"my_table\" where (\"my_table\".\"column1\" = 'test_value' or \"my_table\".\"column2\" = 23)";

        // When
        final SelectQuery<?> query = function.apply(request);

        // Then
        assertEquals(expected, query.getSQL(ParamType.INLINED));
    }

    @Test
    void shouldBuildSelectStatementWithMultipleConditionsAndTableJoins() {
        // Given
        final QueryRequest request = ImmutableQueryRequest.builder()
            .table(Table.of("test_schema", "test_table", "my_table"))
            .columns(List.of(
                ImmutableColumn.builder()
                    .addParts("my_table", "name")
                    .alias("user_name")
                    .build()
            ))
            .joins(List.of(
                TableJoin.of(
                    Table.of("test_schema", "join_table_one", "first_joiner"),
                    QueryCondition.of(
                        Column.of("my_table", "id"),
                        Column.of("first_joiner", "baseId")
                    ),
                    JoinType.INNER
                ),
                TableJoin.of(
                    Table.of("test_schema", "join_table_two", "second_joiner"),
                    QueryCondition.of(
                        Column.of("my_table", "id"),
                        Column.of("second_joiner", "baseId")
                    ),
                    JoinType.LEFT
                )
            ))
            .conditionGroup(ImmutableQueryConditionGroup.builder()
                .conditions(List.of(
                    QueryCondition.of(
                        ImmutableColumn.builder()
                            .addParts("my_table", "column1")
                            .alias("should_ignore")
                            .build(),
                        "test_value"
                    ),
                    QueryCondition.of(
                        ImmutableColumn.builder()
                            .addParts("my_table", "column2")
                            .alias("should_ignore")
                            .build(),
                        23
                    )
                ))
                .join(ConditionJoin.OR)
                .build())
            .build();

        final String expected = "select \"my_table\".\"name\" as \"user_name\" from \"test_schema\".\"test_table\" as \"my_table\" join \"test_schema\".\"join_table_one\" as \"first_joiner\" on \"my_table\".\"id\" = \"first_joiner\".\"baseId\" left outer join \"test_schema\".\"join_table_two\" as \"second_joiner\" on \"my_table\".\"id\" = \"second_joiner\".\"baseId\" where (\"my_table\".\"column1\" = 'test_value' or \"my_table\".\"column2\" = 23)";

        // When
        final SelectQuery<?> query = function.apply(request);

        // Then
        assertEquals(expected, query.getSQL(ParamType.INLINED));
    }
}
