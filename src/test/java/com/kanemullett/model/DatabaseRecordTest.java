package com.kanemullett.model;

import static org.junit.Assert.assertEquals;

import org.immutables.value.Value;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

public class DatabaseRecordTest {

    @Value.Immutable
    @JsonSerialize(as=ImmutableExtendedRecord.class)
    @JsonDeserialize(as=ImmutableExtendedRecord.class)
    interface ExtendedRecord extends DatabaseRecord {
        String getName();
    }

    @Test
    void shouldRetainUnmappedFieldsInDataMap() throws Exception {
        // Given
        final ObjectMapper mapper = new ObjectMapper();

        final String jsonString = """
                {
                    "id": "id1",
                    "name": "my name",
                    "age": 47
                }
                """;

        // When
        final ExtendedRecord extended = mapper.readValue(jsonString, ExtendedRecord.class);

        // Then
        assertEquals("id1", extended.getId());
        assertEquals("my name", extended.getName());
        assertEquals(47, extended.getData().get("age"));
    }
}
