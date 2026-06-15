package com.kanemullett.function;

import java.util.function.Function;

import org.jooq.Constraint;
import org.jooq.CreateTableElementListStep;
import org.jooq.DSLContext;
import org.jooq.DataType;
import org.jooq.Query;
import org.jooq.impl.DSL;
import org.jooq.impl.SQLDataType;
import org.springframework.stereotype.Component;

import com.kanemullett.model.ColumnDefinition;
import com.kanemullett.model.TableDefinition;
import com.kanemullett.model.type.SqlDataType;

/**
 * Function for building jOOQ {@link Query} objects representing SQL
 * {@code CREATE TABLE IF NOT EXISTS} statements from {@link TableDefinition}
 * objects.
 *
 * <p>Handles column definitions with their respective data types and
 * primary key constraints.
 */
@Component
public class TableBuilderFunction implements Function<TableDefinition, Query> {

    private final DSLContext dsl;
    
    /**
     * Constructs a new {@code TableBuilderFunction} with the given
     * {@link DSLContext}.
     *
     * @param dsl the jOOQ DSL context used to construct query components.
     */
    public TableBuilderFunction(DSLContext dsl) {
        this.dsl = dsl;
    }

    /**
     * Converts a {@link TableDefinition} into a jOOQ {@link Query}
     * representing a {@code CREATE TABLE IF NOT EXISTS} statement.
     *
     * @param tableDefinition the table definition to convert.
     * @return the constructed {@link Query}.
     */
    @Override
    public Query apply(TableDefinition tableDefinition) {
        CreateTableElementListStep query = dsl.createTableIfNotExists(
            DSL.table(DSL.name(
                tableDefinition.getSchema(),
                tableDefinition.getTable()
            ))
        );
        
        for (ColumnDefinition column : tableDefinition.getColumns()) {
            query = query.column(
                DSL.field(DSL.name(column.getName())),
                mapDataType(column.getDataType())
            );
        }

        if (tableDefinition.getColumns().stream().anyMatch(ColumnDefinition::getPrimaryKey)) {
            query = query.constraints(
                tableDefinition.getColumns().stream()
                    .filter(ColumnDefinition::getPrimaryKey)
                    .map(col -> DSL.primaryKey(col.getName()))
                    .toArray(Constraint[]::new)                
            );
        }

        return query;
    }

    /**
     * Maps a {@link SqlDataType} to the corresponding jOOQ {@link DataType}.
     *
     * @param dataType the data type to map.
     * @return the corresponding jOOQ data type.
     */
    private DataType<?> mapDataType(SqlDataType dataType) {
        return switch (dataType) {
            case VARCHAR -> SQLDataType.VARCHAR;
            case INTEGER -> SQLDataType.INTEGER;
            case TIMESTAMP_WITHOUT_TIME_ZONE -> SQLDataType.TIMESTAMP;
            case BOOLEAN -> SQLDataType.BOOLEAN;
        };
    }
}
