package com.kanemullett.model.type;

/**
 * Enumeration of SQL operators used in {@link com.kanemullett.model.UpdateRequest}
 * objects to define the type of database operation to perform.
 */
public enum SqlOperator {

    /** Retrieves records from the database. */
    SELECT,

    /** Inserts new records into the database. */
    INSERT,

    /** Updates existing records in the database. */
    UPDATE,

    /** Deletes records from the database. */
    DELETE
}
