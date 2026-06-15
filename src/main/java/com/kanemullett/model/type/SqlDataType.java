package com.kanemullett.model.type;

/**
 * Enumeration of SQL data types used in
 * {@link com.kanemullett.model.ColumnDefinition} objects to define the
 * data type of a database column.
 */
public enum SqlDataType {

    /** Variable-length character string. */
    VARCHAR,

    /** 32-bit signed integer. */
    INTEGER,

    /** Timestamp without timezone information. */
    TIMESTAMP_WITHOUT_TIME_ZONE,

    /** Boolean true/false value. */
    BOOLEAN
}
