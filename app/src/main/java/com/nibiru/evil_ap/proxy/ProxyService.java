package com.nibiru.evil_ap.proxy;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Binder;
import android.os.IBinder;

import com.nibiru.evil_ap.R;
import com.nibiru.evil_ap.SharedClass;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Created by Nibiru on 2016-10-14.
 */

public class ProxyService extends Service{
    /**************************************CLASS FIELDS********************************************/
    protected final String TAG = getClass().getSimpleName();
    public volatile boolean work;
    public SharedClass mSharedObj;
    // Configuration settings
    public SharedPreferences config;
    /**************************************CLASS METHODS*******************************************/
    /**
     * Class for clients to access.  Because we know this service always
     * runs in the same process as its clients, we don't need to deal with
     * IPC.
     */
    public class LocalBinder extends Binder implements IProxyService{
        @Override
        public void setSharedObj(SharedClass sharedObj) {
            mSharedObj = sharedObj;
        }
    }
    // This is the object that receives interactions from clients.  See
    // RemoteService for a more complete example.
    private final IBinder mBinder = new LocalBinder();

    /*********************************************************************************************/
    @Override
    public void onCreate() {
        // Start up the thread running the service.  Note that we create a separate thread because
        // the service normally runs in the process's main thread, which we don't want to block.
        work = true;
        config = getSharedPreferences("Config", 0);
        //start the HTTP proxy socket thread
        Thread proxyHTTP = new Thread(new ProxyHTTPMainLoop(this));
        proxyHTTP.start();
        //start the HTTPS proxy socket thread
        Thread proxyHTTPS = new Thread(new ProxyHTTPSMainLoop(
                getResources().openRawResource(R.raw.evil_ap), this));
        proxyHTTPS.start();
    }

    @Override
    public void onDestroy(){
        work = false;
    }
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    public interface IProxyService {
        void setSharedObj(SharedClass sharedObj);
    }

    private void testImgStream() {
        InputStream inFromServer = getResources().openRawResource(R.raw.pixel_skull);
        byte[] buffer = new byte[0];
        try {
            File targetFile = new File(this.getApplicationContext().getFilesDir(), "test.tmp");
            OutputStream outStream = new FileOutputStream(targetFile);

            int bytes_read;
            byte[] reply = new byte[4096];
            try {
                while ((bytes_read = inFromServer.read(reply)) != -1) {
                    outStream.write(reply, 0, bytes_read);
                    outStream.flush();
                    //TODO CREATE YOUR LOGIC HERE
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

}
