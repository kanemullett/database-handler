package com.kanemullett.mapper;

import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.immutables.value.Value.Immutable;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Record;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.junit.jupiter.api.Test;

import com.kanemullett.model.DatabaseRecord;

import jakarta.annotation.Nullable;

public class DatabaseRecordMapperTest {

    private static final DSLContext DSL_CONTEXT = DSL.using(SQLDialect.POSTGRES);

    private final DatabaseRecordMapper mapper = new DatabaseRecordMapper();

    @Immutable
    interface TestDatabaseRecord extends DatabaseRecord {

        @Nullable
        String getName();

        @Nullable
        Integer getAge();
    }

    @Test
    void shouldMapMatchingColumnsOntoDeclaredProperties() {
        // Given
        final Field<String> id = DSL.field(DSL.name("id"), String.class);
        final Field<String> name = DSL.field(DSL.name("name"), String.class);
        final Field<Integer> age = DSL.field(DSL.name("age"), Integer.class);

        final Record record = DSL_CONTEXT.newRecord(id, name, age);

        record.set(id, "id1");
        record.set(name, "Kane");
        record.set(age, 25);

        // When
        final TestDatabaseRecord result = mapper.map(record, TestDatabaseRecord.class);

        // Then
        assertEquals("id1", result.getId());
        assertEquals("Kane", result.getName());
        assertEquals(25, result.getAge());
    }

    @Test
    void shouldcoerceCompatibleNumericTypes() {
        // Given
        final Field<String> id = DSL.field(DSL.name("id"), String.class);
        final Field<Long> age = DSL.field(DSL.name("age"), Long.class);

        final Record record = DSL_CONTEXT.newRecord(id, age);

        record.set(id, "id1");
        record.set(age, 25L);

        // When
        final TestDatabaseRecord result = mapper.map(record, TestDatabaseRecord.class);

        // Then
        assertEquals("id1", result.getId());
        assertEquals(25, result.getAge());
    }

    @Test
    void shouldPlaceUnmatchedColumnsIntoDataMap() {
        // Given
        final Field<String> id = DSL.field(DSL.name("id"), String.class);
        final Field<String> name = DSL.field(DSL.name("name"), String.class);
        final Field<Integer> age = DSL.field(DSL.name("age"), Integer.class);
        final Field<Boolean> isOnline = DSL.field(DSL.name("isOnline"), Boolean.class);
        final Field<String> occupation = DSL.field(DSL.name("occupation"), String.class);

        final Record record = DSL_CONTEXT.newRecord(id, name, age, isOnline, occupation);

        record.set(id, "id1");
        record.set(name, "Kane");
        record.set(age, 25);
        record.set(isOnline, true);
        record.set(occupation, "Software Engineer");

        // When
        final TestDatabaseRecord result = mapper.map(record, TestDatabaseRecord.class);

        // Then
        assertEquals("id1", result.getId());
        assertEquals("Kane", result.getName());
        assertEquals(25, result.getAge());
        assertTrue((Boolean) result.getData().get("isOnline"));
        assertEquals("Software Engineer", result.getData().get("occupation"));
    }
}
