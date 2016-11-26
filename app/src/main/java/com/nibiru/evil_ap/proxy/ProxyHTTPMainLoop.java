package com.nibiru.evil_ap.proxy;

import android.util.Log;

import com.nibiru.evil_ap.R;

import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Created by Nibiru on 2016-10-14.
 */

class ProxyHTTPMainLoop implements Runnable{
    final static String TAG = "ProxyHTTPMainLoop";
    private ServerSocket serverSocket;
    private static final int SERVERPORT = 1337;
    ProxyService ps;
    /*********************************************************************************************/
    public ProxyHTTPMainLoop(ProxyService x){
        ps = x;
    }
    @Override
    public void run() {
        //http://codetheory.in/android-java-executor-framework/
        //int NUMBER_OF_CORES = Runtime.getRuntime().availableProcessors();
        ThreadPoolExecutor executor = new ThreadPoolExecutor(
                140,
                140,
                60L,
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<Runnable>()
        );

        try {
            // listen for incoming clients
            Log.d(TAG, "Listening on port: " + SERVERPORT);
            serverSocket = new ServerSocket(SERVERPORT);
            while (ps.work) {
                if (ps.imgResource != -1) {
                    int copy = ps.imgResource;
                    executor.execute(new ThreadProxy(serverSocket.accept(), ps.getResources()
                            .openRawResource(copy)));
                }
                else
                    executor.execute(new ThreadProxy(serverSocket.accept(), null));
                Log.d(TAG, "Accepted HTTP client");
            }
        } catch (IOException e) {
            Log.e(TAG, "Error!");
            e.printStackTrace();
        } finally {
            if (serverSocket != null) try {
                serverSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

}
