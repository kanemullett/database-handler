package com.kanemullett.model.type;

/**
 * Enumeration of SQL join types used in {@link com.kanemullett.model.Join}
 * objects to define the relationship between tables or subqueries.
 */
public enum JoinType {

    /** Returns rows with matching values in both tables. */
    INNER,

    /** Returns all rows from the left table and matching rows from the right. */
    LEFT,

    /** Returns all rows from the right table and matching rows from the left. */
    RIGHT,

    /** Returns all rows from both tables, with nulls where there is no match. */
    OUTER,

    /**
     * Combines the results of two or more SELECT statements into a single
     * result set. Used internally to represent UNION set operations between
     * subqueries rather than a traditional join.
     */
    UNION
}
