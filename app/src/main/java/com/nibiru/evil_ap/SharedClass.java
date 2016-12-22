package com.nibiru.evil_ap;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import com.nibiru.evil_ap.log.Client;
import com.nibiru.evil_ap.log.DatabaseManager;
import com.nibiru.evil_ap.log.LogDbContract;
import com.nibiru.evil_ap.log.LogDbHelper;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.DateFormat;
import java.util.Date;
import java.util.List;

/**
 * Created by Nibiru on 2016-12-17.
 */

public class SharedClass {
    /**************************************CLASS FIELDS********************************************/
    protected final String TAG = getClass().getSimpleName();
    private volatile byte[] imgData;
    private volatile List<String> payloads;
    private volatile SQLiteDatabase mDatabase;
    private IMVP.ModelOps mModel;
    /**************************************CLASS METHODS*******************************************/
    public SharedClass(InputStream is, Context ctx, IMVP.ModelOps model){
        try {
            loadStream(is);
            DatabaseManager.initializeInstance(new LogDbHelper(ctx));
            DatabaseManager manager = DatabaseManager.getInstance();
            mDatabase = manager.openDatabase();
            DatabaseManager.cleanDatabase(mDatabase);
            mModel = model;
            //TODO: close ?
            //DatabaseManager.getInstance().closeDatabase();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //no need for synchronized since we only use one database connections
    //and log entried will be added in a queue manner
    public void addRequest(Client c, String host, String reqLine){
        // Create a new map of values, where column names are the keys
        ContentValues values = new ContentValues();
        values.put(LogDbContract.LogEntry.COLUMN_NAME_MAC, c.getMac());
        values.put(LogDbContract.LogEntry.COLUMN_NAME_TIMESTAMP,
                DateFormat.getDateTimeInstance().format(new Date()));
        values.put(LogDbContract.LogEntry.COLUMN_NAME_HOST, host);
        values.put(LogDbContract.LogEntry.COLUMN_NAME_REQUEST_LINE, reqLine);
        values.put(LogDbContract.LogEntry.COLUMN_NAME_HEADERS, "test headers");
        // Insert the new row, returning the primary key value of the new row
        long newRowId = mDatabase.insert(LogDbContract.LogEntry.TABLE_NAME, null, values);
    }

    synchronized void loadImage(String path){
        File file = new File(path);
        try {
            InputStream imgStream = new FileInputStream(file);
            loadStream(imgStream);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private synchronized void loadStream(InputStream is) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        int nRead;
        byte[] data = new byte[16384];
        while ((nRead = is.read(data, 0, data.length)) != -1) {
            buffer.write(data, 0, nRead);
        }
        buffer.flush();
        imgData = buffer.toByteArray();
    }

    synchronized void setPayloads(List<String> p){
        payloads = p;
    }

    public Client getClientByIp(String ip){
        return mModel.getClientByIp(ip);
    }
    public List<String> getPayloads(){
        return payloads;
    }
    public byte[] getImgData(){
        return imgData;
    }
    public int getImgDataLength(){
        return imgData.length;
    }
}
