package com.kanemullett.function;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.jooq.CaseConditionStep;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.JSONB;
import org.jooq.Query;
import org.jooq.Table;
import org.jooq.impl.DSL;
import org.jooq.impl.SQLDataType;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kanemullett.model.DatabaseRecord;
import com.kanemullett.model.UpdateRequest;
import com.kanemullett.util.BuilderUtils;

/**
 * Function for building jOOQ {@link Query} objects from {@link UpdateRequest}
 * objects.
 *
 * <p>Handles INSERT, UPDATE, and DELETE operations. Records are converted from
 * typed {@link DatabaseRecord} instances to column/value maps via Jackson
 * before being used to construct the query. Collection, map and nested
 * object values are serialised to JSON and bound as {@code jsonb}, so the
 * corresponding columns should be declared as {@code jsonb}.
 *
 * <p>UPDATE queries use a bulk CASE/WHEN pattern to update multiple records
 * in a single statement, keyed on the {@code id} field.
 *
 * @param <T> the type of {@link DatabaseRecord} the query is built for.
 */
@Component
public class UpdateBuilderFunction {

    private final DSLContext dsl;
    private final ObjectMapper mapper;

    /**
     * Constructs a new {@code UpdateBuilderFunction} with the given
     * {@link DSLContext}.
     *
     * @param dsl the jOOQ DSL context used to construct query components.
     */
    public UpdateBuilderFunction(DSLContext dsl) {
        this.dsl = dsl;
        this.mapper = new ObjectMapper();
    }

    /**
     * Converts an {@link UpdateRequest} into a jOOQ {@link Query} based on
     * the request's {@link com.kanemullett.model.type.SqlOperator}.
     *
     * @param request the update request to convert.
     * @return the constructed {@link Query}.
     * @throws IllegalArgumentException if the operation is not INSERT, UPDATE,
     *                                  or DELETE.
     */
    public <T extends DatabaseRecord> Query apply(UpdateRequest<T> request) {
        return switch (request.getOperation()) {
            case INSERT -> buildInsert(request);
            case UPDATE -> buildUpdate(request);
            case DELETE -> buildDelete(request);
            default -> throw new IllegalArgumentException(
                "Unsupported operation: " + request.getOperation()
            );
        };
    }

    /**
     * Builds a jOOQ INSERT query from an {@link UpdateRequest}.
     *
     * <p>Columns are derived from the union of all keys across the provided
     * records, sorted alphabetically.
     *
     * @param request the update request containing the records to insert.
     * @return the constructed INSERT {@link Query}.
     */
    private <T extends DatabaseRecord> Query buildInsert(UpdateRequest<T> request) {
        final List<Map<String, Object>> records = toMaps(request.getRecords());

        final List<Field<Object>> columns = records.stream()
            .flatMap(r -> r.keySet().stream())
            .distinct()
            .sorted()
            .map(col -> DSL.field(DSL.name(col)))
            .toList();

        var insert = dsl.insertInto(buildTable(request.getTable()))
            .columns(columns);

        for (Map<String, Object> dbRecord : records) {
            insert = insert.values(columns.stream()
                .map(col -> dbRecord.get(col.getName()))
                .toList());
        }

        return insert;
    }

    /**
     * Builds a jOOQ UPDATE query from an {@link UpdateRequest} using a bulk
     * CASE/WHEN pattern.
     *
     * <p>All records must contain an {@code id} field. A CASE/WHEN clause is
     * generated per column, with a WHERE condition scoped to the provided
     * record IDs.
     *
     * <p>Unset ({@code null}) properties are treated as "leave unchanged":
     * they are omitted from the record, so that record's row falls through
     * to the CASE's {@code ELSE} branch and keeps its current value. A column
     * is only included in the SET clause if at least one record sets it.
     *
     * @param request the update request containing the records to update.
     * @return the constructed UPDATE {@link Query}.
     * @throws IllegalArgumentException if any record is missing the {@code id}
     *                                  field, or if no record sets any column
     *                                  other than {@code id}.
     */
    private <T extends DatabaseRecord> Query buildUpdate(UpdateRequest<T> request) {
        final List<Map<String, Object>> records = toMaps(request.getRecords());
        records.forEach(dbRecord -> dbRecord.values().removeIf(Objects::isNull));

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
            .toList();

        if (columns.isEmpty()) {
            throw new IllegalArgumentException(
                "Update requests should set at least one column other than id."
            );
        }

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
            .toList();

        return setStep.where(DSL.field(DSL.name("id")).in(ids));
    }

