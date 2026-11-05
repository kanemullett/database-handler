package com.kanemullett.mapper;

import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

import org.immutables.value.Value.Immutable;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.JSONB;
import org.jooq.Record;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
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

        @Nullable
        Status getStatus();

        @Nullable
        List<Round> getRounds();

        @Nullable
        Map<String, Integer> getScores();

        @Nullable
        List<LocalDateTime> getReminders();

        @Nullable
        List<OffsetDateTime> getDeadlines();
    }

    @Immutable
    interface RequiredFieldRecord extends DatabaseRecord {
        String getTitle();
    }

    @Immutable
    @JsonSerialize(as = ImmutableRound.class)
    @JsonDeserialize(as = ImmutableRound.class)
    interface Round {
        String getName();

        int getDurationSeconds();
    }

    enum Status {
        ACTIVE,
        INACTIVE
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

    @Test
    void shouldCoerceStringColumnsToEnums() {
        // Given
        final Field<String> id = DSL.field(DSL.name("id"), String.class);
        final Field<String> status = DSL.field(DSL.name("status"), String.class);

        final Record record = DSL_CONTEXT.newRecord(id, status);

        record.set(id, "id1");
        record.set(status, "ACTIVE");

        // When
        final TestDatabaseRecord result = mapper.map(record, TestDatabaseRecord.class);

        // Then
        assertEquals(Status.ACTIVE, result.getStatus());
    }

    @Test
    void shouldCoerceStringColumnsToEnumsCaseInsensitively() {
        // Given
        final Field<String> id = DSL.field(DSL.name("id"), String.class);
        final Field<String> status = DSL.field(DSL.name("status"), String.class);

        final Record record = DSL_CONTEXT.newRecord(id, status);

        record.set(id, "id1");
        record.set(status, "inactive");

        // When
        final TestDatabaseRecord result = mapper.map(record, TestDatabaseRecord.class);

        // Then
        assertEquals(Status.INACTIVE, result.getStatus());
    }

    @Test
    void shouldRejectUnknownEnumValues() {
        // Given
        final Field<String> id = DSL.field(DSL.name("id"), String.class);
        final Field<String> status = DSL.field(DSL.name("status"), String.class);

        final Record record = DSL_CONTEXT.newRecord(id, status);

        record.set(id, "id1");
        record.set(status, "UNKNOWN");

        // When / Then
        assertThrows(IllegalArgumentException.class, () -> mapper.map(record, TestDatabaseRecord.class));
    }

    @Test
    void shouldDeserialiseJsonbColumnsIntoGenericListProperties() {
        // Given
        final Field<String> id = DSL.field(DSL.name("id"), String.class);
        final Field<JSONB> rounds = DSL.field(DSL.name("rounds"), JSONB.class);

        final Record record = DSL_CONTEXT.newRecord(id, rounds);

        record.set(id, "id1");
        record.set(rounds, JSONB.valueOf("[{\"name\":\"Warm up\",\"durationSeconds\":60},{\"name\":\"Sprint\",\"durationSeconds\":30}]"));

        // When
        final TestDatabaseRecord result = mapper.map(record, TestDatabaseRecord.class);

        // Then
        assertEquals(List.of(
            ImmutableRound.builder().name("Warm up").durationSeconds(60).build(),
            ImmutableRound.builder().name("Sprint").durationSeconds(30).build()
        ), result.getRounds());
    }

    @Test
    void shouldDeserialiseJsonbColumnsIntoGenericMapProperties() {
        // Given
        final Field<String> id = DSL.field(DSL.name("id"), String.class);
        final Field<JSONB> scores = DSL.field(DSL.name("scores"), JSONB.class);

        final Record record = DSL_CONTEXT.newRecord(id, scores);

        record.set(id, "id1");
        record.set(scores, JSONB.valueOf("{\"a\":1,\"b\":2}"));

        // When
        final TestDatabaseRecord result = mapper.map(record, TestDatabaseRecord.class);

        // Then
        assertEquals(Map.of("a", 1, "b", 2), result.getScores());
    }

    @Test
    void shouldDeserialiseJsonStringColumnsIntoListProperties() {
        // Given
        final Field<String> id = DSL.field(DSL.name("id"), String.class);
        final Field<String> rounds = DSL.field(DSL.name("rounds"), String.class);

        final Record record = DSL_CONTEXT.newRecord(id, rounds);

        record.set(id, "id1");
        record.set(rounds, "[{\"name\":\"Warm up\",\"durationSeconds\":60}]");

        // When
        final TestDatabaseRecord result = mapper.map(record, TestDatabaseRecord.class);

        // Then
        assertEquals(List.of(ImmutableRound.builder().name("Warm up").durationSeconds(60).build()), result.getRounds());
    }

    @Test
    void shouldOmitNullUnmatchedColumnsFromDataMap() {
        // Given
        final Field<String> id = DSL.field(DSL.name("id"), String.class);
        final Field<String> occupation = DSL.field(DSL.name("occupation"), String.class);
        final Field<String> nickname = DSL.field(DSL.name("nickname"), String.class);

        final Record record = DSL_CONTEXT.newRecord(id, occupation, nickname);

        record.set(id, "id1");
        record.set(occupation, "Software Engineer");
        record.set(nickname, null);

        // When
        final TestDatabaseRecord result = mapper.map(record, TestDatabaseRecord.class);

        // Then
        assertEquals(Map.of("occupation", "Software Engineer"), result.getData());
    }

    @Test
    void shouldLeaveNullPropertiesUnsetOnBuilder() {
        // Given
        final Field<String> id = DSL.field(DSL.name("id"), String.class);
        final Field<String> name = DSL.field(DSL.name("name"), String.class);

        final Record record = DSL_CONTEXT.newRecord(id, name);

        record.set(id, null);
        record.set(name, null);

        // When
        final TestDatabaseRecord result = mapper.map(record, TestDatabaseRecord.class);

        // Then
        assertNotNull(result.getId());
        assertNull(result.getName());
    }

    @Test
    void shouldFailClearlyWhenRequiredPropertyIsNull() {
        // Given
        final Field<String> id = DSL.field(DSL.name("id"), String.class);
        final Field<String> title = DSL.field(DSL.name("title"), String.class);

        final Record record = DSL_CONTEXT.newRecord(id, title);

        record.set(id, "id1");
        record.set(title, null);

        // When
        final IllegalStateException exception = assertThrows(IllegalStateException.class,
            () -> mapper.map(record, RequiredFieldRecord.class));

        // Then
        assertTrue(exception.getCause().getCause().getMessage().contains("title"));
    }

    @Test
    void shouldDeserialiseIsoDateTimesNestedInJsonbColumns() {
        // Given
        final Field<String> id = DSL.field(DSL.name("id"), String.class);
        final Field<JSONB> reminders = DSL.field(DSL.name("reminders"), JSONB.class);

        final Record record = DSL_CONTEXT.newRecord(id, reminders);

        // LocalDateTime.toString() omits zero seconds, as in the first value
        record.set(id, "id1");
        record.set(reminders, JSONB.valueOf("[\"2026-06-11T20:00\",\"2026-06-11T19:00:30\"]"));

        // When
        final TestDatabaseRecord result = mapper.map(record, TestDatabaseRecord.class);

        // Then
        assertEquals(List.of(
            LocalDateTime.of(2026, 6, 11, 20, 0),
            LocalDateTime.of(2026, 6, 11, 19, 0, 30)
        ), result.getReminders());
    }

    @Test
    void shouldPreserveOffsetsOfDateTimesNestedInJsonbColumns() {
        // Given
        final Field<String> id = DSL.field(DSL.name("id"), String.class);
        final Field<JSONB> deadlines = DSL.field(DSL.name("deadlines"), JSONB.class);

        final Record record = DSL_CONTEXT.newRecord(id, deadlines);

        record.set(id, "id1");
        record.set(deadlines, JSONB.valueOf("[\"2026-06-11T20:00+01:00\"]"));

        // When
        final TestDatabaseRecord result = mapper.map(record, TestDatabaseRecord.class);

        // Then
        assertEquals(List.of(OffsetDateTime.of(2026, 6, 11, 20, 0, 0, 0, ZoneOffset.ofHours(1))), result.getDeadlines());
    }
}
