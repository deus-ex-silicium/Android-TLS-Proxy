package com.nibiru.evil_ap.log;

import android.provider.BaseColumns;

/**
 * Created by Nibiru on 2016-12-21.
 */

final class LogDbContract {
    // To prevent someone from accidentally instantiating the contract class,
    // make the constructor private.
    private LogDbContract() {}

    /* Inner class that defines the table contents */
    public static class LogEntry implements BaseColumns {
        public static final String TABLE_NAME = "log";
        public static final String COLUMN_NAME_TIMESTAMP = "timestamp";
        public static final String COLUMN_NAME_MAC = "mac";
        public static final String COLUMN_NAME_HOST = "host";
        public static final String COLUMN_NAME_REQUEST_LINE = "request_line";
        public static final String COLUMN_NAME_HEADERS = "headers";
    }

    public static final String SQL_CREATE_ENTRIES =
            "CREATE TABLE " + LogEntry.TABLE_NAME + " (" +
                    LogEntry._ID + " INTEGER PRIMARY KEY," +
                    LogEntry.COLUMN_NAME_TIMESTAMP + " TEXT," +
                    LogEntry.COLUMN_NAME_MAC + " TEXT," +
                    LogEntry.COLUMN_NAME_HOST + " TEXT," +
                    LogEntry.COLUMN_NAME_REQUEST_LINE + " TEXT," +
                    LogEntry.COLUMN_NAME_HEADERS + " TEXT)";

    public static final String SQL_DELETE_ENTRIES =
            "DROP TABLE IF EXISTS " + LogEntry.TABLE_NAME;

    public static final String SQL_CLEAR =
            "DELETE FROM " + LogEntry.TABLE_NAME;

}
