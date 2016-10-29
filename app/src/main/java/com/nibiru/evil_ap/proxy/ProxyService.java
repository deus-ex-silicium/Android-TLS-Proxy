package com.nibiru.evil_ap.proxy;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import com.nibiru.evil_ap.R;

/**
 * Created by Nibiru on 2016-10-14.
 */

public class ProxyService extends Service{
    final static String TAG = "proxyService";
    /*********************************************************************************************/
    @Override
    public void onCreate() {
        // Start up the thread running the service.  Note that we create a separate thread because
        // the service normally runs in the process's main thread, which we don't want to block.

        //start the HTTP proxy socket thread
        Thread proxyHTTP = new Thread(new ProxyHTTPMainLoop());
        proxyHTTP.start();
        //start the HTTPS proxy
        Thread proxyHTTPS = new Thread(new ProxyHTTPSMainLoop(
                getResources().openRawResource(R.raw.evil_ap)));
        proxyHTTPS.start();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null; // We don't provide binding, so return null
    }
}
