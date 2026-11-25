package com.kanemullett.testing;

import static org.junit.Assert.assertEquals;

import java.util.List;

import com.kanemullett.model.Column;
import com.kanemullett.model.OrderBy;
import com.kanemullett.model.QueryCondition;
import com.kanemullett.model.QueryConditionGroup;
import com.kanemullett.model.Table;
import com.kanemullett.model.TableJoin;
import com.kanemullett.model.type.ConditionJoin;
import com.kanemullett.model.type.ConditionOperator;
import com.kanemullett.model.type.JoinType;
import com.kanemullett.model.type.OrderDirection;

import jakarta.annotation.Nullable;

/**
 * Static assertion methods for Database Handler model objects, for use in
 * the tests of applications that consume this library.
 *
 * <p>Published in the {@code database-handler} test-jar. Consuming apps add
 * it as a test-scoped dependency:
 * <pre>
 * &lt;dependency&gt;
 *   &lt;groupId&gt;com.kanemullett&lt;/groupId&gt;
 *   &lt;artifactId&gt;database-handler&lt;/artifactId&gt;
 *   &lt;version&gt;${database-handler.version}&lt;/version&gt;
 *   &lt;type&gt;test-jar&lt;/type&gt;
 *   &lt;scope&gt;test&lt;/scope&gt;
 * &lt;/dependency&gt;
 * </pre>
 *
 * <p>and import the methods statically:
 * <pre>
 * import static com.kanemullett.testing.DatabaseHandlerAssertions.*;
 * </pre>
 */
public final class DatabaseHandlerAssertions {

    private DatabaseHandlerAssertions() {
    }

    public static void assertColumn(Column expected, Column actual) {
        assertColumn(expected.getParts(), expected.getAlias(), actual);
    }

    public static void assertColumn(List<String> expectedParts, Column actual) {
        assertColumn(expectedParts, null, actual);
    }

    public static void assertColumn(
            List<String> expectedParts,
            @Nullable String expectedAlias,
            Column actual) {
        assertEquals(expectedParts, actual.getParts());
        assertEquals(expectedAlias, actual.getAlias());
    }

    public static void assertJoinCondition(QueryCondition expected, QueryCondition actual) {
        assertJoinCondition(expected, actual);
    }

    public static void assertJoinCondition(
            Column expectedColumn,
            ConditionOperator expectedOperator,
            Column expectedValue,
            QueryCondition actual) {
        assertColumn(expectedColumn, actual.getColumn());
        assertEquals(expectedOperator, actual.getOperator());
        assertColumn((Column) expectedValue, (Column) actual.getValue());
    }

    public static void assertOrderBy(OrderBy expected, OrderBy actual) {
        assertOrderBy(expected.getColumn(), expected.getDirection(), actual);
    }

    public static void assertOrderBy(
            Column expectedColumn,
            OrderDirection expectedDirection,
            OrderBy actual) {
        assertColumn(expectedColumn, actual.getColumn());
        assertEquals(expectedDirection, actual.getDirection());
    }

    public static void assertQueryCondition(QueryCondition expected, QueryCondition actual) {
        assertQueryCondition(expected.getColumn(), expected.getOperator(), expected.getValue(), actual);
    }

    public static void assertQueryCondition(
            Column expectedColumn,
            ConditionOperator expectedOperator,
            QueryCondition actual) {
        assertQueryCondition(expectedColumn, expectedOperator, null, actual);
    }

    public static void assertQueryCondition(
            Column expectedColumn,
            ConditionOperator expectedOperator,
            @Nullable Object expectedValue,
            QueryCondition actual) {
        assertColumn(expectedColumn, actual.getColumn());
        assertEquals(expectedOperator, actual.getOperator());
        assertEquals(expectedValue, actual.getValue());
    }

    public static void assertQueryConditionGroup(QueryConditionGroup expected, QueryConditionGroup actual) {
        assertQueryConditionGroup(expected.getConditions(), expected.getJoin(), actual);
    }

    public static void assertQueryConditionGroup(
            List<QueryCondition> expectedConditions,
            ConditionJoin expectedJoin,
            QueryConditionGroup actual) {
        assertEquals(expectedConditions.size(), actual.getConditions().size());
        for (int i = 0; i < expectedConditions.size(); i++) {
            assertQueryCondition(expectedConditions.get(i), actual.getConditions().get(i));
        }
        assertEquals(expectedJoin, actual.getJoin());
    }

    public static void assertTable(Table expected, Table actual) {
        assertTable(expected.getSchema(), expected.getTable(), expected.getAlias(), actual);
    }

    public static void assertTable(
            String expectedSchema,
            String expectedTable,
            Table actual) {
        assertTable(expectedSchema, expectedTable, null, actual);
    }

    public static void assertTable(
            String expectedSchema,
            String expectedTable,
            @Nullable String expectedAlias,
            Table actual) {
        assertEquals(expectedSchema, actual.getSchema());
        assertEquals(expectedTable, actual.getTable());
        assertEquals(expectedAlias, actual.getAlias());
    }

    public static void assertTableJoin(TableJoin expected, TableJoin actual) {
        assertTableJoin(expected.getTable(), expected.getJoinCondition(), expected.getJoinType(), actual);
    }

    public static void assertTableJoin(
            Table expectedTable,
            QueryCondition expectedJoinCondition,
            JoinType expectedJoinType,
            TableJoin actual) {
        assertTable(expectedTable, actual.getTable());
        assertJoinCondition(expectedJoinCondition, actual.getJoinCondition());
        assertEquals(expectedJoinType, actual.getJoinType());
    }
}
