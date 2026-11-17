package com.kanemullett.service;

import java.util.List;

import org.jooq.DSLContext;
import org.jooq.Query;
import org.jooq.impl.DSL;

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
     * operation is safe to call even if the table already exists. To create
     * several tables together, use {@link #createTables(List)}.
     *
     * @param tableDefinition the definition of the table to create.
     */
    public void createTable(TableDefinition tableDefinition) {
        final Query query = tableBuilderFunction.apply(tableDefinition);
        dsl.execute(query);
    }

    /**
     * Creates database tables based on the given {@link TableDefinition}s, as
     * a single unit.
     *
     * <p>Every {@code CREATE TABLE IF NOT EXISTS} statement is built before
     * any is executed, so a definition that cannot be turned into SQL fails
     * the call without touching the database. The statements are then sent
     * as one batch inside one transaction: if any of them fails, the
     * transaction is rolled back and none of the tables are created.
     *
     * <p>Tables that already exist are left as they are, so the operation is
     * safe to repeat.
     *
     * @param tableDefinitions the definitions of the tables to create, in the
     *                         order they should be created.
     * @throws org.jooq.exception.DataAccessException if creating any of the
     *                                                tables fails.
     */
    public void createTables(List<TableDefinition> tableDefinitions) {
        final List<Query> queries = tableDefinitions.stream()
            .map(tableBuilderFunction)
            .toList();

        dsl.transaction(configuration -> DSL.using(configuration).batch(queries).execute());
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
