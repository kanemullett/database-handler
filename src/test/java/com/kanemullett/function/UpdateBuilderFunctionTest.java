package com.kanemullett.function;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.immutables.value.Value.Immutable;
import org.jooq.DSLContext;
import org.jooq.Query;
import org.jooq.SQLDialect;
import org.jooq.conf.ParamType;
import org.jooq.impl.DSL;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.kanemullett.model.Column;
import com.kanemullett.model.DatabaseRecord;
import com.kanemullett.model.ImmutableQueryConditionGroup;
import com.kanemullett.model.ImmutableUpdateRequest;
import com.kanemullett.model.QueryCondition;
import com.kanemullett.model.QueryConditionGroup;
import com.kanemullett.model.Table;
import com.kanemullett.model.UpdateRequest;
import com.kanemullett.model.type.ConditionJoin;
import com.kanemullett.model.type.SqlOperator;

import jakarta.annotation.Nullable;

public class UpdateBuilderFunctionTest {
    private UpdateBuilderFunction function;

    private final DSLContext dsl = DSL.using(SQLDialect.POSTGRES);

    @Immutable
    @JsonSerialize(as=ImmutableExtendedRecord.class)
    @JsonDeserialize(as=ImmutableExtendedRecord.class)
    interface ExtendedRecord extends DatabaseRecord {
        @Nullable
        String getName();

        @Nullable
        Integer getAge();
    }

    @BeforeEach
    void setUp() {
        this.function = new UpdateBuilderFunction(dsl);
    }

    @Test
    void shouldBuildInsertStatementWithSingleRecord() {
        // Given
        final UpdateRequest<ExtendedRecord> request = ImmutableUpdateRequest.<ExtendedRecord>builder()
            .operation(SqlOperator.INSERT)
            .table(Table.of("test_schema", "test_table"))
            .records(List.of(
                ImmutableExtendedRecord.builder()
                    .id("id1")
                    .name("Kane")
                    .age(25)
                    .build()
            ))
            .recordClass(ExtendedRecord.class)
            .build();

        final String expected = "insert into \"test_schema\".\"test_table\" (\"age\", \"id\", \"name\") values (25, 'id1', 'Kane')";

        // When
        final Query query = function.apply(request);

        // Then
        assertEquals(expected, query.getSQL(ParamType.INLINED));
    }

    @Test
    void shouldBuildInsertStatementWithMultipleRecords() {
        // Given
        final UpdateRequest<ExtendedRecord> request = ImmutableUpdateRequest.<ExtendedRecord>builder()
            .operation(SqlOperator.INSERT)
            .table(Table.of("test_schema", "test_table"))
            .records(List.of(
                ImmutableExtendedRecord.builder()
                    .id("id1")
                    .name("Kane")
                    .age(25)
                    .build(),
                ImmutableExtendedRecord.builder()
                    .id("id2")
                    .name("Raúl")
                    .age(35)
                    .build()
            ))
            .recordClass(ExtendedRecord.class)
            .build();

        final String expected = "insert into \"test_schema\".\"test_table\" (\"age\", \"id\", \"name\") values (25, 'id1', 'Kane'), (35, 'id2', 'Raúl')";

        // When
        final Query query = function.apply(request);

        // Then
        assertEquals(expected, query.getSQL(ParamType.INLINED));
    }

    @Test
    void shouldBuildInsertStatementWithMultipleRecordsAndMissingColumns() {
        // Given
        final UpdateRequest<ExtendedRecord> request = ImmutableUpdateRequest.<ExtendedRecord>builder()
            .operation(SqlOperator.INSERT)
            .table(Table.of("test_schema", "test_table"))
            .records(List.of(
                ImmutableExtendedRecord.builder()
                    .id("id1")
                    .name("Kane")
                    .build(),
                ImmutableExtendedRecord.builder()
                    .id("id2")
                    .age(35)
                    .build()
            ))
            .recordClass(ExtendedRecord.class)
            .build();

        final String expected = "insert into \"test_schema\".\"test_table\" (\"age\", \"id\", \"name\") values (null, 'id1', 'Kane'), (35, 'id2', null)";

        // When
        final Query query = function.apply(request);

        // Then
        assertEquals(expected, query.getSQL(ParamType.INLINED));
    }

