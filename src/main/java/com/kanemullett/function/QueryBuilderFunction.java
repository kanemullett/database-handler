package com.kanemullett.function;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.SelectQuery;
import org.jooq.Table;
import org.jooq.TableLike;
import org.jooq.impl.DSL;

import com.kanemullett.model.Column;
import com.kanemullett.model.Join;
import com.kanemullett.model.QueryCondition;
import com.kanemullett.model.QueryConditionGroup;
import com.kanemullett.model.QueryJoin;
import com.kanemullett.model.QueryRequest;
import com.kanemullett.model.TableJoin;
import com.kanemullett.model.type.JoinType;
import com.kanemullett.model.type.OrderDirection;

public class QueryBuilderFunction implements Function<QueryRequest, SelectQuery<?>> {

    private final DSLContext dsl;

    public QueryBuilderFunction(DSLContext dsl) {
        this.dsl = dsl;
    }

    @Override
    public SelectQuery<?> apply(QueryRequest request) {
        final SelectQuery<?> query = request.isDistinct()
            ? dsl.selectDistinct().getQuery()
            : dsl.selectQuery();

        if (request.getColumns() == null) {
            query.addSelect(DSL.asterisk());
        } else {
            query.addSelect(request.getColumns().stream()
                .map(col -> col.getAlias() != null
                    ? DSL.field(DSL.name(col.getParts().toArray(String[]::new))).as(col.getAlias())
                    : DSL.field(DSL.name(col.getParts().toArray(String[]::new))))
                .collect(Collectors.toList()));
        }

        query.addFrom(buildTable(request.getTable()));

        if (request.getJoins() != null) {
            request.getJoins().forEach(join -> buildJoin(query, join));
        }

        if (request.getConditionGroup() != null) {
            query.addConditions(buildConditionGroup(request.getConditionGroup()));
        }

        if (request.getGroupBy() != null) {
            query.addGroupBy(request.getGroupBy().getColumns().stream()
                .map(col -> DSL.field(DSL.name(col.getParts())))
                .collect(Collectors.toList()));
        }

        if (request.getOrderBy() != null) {
            query.addOrderBy(request.getOrderBy().getDirection() == OrderDirection.ASCENDING
                ? DSL.field(DSL.name(request.getOrderBy().getColumn().getParts().toArray(String[]::new))).asc()
                : DSL.field(DSL.name(request.getOrderBy().getColumn().getParts().toArray(String[]::new))).desc());
        }

        return query;
    }

    private Table<?> buildTable(com.kanemullett.model.Table table) {
        if (table.getAlias() != null) {
            return DSL.table(DSL.name(table.getSchema(), table.getTable()))
                .as(table.getAlias());
        }
        return DSL.table(DSL.name(table.getSchema(), table.getTable()));
    }

    private void buildJoin(SelectQuery<?> query, Join join) {
        final JoinType joinType = join.getJoinType();
        final Condition joinCondition = join.getJoinCondition() != null
            ? buildCondition(join.getJoinCondition())
            : DSL.noCondition();

        if (join instanceof TableJoin tableJoin) {
            query.addJoin(
                buildTable(tableJoin.getTable()),
                mapJoinType(joinType),
                joinCondition
            );
        } else if (join instanceof QueryJoin<?> queryJoin) {
            final TableLike<?> subquery = buildSubquery(queryJoin);
            query.addJoin(subquery, mapJoinType(joinType), joinCondition);
        }
    }

    private TableLike<?> buildSubquery(QueryJoin<?> queryJoin) {
        final Table<?> derived = queryJoin.getQuery() != null
            ? DSL.table("(" + queryJoin.getQuery().toString() + ")")
            : DSL.table("");

        return queryJoin.getAlias() != null
            ? derived.as(queryJoin.getAlias())
            : derived;
    }

    private org.jooq.JoinType mapJoinType(JoinType joinType) {
        return switch (joinType) {
            case INNER -> org.jooq.JoinType.JOIN;
            case LEFT -> org.jooq.JoinType.LEFT_OUTER_JOIN;
            case RIGHT -> org.jooq.JoinType.RIGHT_OUTER_JOIN;
            case OUTER -> org.jooq.JoinType.FULL_OUTER_JOIN;
            case UNION -> org.jooq.JoinType.CROSS_JOIN;
        };
    }

    private Condition buildConditionGroup(QueryConditionGroup group) {
        final List<Condition> conditions = group.getConditions().stream()
            .map(this::buildCondition)
            .collect(Collectors.toList());

        return switch (group.getJoin()) {
            case AND -> DSL.and(conditions);
            case OR -> DSL.or(conditions);
        };
    }

    private Condition buildCondition(QueryCondition queryCondition) {
        final Field<Object> field = DSL.field(DSL.name(
            queryCondition.getColumn().getParts().toArray(String[]::new)
        ));

        final Object value = queryCondition.getValue();

        final Field<Object> valueField = value instanceof Column col
            ? DSL.field(DSL.name(col.getParts().toArray(String[]::new)))
            : null;

        return switch (queryCondition.getOperator()) {
            case EQUAL -> valueField != null ? field.eq(valueField) : field.eq(value);
            case LESS_THAN -> valueField != null ? field.lt(valueField) : field.lt(value);
            case GREATER_THAN -> valueField != null ? field.gt(valueField) : field.gt(value);
            case IN -> field.in(value);
        };
    }
}
