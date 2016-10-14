package com.nibiru.evil_ap.Proxy;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;

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
        Thread proxy = new Thread(new ProxyMainLoop());
        proxy.start();
    }


    @Override
    public IBinder onBind(Intent intent) {
        return null; // We don't provide binding, so return null
    }
}