    @Test
    void shouldBuildUpdateStatementWithSingleRecord() {
        // Given
        final UpdateRequest<ExtendedRecord> request = ImmutableUpdateRequest.<ExtendedRecord>builder()
            .operation(SqlOperator.UPDATE)
            .table(Table.of("test_schema", "test_table"))
            .records(List.of(
                ImmutableExtendedRecord.builder()
                    .id("id1")
                    .name("Kane Mullett")
                    .age(23)
                    .build()
            ))
            .recordClass(ExtendedRecord.class)
            .build();

        final String expected = "update \"test_schema\".\"test_table\" set \"age\" = case when \"id\" = 'id1' then 23 else \"age\" end, \"name\" = case when \"id\" = 'id1' then 'Kane Mullett' else \"name\" end where \"id\" in ('id1')";

        // When
        final Query query = function.apply(request);

        // Then
        assertEquals(expected, query.getSQL(ParamType.INLINED));
    }

    @Test
    void shouldBuildUpdateStatementWithMultipleRecords() {
        // Given
        final UpdateRequest<ExtendedRecord> request = ImmutableUpdateRequest.<ExtendedRecord>builder()
            .operation(SqlOperator.UPDATE)
            .table(Table.of("test_schema", "test_table"))
            .records(List.of(
                ImmutableExtendedRecord.builder()
                    .id("id1")
                    .name("Kane Mullett")
                    .age(23)
                    .build(),
                ImmutableExtendedRecord.builder()
                    .id("id2")
                    .name("Raúl Jiménez")
                    .age(29)
                    .build()
            ))
            .recordClass(ExtendedRecord.class)
            .build();

        final String expected = "update \"test_schema\".\"test_table\" set \"age\" = case when \"id\" = 'id1' then 23 when \"id\" = 'id2' then 29 else \"age\" end, \"name\" = case when \"id\" = 'id1' then 'Kane Mullett' when \"id\" = 'id2' then 'Raúl Jiménez' else \"name\" end where \"id\" in ('id1', 'id2')";

        // When
        final Query query = function.apply(request);

        // Then
        assertEquals(expected, query.getSQL(ParamType.INLINED));
    }

    @Test
    void shouldBuildDeleteStatementWithSingleCondition() {
        // Given
        final UpdateRequest<ExtendedRecord> request = ImmutableUpdateRequest.<ExtendedRecord>builder()
            .operation(SqlOperator.DELETE)
            .table(Table.of("test_schema", "test_table"))
            .conditionGroup(QueryConditionGroup.of(
                QueryCondition.of(Column.of("id"), "id1")
            ))
            .recordClass(ExtendedRecord.class)
            .build();

        final String expected = "delete from \"test_schema\".\"test_table\" where \"id\" = 'id1'";

        // When
        final Query query = function.apply(request);

        // Then
        assertEquals(expected, query.getSQL(ParamType.INLINED));
    }

    @Test
    void shouldBuildDeleteStatementWithMultipleConditions() {
        // Given
        final UpdateRequest<ExtendedRecord> request = ImmutableUpdateRequest.<ExtendedRecord>builder()
            .operation(SqlOperator.DELETE)
            .table(Table.of("test_schema", "test_table"))
            .conditionGroup(ImmutableQueryConditionGroup.builder()
                .conditions(List.of(
                    QueryCondition.of(Column.of("age"), 65),
                    QueryCondition.of(Column.of("occupation"), "solicitor")
                ))
                .join(ConditionJoin.OR)
                .build())
            .recordClass(ExtendedRecord.class)
            .build();

        final String expected = "delete from \"test_schema\".\"test_table\" where (\"age\" = 65 or \"occupation\" = 'solicitor')";

        // When
        final Query query = function.apply(request);

        // Then
        assertEquals(expected, query.getSQL(ParamType.INLINED));
    }
}
