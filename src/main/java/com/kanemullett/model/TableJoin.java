package com.kanemullett.model;

import org.immutables.value.Value.Immutable;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.kanemullett.model.type.JoinType;

/**
 * Represents a SQL join against a database table, extending {@link Join} with
 * a {@link Table} reference.
 *
 * <p>Used within {@link QueryRequest} to define joins between tables in a
 * SELECT query.
 *
 * <p>Instances are created via the
 * {@link #of(Table, QueryCondition, JoinType)} factory method:
 * <pre>
 * TableJoin.of(
 *     Table.of("my_schema", "my_table", "my_alias"),
 *     QueryCondition.of(
 *         Column.of("base_table", "id"),
 *         Column.of("my_alias", "baseId")
 *     ),
 *     JoinType.INNER
 * );
 * </pre>
 */
@Immutable
@JsonSerialize(as=ImmutableTableJoin.class)
@JsonDeserialize(as=ImmutableTableJoin.class)
public interface TableJoin extends Join {

    /**
     * Returns the table to join against.
     *
     * @return the {@link Table}.
     */
    Table getTable();

    /**
     * Creates a new {@code TableJoin} with the given table, join condition,
     * and join type.
     *
     * @param table         the table to join against.
     * @param joinCondition the condition to join on.
     * @param joinType      the type of join to perform.
     * @return the constructed {@code TableJoin}.
     */
    static TableJoin of(Table table, QueryCondition joinCondition, JoinType joinType) {
        return ImmutableTableJoin.builder()
            .table(table)
            .joinCondition(joinCondition)
            .joinType(joinType)
            .build();
    }
}
