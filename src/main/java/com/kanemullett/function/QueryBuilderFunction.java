package com.kanemullett.function;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.SelectQuery;
import org.jooq.Table;
import org.jooq.TableLike;
import org.jooq.conf.ParamType;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Component;

import com.kanemullett.model.Column;
import com.kanemullett.model.DatabaseRecord;
import com.kanemullett.model.Join;
import com.kanemullett.model.QueryJoin;
import com.kanemullett.model.QueryRequest;
import com.kanemullett.model.TableJoin;
import com.kanemullett.model.type.JoinType;
import com.kanemullett.model.type.OrderDirection;
import com.kanemullett.util.BuilderUtils;

/**
 * Function for building jOOQ {@link SelectQuery} objects from
 * {@link QueryRequest} objects.
 *
 * <p>Handles the full range of SELECT query features including distinct
 * selection, column aliases, table aliases, inner/outer/left/right joins,
 * subquery joins, UNION subqueries, WHERE conditions, GROUP BY, ORDER BY,
 * and SQL functions.
 *
 * @param <T> the type of {@link DatabaseRecord} the query is built for.
 */
@Component
public class QueryBuilderFunction {

    private final DSLContext dsl;

    /**
     * Constructs a new {@code QueryBuilderFunction} with the given
     * {@link DSLContext}.
     *
     * @param dsl the jOOQ DSL context used to construct query components.
     */
    public QueryBuilderFunction(DSLContext dsl) {
        this.dsl = dsl;
    }

    /**
     * Converts a {@link QueryRequest} into a jOOQ {@link SelectQuery}.
     *
     * @param request the query request to convert.
     * @return the constructed {@link SelectQuery}.
     */
    public <T extends DatabaseRecord> SelectQuery<?> apply(QueryRequest<T> request) {
        return buildSelect(request);
    }

    /**
     * Builds a {@link SelectQuery} from a {@link QueryRequest}.
     *
     * @param request the query request to build from.
     * @param <R>     the record type.
     * @return the constructed {@link SelectQuery}.
     */
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

    /**
     * Builds a jOOQ {@link org.jooq.Field} from a {@link com.kanemullett.model.Function},
     * handling nested functions, schema-qualified names, and aliases.
     *
     * @param function the function model to convert.
     * @return the constructed jOOQ field.
     */
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
     * Builds and adds a join to the given {@link SelectQuery} from a
     * {@link Join} object. Handles both {@link TableJoin} and
     * {@link QueryJoin} types.
     *
     * @param query the query to add the join to.
     * @param join  the join to build.
     */
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

    /**
     * Builds a {@link TableLike} subquery from a {@link QueryJoin}, wrapping
     * the result in parentheses and applying an alias if present.
     *
     * @param queryJoin the query join to build the subquery from.
     * @return the constructed subquery as a {@link TableLike}.
     */
    private TableLike<?> buildSubquery(QueryJoin<?> queryJoin) {
        final String sql = buildSubqueryHelper((QueryJoin<?>) queryJoin);

        return queryJoin.getAlias() != null
            ? DSL.table("(" + sql + ")").as(queryJoin.getAlias())
            : DSL.table("(" + sql + ")");
    }

    /**
     * Recursively builds a SQL string for a subquery, handling UNION joins
     * by combining multiple SELECT statements with UNION.
     *
     * @param queryJoin the query join to build the subquery SQL from.
     * @param <R>       the record type.
     * @return the constructed SQL string.
     */
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

    /**
     * Maps a {@link JoinType} to the corresponding jOOQ {@link org.jooq.JoinType}.
     *
     * @param joinType the join type to map.
     * @return the corresponding jOOQ join type.
     */
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
