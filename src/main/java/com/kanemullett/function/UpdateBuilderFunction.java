package com.kanemullett.function;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.jooq.CaseConditionStep;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Query;
import org.jooq.Table;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kanemullett.model.DatabaseRecord;
import com.kanemullett.model.UpdateRequest;
import com.kanemullett.util.BuilderUtils;

@Component
public class UpdateBuilderFunction<T extends DatabaseRecord> implements Function<UpdateRequest<T>, Query> {

    private final DSLContext dsl;
    private final ObjectMapper mapper;

    public UpdateBuilderFunction(DSLContext dsl) {
        this.dsl = dsl;
        this.mapper = new ObjectMapper();
    }

    @Override
    public Query apply(UpdateRequest<T> request) {
        return switch (request.getOperation()) {
            case INSERT -> buildInsert(request);
            case UPDATE -> buildUpdate(request);
            case DELETE -> buildDelete(request);
            default -> throw new IllegalArgumentException(
                "Unsupported operation: " + request.getOperation()
            );
        };
    }

    private Query buildInsert(UpdateRequest<T> request) {
        final List<Map<String, Object>> records = toMaps(request.getRecords());

        final List<Field<?>> columns = records.stream()
            .flatMap(r -> r.keySet().stream())
            .distinct()
            .sorted()
            .map(col -> DSL.field(DSL.name(col)))
            .collect(Collectors.toList());

        var insert = dsl.insertInto(buildTable(request.getTable()))
            .columns(columns);

        for (Map<String, Object> record : records) {
            insert = insert.values(columns.stream()
                .map(col -> record.get(col.getName()))
                .collect(Collectors.toList()));
        }

        return insert;
    }

    private Query buildUpdate(UpdateRequest<T> request) {
        final List<Map<String, Object>> records = toMaps(request.getRecords());

        final boolean allHaveId = records.stream().allMatch(r -> r.containsKey("id"));

        if (!allHaveId) {
            throw new IllegalArgumentException(
                "All records in update requests should contain id field."
            );
        }

        final List<String> columns = records.stream()
            .flatMap(r -> r.keySet().stream())
            .filter(k -> !k.equals("id"))
            .distinct()
            .sorted()
            .collect(Collectors.toList());

        var update = dsl.update(buildTable(request.getTable()));

        var setStep = update.<Object>set(
            DSL.field(DSL.name(columns.get(0))),
            (Object) buildCaseField(columns.get(0), records)
        );

        for (int i = 1; i < columns.size(); i++) {
            final String column = columns.get(i);
            setStep = setStep.<Object>set(
                DSL.field(DSL.name(column)),
                (Object) buildCaseField(column, records)
            );
        }

        final List<Object> ids = records.stream()
            .map(r -> r.get("id"))
            .distinct()
            .collect(Collectors.toList());

        return setStep.where(DSL.field(DSL.name("id")).in(ids));
    }

    private Field<Object> buildCaseField(String column, List<Map<String, Object>> records) {
        var caseStep = DSL.case_();

        var whenStep = records.stream()
            .filter(r -> r.containsKey(column))
            .reduce(
                (CaseConditionStep<Object>) null,
                (acc, record) -> {
                    var condition = DSL.field(DSL.name("id")).eq(record.get("id"));
                    return acc == null
                        ? caseStep.when(condition, record.get(column))
                        : acc.when(condition, record.get(column));
                },
                (a, b) -> b
            );

        return whenStep.otherwise(DSL.field(DSL.name(column)));
    }

    private Query buildDelete(UpdateRequest<T> request) {
        var delete = dsl.deleteFrom(buildTable(request.getTable()));

        if (request.getConditionGroup() != null) {
            return delete.where(BuilderUtils.buildConditionGroup(request.getConditionGroup()));
        }

        return delete;
    }

    private Table<?> buildTable(com.kanemullett.model.Table table) {
        if (table.getAlias() != null) {
            return DSL.table(DSL.name(table.getSchema(), table.getTable()))
                .as(table.getAlias());
        }
        return DSL.table(DSL.name(table.getSchema(), table.getTable()));
    }

    private List<Map<String, Object>> toMaps(List<T> records) {
        return records.stream()
            .map(record -> mapper.convertValue(
                record, new TypeReference<Map<String, Object>>() {}))
            .collect(Collectors.toList());
    }
}
