package com.kanemullett.service;

import org.jooq.DSLContext;
import org.jooq.Query;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Service;

import com.kanemullett.function.TableBuilderFunction;
import com.kanemullett.model.Table;
import com.kanemullett.model.TableDefinition;

/**
 * Service for managing database table lifecycle operations.
 *
 * <p>Provides functionality to create and delete database tables, delegating
 * query construction to {@link TableBuilderFunction} before executing the
 * resulting jOOQ queries via the injected {@link DSLContext}.
 */
@Service
public class DatabaseTableService {
    private final DSLContext dsl;
    private final TableBuilderFunction tableBuilderFunction;

    /**
     * Constructs a new {@code DatabaseTableService} with the given
     * {@link DSLContext} and {@link TableBuilderFunction}.
     *
     * @param dsl                  the jOOQ DSL context used to execute queries.
     * @param tableBuilderFunction the function used to build
     *                             {@code CREATE TABLE IF NOT EXISTS} queries.
     */
    public DatabaseTableService(DSLContext dsl, TableBuilderFunction tableBuilderFunction) {
        this.dsl = dsl;
        this.tableBuilderFunction = tableBuilderFunction;
    }

    /**
     * Creates a database table based on the given {@link TableDefinition}.
     *
     * <p>Executes a {@code CREATE TABLE IF NOT EXISTS} statement, so the
     * operation is safe to call even if the table already exists.
     *
     * @param tableDefinition the definition of the table to create.
     */
    public void createTable(TableDefinition tableDefinition) {
        final Query query = tableBuilderFunction.apply(tableDefinition);
        dsl.execute(query);
    }

    /**
     * Deletes a database table if it exists.
     *
     * <p>Executes a {@code DROP TABLE IF EXISTS} statement, so the operation
     * is safe to call even if the table does not exist.
     *
     * @param table the table to delete.
     */
    public void deleteTable(Table table) {
        dsl.dropTableIfExists(
            DSL.table(DSL.name(table.getSchema(), table.getTable()))
        ).execute();
    }
}
