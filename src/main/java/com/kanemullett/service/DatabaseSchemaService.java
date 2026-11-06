package com.kanemullett.service;

import org.jooq.DSLContext;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Service;

@Service
public class DatabaseSchemaService {

    private final DSLContext dsl;

    public DatabaseSchemaService(DSLContext dsl) {
        this.dsl = dsl;
    }

    public void createSchema(String schema) {
        dsl.createSchemaIfNotExists(DSL.name(schema)).execute();
    }

    public void deleteSchema(String schema, boolean cascade) {
        if (cascade) {
            dsl.dropSchemaIfExists(DSL.name(schema)).cascade().execute();
        } else {
            dsl.dropSchemaIfExists(DSL.name(schema)).restrict().execute();
        }
    }
}
