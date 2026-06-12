package com.kanemullett.function;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Select;
import org.jooq.SelectQuery;
import org.jooq.Table;
import org.jooq.TableLike;
import org.jooq.conf.ParamType;
import org.jooq.impl.DSL;

import com.kanemullett.model.Column;
import com.kanemullett.model.DatabaseRecord;
import com.kanemullett.model.Join;
import com.kanemullett.model.QueryJoin;
import com.kanemullett.model.QueryRequest;
import com.kanemullett.model.TableJoin;
import com.kanemullett.model.type.JoinType;
import com.kanemullett.model.type.OrderDirection;
import com.kanemullett.util.BuilderUtils;

public class QueryBuilderFunction<T extends DatabaseRecord> implements Function<QueryRequest<T>, SelectQuery<?>> {

    private final DSLContext dsl;

    public QueryBuilderFunction(DSLContext dsl) {
        this.dsl = dsl;
    }

    @Override
    public SelectQuery<?> apply(QueryRequest<T> request) {
        return buildSelect(request);
    }

    private <R extends DatabaseRecord> SelectQuery<?> buildSelect(QueryRequest<R> request) {
        final SelectQuery<?> query = request.getDistinct()
            ? dsl.selectDistinct().getQuery()
            : dsl.selectQuery();

        if (request.getColumns() == null) {
            query.addSelect(DSL.asterisk());
        } else {
            query.addSelect(request.getColumns().stream()
                .map(col -> {
                    if (col instanceof com.kanemullett.model.Function func) {
                        return buildFunction(func);
                    }
                    return col.getAlias() != null
                        ? DSL.field(DSL.name(col.getParts().toArray(String[]::new))).as(col.getAlias())
                        : DSL.field(DSL.name(col.getParts().toArray(String[]::new)));
                })
                .collect(Collectors.toList()));
        }

        query.addFrom(buildTable(request.getTable()));

        if (request.getJoins() != null) {
            request.getJoins().stream()
                .filter(join -> join.getJoinType() != JoinType.UNION)
                .forEach(join -> buildJoin(query, join));
        }

        if (request.getConditionGroup() != null) {
            query.addConditions(BuilderUtils.buildConditionGroup(request.getConditionGroup()));
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

    private org.jooq.Field<?> buildFunction(com.kanemullett.model.Function function) {
        final String functionName = function.getParts().size() == 2
            ? "\"" + function.getParts().get(0) + "\"." + function.getParts().get(1)
            : function.getParts().get(function.getParts().size() - 1);

        final org.jooq.Field<?>[] args = function.getArgs().stream()
            .map(arg -> {
                if (arg instanceof com.kanemullett.model.Function nestedFunc) {
                    return (org.jooq.Field<?>) buildFunction(nestedFunc);
                }
                if (arg instanceof Column col) {
                    return (org.jooq.Field<?>) DSL.field(DSL.name(col.getParts().toArray(String[]::new)));
                }
                return DSL.val(arg);
            })
            .toArray(org.jooq.Field[]::new);

        final org.jooq.Field<?> field = DSL.function(functionName, Object.class, args);

        return function.getAlias() != null
            ? field.as(function.getAlias())
            : field;
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
            ? BuilderUtils.buildCondition(join.getJoinCondition())
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
        final String sql = buildSubqueryHelper((QueryJoin<?>) queryJoin);

        return queryJoin.getAlias() != null
            ? DSL.table("(" + sql + ")").as(queryJoin.getAlias())
            : DSL.table("(" + sql + ")");
    }

    private <R extends DatabaseRecord> String buildSubqueryHelper(QueryJoin<R> queryJoin) {
        List<String> unionParts = new ArrayList<>();
        
        // build base query without its UNION joins
        unionParts.add(buildSelect(queryJoin.getQuery()).getSQL(ParamType.INLINED));

        if (queryJoin.getQuery().getJoins() != null) {
            for (Join join : queryJoin.getQuery().getJoins()) {
                if (join instanceof QueryJoin<?> innerJoin && join.getJoinType() == JoinType.UNION) {
                    @SuppressWarnings("unchecked")
                    String unionSql = buildSelect(((QueryJoin<R>) innerJoin).getQuery()).getSQL(ParamType.INLINED);
                    unionParts.add(unionSql);
                }
            }
        }

        return String.join(" union ", unionParts);
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
}
