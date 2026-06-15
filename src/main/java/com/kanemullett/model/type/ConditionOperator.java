package com.kanemullett.model.type;

/**
 * Enumeration of comparison operators used in
 * {@link com.kanemullett.model.QueryCondition} objects to define the
 * relationship between a column and its value.
 */
public enum ConditionOperator {

    /** Matches values equal to the specified value. */
    EQUAL,

    /** Matches values less than the specified value. */
    LESS_THAN,

    /** Matches values greater than the specified value. */
    GREATER_THAN,

    /** Matches values contained within the specified collection. */
    IN
}
