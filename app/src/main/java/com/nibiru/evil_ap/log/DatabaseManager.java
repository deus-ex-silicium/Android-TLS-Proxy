package com.nibiru.evil_ap.log;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by Nibiru on 2016-12-21.
 */

public class DatabaseManager {
    /**************************************CLASS FIELDS********************************************/
    protected final String TAG = getClass().getSimpleName();
    private AtomicInteger  mOpenCounter = new AtomicInteger();
    private static DatabaseManager instance;
    private static LogDbHelper mDatabaseHelper;
    private SQLiteDatabase mDatabase;
    private ThreadPoolExecutor executor;
    /**************************************CLASS METHODS*******************************************/

    // for client log
    // select timestamp, host from log where mac="dc:85:de:8d:56:b5"
    // for request details
    // select count(host) from log where mac="dc:85:de:8d:56:b5" and host="joemonster.org"

    // To prevent someone from accidentally instantiating the DatabaseManager class,
    // make the constructor private.
    private DatabaseManager(){
        executor = new ThreadPoolExecutor(
                140,
                140,
                60L,
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<Runnable>()
        );
    }

    public static synchronized void initializeInstance(LogDbHelper helper) {
        if (instance == null) {
            instance = new DatabaseManager();
            mDatabaseHelper = helper;
        }
    }

    public static synchronized DatabaseManager getInstance() {
        if (instance == null) {
            throw new IllegalStateException(DatabaseManager.class.getSimpleName() +
                    " is not initialized, call initialize(..) method first.");
        }
        return instance;
    }

    public synchronized void cleanDatabase(){
        mDatabaseHelper.onClear(mDatabase);
    }

    public synchronized void openDatabase() {
        if(mOpenCounter.incrementAndGet() == 1) {
            // Opening new database
            mDatabase = mDatabaseHelper.getWritableDatabase();
        }
    }

    public synchronized void closeDatabase() {
        if(mOpenCounter.decrementAndGet() == 0) {
            // Closing database
            mDatabase.close();
        }
    }

    public void addRequest(Client c, String host, String reqLine, String headers){
        executor.execute(new RunnableAddDbEntry(c, host, reqLine, headers, mDatabase));
    }

    public List<LogEntry> getClientLog(Client c){
        // Define a projection that specifies which columns from the database we want to get
        String[] projection = {
                LogDbContract.LogEntry._ID,
                LogDbContract.LogEntry.COLUMN_NAME_TIMESTAMP,
                LogDbContract.LogEntry.COLUMN_NAME_HOST,
                LogDbContract.LogEntry.COLUMN_NAME_REQUEST_LINE,
                LogDbContract.LogEntry.COLUMN_NAME_HEADERS
        };
        // Filter results WHERE "MAC" = client's mac
        String selection = LogDbContract.LogEntry.COLUMN_NAME_MAC + " = ?";
        String[] selectionArgs = { c.getMac() };
        // Sort resulting Cursor by date and time
        String sortOrder = "datetime("+LogDbContract.LogEntry.COLUMN_NAME_TIMESTAMP+") DESC";

        Cursor cursor = mDatabase.query(
                LogDbContract.LogEntry.TABLE_NAME,        // The table to query
                projection,                               // The columns to return
                selection,                                // The columns for the WHERE clause
                selectionArgs,                            // The values for the WHERE clause
                null,                                     // don't group the rows
                null,                                     // don't filter by row groups
                sortOrder                                 // The sort order
        );

        //read and return results
        List<LogEntry> logEntries = new ArrayList<LogEntry>();
        while(cursor.moveToNext()) {
            long itemId = cursor.getLong(
                    cursor.getColumnIndexOrThrow(LogDbContract.LogEntry._ID));
            String itemTimestamp = cursor.getString(cursor.getColumnIndexOrThrow(LogDbContract
                    .LogEntry.COLUMN_NAME_TIMESTAMP));
            String itemHost = cursor.getString(cursor.getColumnIndexOrThrow(LogDbContract
                    .LogEntry.COLUMN_NAME_HOST));
            String itemDetails =
                    cursor.getString(cursor.getColumnIndexOrThrow
                            (LogDbContract.LogEntry.COLUMN_NAME_REQUEST_LINE)) + "\n" +
                    cursor.getString(cursor.getColumnIndexOrThrow
                            (LogDbContract.LogEntry.COLUMN_NAME_HEADERS));
            LogEntry entry = new LogEntry(itemId, itemTimestamp, itemHost, itemDetails);
            logEntries.add(entry);
        }
        cursor.close();
        return logEntries;
    }
}
