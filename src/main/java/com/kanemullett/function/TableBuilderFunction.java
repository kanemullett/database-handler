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

@Component
public class TableBuilderFunction implements Function<TableDefinition, Query> {

    private final DSLContext dsl;
    
    public TableBuilderFunction(DSLContext dsl) {
        this.dsl = dsl;
    }

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

    private DataType<?> mapDataType(SqlDataType dataType) {
        return switch (dataType) {
            case VARCHAR -> SQLDataType.VARCHAR;
            case INTEGER -> SQLDataType.INTEGER;
            case TIMESTAMP_WITHOUT_TIME_ZONE -> SQLDataType.TIMESTAMP;
            case BOOLEAN -> SQLDataType.BOOLEAN;
        };
    }
}