    /**
     * Builds a jOOQ CASE/WHEN field for a single column across multiple
     * records, falling back to the existing column value if no matching
     * record is found.
     *
     * @param column  the column name to build the CASE/WHEN field for.
     * @param records the records to build the CASE/WHEN clauses from.
     * @return the constructed CASE/WHEN {@link Field}.
     */
    private Field<Object> buildCaseField(String column, List<Map<String, Object>> records) {
        CaseConditionStep<Object> whenStep = null;

        for (Map<String, Object> dbRecord : records) {
            if (!dbRecord.containsKey(column)) {
                continue;
            }
            final Condition condition = DSL.field(DSL.name("id")).eq(dbRecord.get("id"));
            final Field<Object> result = toField(dbRecord.get(column));
            whenStep = whenStep == null
                ? DSL.when(condition, result)
                : whenStep.when(condition, result);
        }

        if (whenStep == null) {
            return DSL.field(DSL.name(column));
        }

        return whenStep.otherwise(DSL.field(DSL.name(column)));
    }

    /**
     * Builds a jOOQ DELETE query from an {@link UpdateRequest}, applying a
     * WHERE condition if a {@link com.kanemullett.model.QueryConditionGroup}
     * is present.
     *
     * @param request the update request to build the DELETE query from.
     * @return the constructed DELETE {@link Query}.
     */
    private <T extends DatabaseRecord> Query buildDelete(UpdateRequest<T> request) {
        var delete = dsl.deleteFrom(buildTable(request.getTable()));

        if (request.getConditionGroup() != null) {
            return delete.where(BuilderUtils.buildConditionGroup(request.getConditionGroup()));
        }

        return delete;
    }

    /**
     * Builds a jOOQ {@link Table} from a {@link com.kanemullett.model.Table},
     * applying an alias if present.
     *
     * @param table the table model to convert.
     * @return the constructed jOOQ table.
     */
    private Table<?> buildTable(com.kanemullett.model.Table table) {
        if (table.getAlias() != null) {
            return DSL.table(DSL.name(table.getSchema(), table.getTable()))
                .as(table.getAlias());
        }
        return DSL.table(DSL.name(table.getSchema(), table.getTable()));
    }

    /**
     * Converts a list of typed {@link DatabaseRecord} instances to a list of
     * column/value maps using Jackson.
     *
     * <p>Jackson converts collection, map and nested object properties into
     * raw {@link java.util.ArrayList} / {@link java.util.LinkedHashMap} instances, which
     * jOOQ has no binding for. Such values are therefore serialised to JSON
     * and wrapped as {@code jsonb} bind values.
     *
     * @param records the records to convert.
     * @return the list of column/value maps.
     */
    private <T extends DatabaseRecord> List<Map<String, Object>> toMaps(List<T> records) {
        return records.stream()
            .map(dbRecord -> {
                final Map<String, Object> map = mapper.convertValue(dbRecord, new TypeReference<Map<String, Object>>() {});
                map.remove("data");
                map.replaceAll((column, value) -> toBindValue(value));
                return map;
            })
            .toList();
    }

    /**
     * Converts a structured value (collection or map) into a {@code jsonb}
     * bind value. Any other value is returned unchanged.
     *
     * @param value the column value to convert.
     * @return a {@code jsonb} {@link Field} for structured values, otherwise
     *         the original value.
     * @throws IllegalArgumentException if the value cannot be serialised to
     *                                  JSON.
     */
    private Object toBindValue(Object value) {
        if (!(value instanceof Collection<?>) && !(value instanceof Map<?, ?>)) {
            return value;
        }
        try {
            return DSL.val(JSONB.valueOf(mapper.writeValueAsString(value)), SQLDataType.JSONB);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Unable to serialise value to JSON: " + value, e);
        }
    }

    /**
     * Wraps a column value as a jOOQ {@link Field}, passing through values
     * that are already fields (e.g. typed {@code jsonb} bind values).
     *
     * @param value the column value.
     * @return the value as a {@link Field}.
     */
    @SuppressWarnings("unchecked")
    private static Field<Object> toField(Object value) {
        return value instanceof Field<?> field ? (Field<Object>) field : DSL.val(value);
    }
}
