package com.nibiru.evil_ap.log;

import android.database.sqlite.SQLiteDatabase;

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
    /**************************************CLASS METHODS*******************************************/
    //TODO: things to show when log is pressed
    // select distinct host from log where mac="dc:85:de:8d:56:b5"
    // for each host from that list get number of requests that were sent to that host
    // select count(host) from log where mac="dc:85:de:8d:56:b5" and host="joemonster.org"


    // To prevent someone from accidentally instantiating the DatabaseManager class,
    // make the constructor private.
    private DatabaseManager(){}

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

    public static synchronized void cleanDatabase(SQLiteDatabase db){
        mDatabaseHelper.onClear(db);
    }

    public synchronized SQLiteDatabase openDatabase() {
        if(mOpenCounter.incrementAndGet() == 1) {
            // Opening new database
            mDatabase = mDatabaseHelper.getWritableDatabase();
        }
        return mDatabase;
    }

    public synchronized void closeDatabase() {
        if(mOpenCounter.decrementAndGet() == 0) {
            // Closing database
            mDatabase.close();
        }
    }
}
