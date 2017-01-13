package com.nibiru.evil_ap.log;

import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;

/**
 * Created by Nibiru on 2017-01-13.
 */

public class RunnableAddDbEntry implements Runnable{
    /**************************************CLASS FIELDS********************************************/
    private final Client c;
    private final String host;
    private final String reqLine;
    private final String headers;
    private SQLiteDatabase mDatabase;
    /**************************************CLASS METHODS*******************************************/
    RunnableAddDbEntry(Client c, String host, String reqLine,
                       String headers, SQLiteDatabase mDatabase){
        this.c = c;
        this.host = host;
        this.reqLine = reqLine;
        this.headers = headers;
        this.mDatabase = mDatabase;
    }
    @Override
    public void run() {
        String date = (String) android.text.format.DateFormat.format
                ("yyyy-MM-dd kk:mm:ss", new java.util.Date());
        // Create a new map of values, where column names are the keys
        ContentValues values = new ContentValues();
        values.put(LogDbContract.LogEntry.COLUMN_NAME_MAC, c.getMac());
        values.put(LogDbContract.LogEntry.COLUMN_NAME_TIMESTAMP, date);
        values.put(LogDbContract.LogEntry.COLUMN_NAME_HOST, host);
        values.put(LogDbContract.LogEntry.COLUMN_NAME_REQUEST_LINE, reqLine);
        values.put(LogDbContract.LogEntry.COLUMN_NAME_HEADERS, headers);
        // Insert the new row, returning the primary key value of the new row
        mDatabase.insert(LogDbContract.LogEntry.TABLE_NAME, null, values);
    }
}
