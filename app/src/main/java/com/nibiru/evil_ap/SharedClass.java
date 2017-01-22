package com.nibiru.evil_ap;

import android.content.Context;
import android.content.SharedPreferences;

import com.nibiru.evil_ap.log.Client;
import com.nibiru.evil_ap.log.DatabaseManager;
import com.nibiru.evil_ap.log.LogDbHelper;
import com.nibiru.evil_ap.log.LogEntry;
import com.nibiru.evil_ap.proxy.InterceptorRequest;
import com.nibiru.evil_ap.proxy.InterceptorResponse;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;

/**
 * Created by Nibiru on 2016-12-17.
 */

public class SharedClass {
    /**************************************CLASS FIELDS********************************************/
    protected final String TAG = getClass().getSimpleName();
    private volatile byte[] imgData;
    private volatile List<String> payloads;
    private volatile DatabaseManager mDbManager;
    private IMVP.ModelOps mModel;
    private OkHttpClient okhttp;
    /**************************************CLASS METHODS*******************************************/
    SharedClass(InputStream is, Context ctx, IMVP.ModelOps model, SharedPreferences config){
        //make client not follow redirects!
        okhttp = new OkHttpClient().newBuilder().followRedirects(false)
                .connectTimeout(10, TimeUnit.SECONDS)
                .writeTimeout(10, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .addInterceptor(new InterceptorRequest())
                .addInterceptor(new InterceptorResponse(this, config))
                .followSslRedirects(false).build();
        //load default payload
        this.payloads = new ArrayList<>();
        this.payloads.add("<script type=\"text/javascript\">alert(\"Hi from Evil-AP!\");</script>");
        try {
            loadStream(is);
            DatabaseManager.initializeInstance(new LogDbHelper(ctx));
            mDbManager = DatabaseManager.getInstance();
            mDbManager.openDatabase();
            mDbManager.cleanDatabase();
            mModel = model;
            //DatabaseManager.getInstance().closeDatabase();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //no need for synchronized since we only use one database connections
    //and log entries will be added in a queue manner
    public void addRequest(Client c, String host, String reqLine, String headers){
        mDbManager.addRequest(c, host, reqLine, headers);
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

    synchronized void loadImage(String path){
        try {
            loadStream(new FileInputStream(new File(path)));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    synchronized void setPayloads(List<String> p){
        payloads = p;
    }

    public OkHttpClient getHttpClient(){
        return okhttp;
    }
    List<LogEntry> getClientLog(Client c){
        return mDbManager.getClientLog(c);
    }
    public synchronized Client getClientByIp(String ip){
        return mModel.getClientByIp(ip);
    }
    public synchronized List<String> getPayloads(){
        return payloads;
    }
    public synchronized byte[] getImgData(){
        return imgData;
    }
    public synchronized int getImgDataLength(){
        return imgData.length;
    }
}
