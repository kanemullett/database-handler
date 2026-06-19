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
import com.kanemullett.model.DatabaseRecord;
import com.kanemullett.model.GroupBy;
import com.kanemullett.model.ImmutableColumn;
import com.kanemullett.model.ImmutableFunction;
import com.kanemullett.model.ImmutableOrderBy;
import com.kanemullett.model.ImmutableQueryCondition;
import com.kanemullett.model.ImmutableQueryConditionGroup;
import com.kanemullett.model.ImmutableQueryJoin;
import com.kanemullett.model.ImmutableQueryRequest;
import com.kanemullett.model.QueryCondition;
import com.kanemullett.model.QueryConditionGroup;
import com.kanemullett.model.QueryRequest;
import com.kanemullett.model.Table;
import com.kanemullett.model.TableJoin;
import com.kanemullett.model.type.ConditionJoin;
import com.kanemullett.model.type.ConditionOperator;
import com.kanemullett.model.type.JoinType;
import com.kanemullett.model.type.OrderDirection;

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
        final QueryRequest<DatabaseRecord> request = ImmutableQueryRequest.builder()
            .table(Table.of("test_schema", "test_table"))
            .recordClass(DatabaseRecord.class)
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
        final QueryRequest<DatabaseRecord> request = ImmutableQueryRequest.builder()
            .table(Table.of("test_schema", "test_table"))
            .columns(List.of(
                Column.of("column1")
            ))
            .recordClass(DatabaseRecord.class)
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
        final QueryRequest<DatabaseRecord> request = ImmutableQueryRequest.builder()
            .table(Table.of("test_schema", "test_table"))
            .columns(List.of(
                Column.of("column1"),
                Column.of("column2"),
                Column.of("column3")
            ))
            .recordClass(DatabaseRecord.class)
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
        final QueryRequest<DatabaseRecord> request = ImmutableQueryRequest.builder()
            .table(Table.of("test_schema", "test_table"))
            .conditionGroup(QueryConditionGroup.of(
                QueryCondition.of(Column.of("column1"), "test_value")
            ))
            .recordClass(DatabaseRecord.class)
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
        final QueryRequest<DatabaseRecord> request = ImmutableQueryRequest.builder()
            .table(Table.of("test_schema", "test_table"))
            .conditionGroup(ImmutableQueryConditionGroup.builder()
                .conditions(List.of(
                    QueryCondition.of(Column.of("column1"), "test_value"),
                    QueryCondition.of(Column.of("column2"), 23)
                ))
                .join(ConditionJoin.OR)
                .build()
            )
            .recordClass(DatabaseRecord.class)
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
        final QueryRequest<DatabaseRecord> request = ImmutableQueryRequest.builder()
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
            .recordClass(DatabaseRecord.class)
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
        final QueryRequest<DatabaseRecord> request = ImmutableQueryRequest.builder()
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
            .recordClass(DatabaseRecord.class)
            .build();

        final String expected = "select \"my_table\".\"name\" as \"user_name\" from \"test_schema\".\"test_table\" as \"my_table\" join \"test_schema\".\"join_table_one\" as \"first_joiner\" on \"my_table\".\"id\" = \"first_joiner\".\"baseId\" left outer join \"test_schema\".\"join_table_two\" as \"second_joiner\" on \"my_table\".\"id\" = \"second_joiner\".\"baseId\" where (\"my_table\".\"column1\" = 'test_value' or \"my_table\".\"column2\" = 23)";

        // When
        final SelectQuery<?> query = function.apply(request);

        // Then
        assertEquals(expected, query.getSQL(ParamType.INLINED));
    }

    @Test
    void shouldBuildSelectStatementWithInCondition() {
        // Given
        final QueryRequest<DatabaseRecord> request = ImmutableQueryRequest.builder()
            .table(Table.of("test_schema", "test_table"))
            .conditionGroup(QueryConditionGroup.of(
                ImmutableQueryCondition.builder()
                    .column(Column.of("column1"))
                    .operator(ConditionOperator.IN)
                    .value(List.of("val1", "val2"))
                    .build()
            ))
            .recordClass(DatabaseRecord.class)
            .build();

        final String expected = "select * from \"test_schema\".\"test_table\" where \"column1\" in ('val1', 'val2')";

        // When
        final SelectQuery<?> query = function.apply(request);

        // Then
        assertEquals(expected, query.getSQL(ParamType.INLINED));
    }

    @Test
    void shouldBuildSelectStatementWithFunctionAndGroupBy() {
        // Given
        final QueryRequest<DatabaseRecord> request = ImmutableQueryRequest.builder()
            .columns(List.of(
                Column.of("tab", "column"),
                ImmutableFunction.builder()
                    .addParts("AGG_FUNCTION")
                    .args(List.of(
                        ImmutableFunction.builder()
                            .addParts("test_schema", "test_function")
                            .args(List.of(
                                Column.of("tab", "integer_column"),
                                Column.of("join", "integer_column")
                            ))
                            .build()
                    ))
                    .alias("total")
                    .build()
            ))
            .table(Table.of("test_schema", "test_table", "tab"))
            .joins(List.of(
                TableJoin.of(
                    Table.of("test_schema", "join_table", "join"),
                    QueryCondition.of(
                        Column.of("tab", "matchId"),
                        Column.of("join", "matchId")
                    ),
                    JoinType.RIGHT
                )
            ))
            .groupBy(GroupBy.of(
                Column.of("tab", "column")
            ))
            .orderBy(ImmutableOrderBy.builder()
                .column(Column.of("total"))
                .direction(OrderDirection.DESCENDING)
                .build())
            .recordClass(DatabaseRecord.class)
            .build();

        final String expected = "select \"tab\".\"column\", AGG_FUNCTION(\"test_schema\".test_function(\"tab\".\"integer_column\", \"join\".\"integer_column\")) as \"total\" from \"test_schema\".\"test_table\" as \"tab\" right outer join \"test_schema\".\"join_table\" as \"join\" on \"tab\".\"matchId\" = \"join\".\"matchId\" group by \"tab\".\"column\" order by \"total\" desc";

        // When
        final SelectQuery<?> query = function.apply(request);

        // Then
        assertEquals(expected, query.getSQL(ParamType.INLINED));
    }

    @Test
    void shouldBuildSelectStatementWithQueryJoinAndUnion() {
        // Given
        final QueryRequest<DatabaseRecord> request = ImmutableQueryRequest.builder()
            .distinct(true)
            .columns(List.of(
                Column.of("test_alias", "id"),
                Column.of("test_alias", "column1"),
                Column.of("test_alias", "column2"),
                Column.of("test_alias", "column3")
            ))
            .table(Table.of("test_schema", "test_table", "test_alias"))
            .joins(List.of(
                ImmutableQueryJoin.builder()
                    .query(ImmutableQueryRequest.builder()
                        .distinct(true)
                        .columns(List.of(
                            Column.of("fkId")
                        ))
                        .table(Table.of("test_schema", "table1"))
                        .joins(List.of(
                            ImmutableQueryJoin.builder()
                                .query(ImmutableQueryRequest.builder()
                                    .distinct(true)
                                    .columns(List.of(
                                        Column.of("firstFkId")
                                    ))
                                    .table(Table.of("test_schema", "table2"))
                                    .recordClass(DatabaseRecord.class)
                                    .build())
                                .joinType(JoinType.UNION)
                                .build(),
                            ImmutableQueryJoin.builder()
                                .query(ImmutableQueryRequest.builder()
                                    .distinct(true)
                                    .columns(List.of(
                                        Column.of("secondFkId")
                                    ))
                                    .table(Table.of("test_schema", "table2"))
                                    .recordClass(DatabaseRecord.class)
                                    .build())
                                .joinType(JoinType.UNION)
                                .build()
                        ))
                        .recordClass(DatabaseRecord.class)
                        .build())
                    .alias("ids")
                    .joinType(JoinType.INNER)
                    .joinCondition(QueryCondition.of(
                        Column.of("test_alias", "id"),
                        Column.of("ids", "fkId")
                    ))
                    .build()
            ))
            .recordClass(DatabaseRecord.class)
            .build();

        final String expected = "select distinct \"test_alias\".\"id\", \"test_alias\".\"column1\", \"test_alias\".\"column2\", \"test_alias\".\"column3\" from \"test_schema\".\"test_table\" as \"test_alias\" join (select distinct \"fkId\" from \"test_schema\".\"table1\" union select distinct \"firstFkId\" from \"test_schema\".\"table2\" union select distinct \"secondFkId\" from \"test_schema\".\"table2\") as \"ids\" on \"test_alias\".\"id\" = \"ids\".\"fkId\"";

        // When
        final SelectQuery<?> query = function.apply(request);

        // Then
        assertEquals(expected, query.getSQL(ParamType.INLINED));
    }
}
