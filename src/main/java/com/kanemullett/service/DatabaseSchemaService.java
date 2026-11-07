package com.kanemullett.service;

import org.jooq.DSLContext;
import org.jooq.impl.DSL;

/**
 * Service for managing database schema lifecycle operations.
 *
 * <p>Provides functionality to create and delete schemas using the injected
 * {@link DSLContext}. Schema names are quoted, so they are case-sensitive.
 */
public class DatabaseSchemaService {

    private final DSLContext dsl;

    /**
     * Constructs a new {@code DatabaseSchemaService} with the given
     * {@link DSLContext}.
     *
     * @param dsl the jOOQ DSL context used to execute queries.
     */
    public DatabaseSchemaService(DSLContext dsl) {
        this.dsl = dsl;
    }

    /**
     * Creates a schema if it does not already exist.
     *
     * <p>Executes a {@code CREATE SCHEMA IF NOT EXISTS} statement, so the
     * operation is safe to call even if the schema already exists.
     *
     * @param schema the name of the schema to create.
     */
    public void createSchema(String schema) {
        dsl.createSchemaIfNotExists(DSL.name(schema)).execute();
    }

    /**
     * Deletes a schema if it exists.
     *
     * <p>Executes a {@code DROP SCHEMA IF EXISTS} statement, so the operation
     * is safe to call even if the schema does not exist. With {@code cascade}
     * set to {@code false} ({@code RESTRICT}), the drop fails if the schema
     * still contains any objects. With {@code cascade} set to {@code true}
     * ({@code CASCADE}), every table and other object in the schema is dropped
     * with it, along with all of their data.
     *
     * @param schema  the name of the schema to delete.
     * @param cascade {@code true} to also drop everything in the schema,
     *                {@code false} to fail if the schema is not empty.
     */
    public void deleteSchema(String schema, boolean cascade) {
        if (cascade) {
            dsl.dropSchemaIfExists(DSL.name(schema)).cascade().execute();
        } else {
            dsl.dropSchemaIfExists(DSL.name(schema)).restrict().execute();
        }
    }
}
