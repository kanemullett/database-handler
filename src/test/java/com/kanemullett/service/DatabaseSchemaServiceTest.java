package com.kanemullett.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.jooq.CreateSchemaFinalStep;
import org.jooq.DSLContext;
import org.jooq.DropSchemaFinalStep;
import org.jooq.DropSchemaStep;
import org.jooq.Name;
import org.jooq.impl.DSL;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class DatabaseSchemaServiceTest {
    private DatabaseSchemaService service;

    private final DSLContext dsl = mock(DSLContext.class);

    @BeforeEach
    void setUp() {
        service = new DatabaseSchemaService(dsl);
    }

    @Test
    void shouldCreateSchema() {
        // Given
        final CreateSchemaFinalStep step = mock(CreateSchemaFinalStep.class);

        when(dsl.createSchemaIfNotExists(any(Name.class)))
            .thenReturn(step);

        // When
        service.createSchema("test-schema");

        // Then
        verify(dsl).createSchemaIfNotExists(DSL.name("test-schema"));
        verify(step).execute();
    }

    @Test
    void shouldDeleteSchemaWithRestrict() {
        // Given
        final DropSchemaStep dropStep = mock(DropSchemaStep.class);
        final DropSchemaFinalStep finalStep = mock(DropSchemaFinalStep.class);

        when(dsl.dropSchemaIfExists(any(Name.class)))
            .thenReturn(dropStep);
        when(dropStep.restrict())
            .thenReturn(finalStep);

        // When
        service.deleteSchema("test-schema", false);

        // Then
        verify(dsl).dropSchemaIfExists(DSL.name("test-schema"));
        verify(dropStep).restrict();
        verify(dropStep, never()).cascade();
    }

    @Test
    void shouldDeleteSchemaWithCascade() {
        // Given
        final DropSchemaStep dropStep = mock(DropSchemaStep.class);
        final DropSchemaFinalStep finalStep = mock(DropSchemaFinalStep.class);

        when(dsl.dropSchemaIfExists(any(Name.class)))
            .thenReturn(dropStep);
        when(dropStep.cascade())
            .thenReturn(finalStep);

        // When
        service.deleteSchema("test-schema", true);

        // Then
        verify(dsl).dropSchemaIfExists(DSL.name("test-schema"));
        verify(dropStep).cascade();
        verify(dropStep, never()).restrict();
    }
}
