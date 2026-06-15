package com.kanemullett.model.type;

/**
 * Enumeration of logical join operators used to combine multiple
 * {@link com.kanemullett.model.QueryCondition} objects within a
 * {@link com.kanemullett.model.QueryConditionGroup}.
 */
public enum ConditionJoin {

    /** Combines conditions with a logical AND. */
    AND,

    /** Combines conditions with a logical OR. */
    OR
}
