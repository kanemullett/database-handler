package com.kanemullett.service;

import org.jooq.DSLContext;
import org.jooq.Query;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Service;

import com.kanemullett.function.TableBuilderFunction;
import com.kanemullett.model.Table;
import com.kanemullett.model.TableDefinition;

@Service
public class DatabaseTableService {
    private final DSLContext dsl;
    private final TableBuilderFunction tableBuilderFunction;

    public DatabaseTableService(DSLContext dsl, TableBuilderFunction tableBuilderFunction) {
        this.dsl = dsl;
        this.tableBuilderFunction = tableBuilderFunction;
    }

    public void createTable(TableDefinition tableDefinition) {
        final Query query = tableBuilderFunction.apply(tableDefinition);
        dsl.execute(query);
    }

    public void deleteTable(Table table) {
        dsl.dropTableIfExists(
            DSL.table(DSL.name(table.getSchema(), table.getTable()))
        ).execute();
    }
}
