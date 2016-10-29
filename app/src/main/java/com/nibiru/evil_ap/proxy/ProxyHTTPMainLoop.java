package com.nibiru.evil_ap.proxy;

import android.util.Log;

import java.io.IOException;
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
    private Boolean work = true;
    /*********************************************************************************************/
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
            while (work) {
                executor.execute(new ClientRevEcho(serverSocket.accept()));
                Log.d(TAG, "Accepted HTTP client");
            }
        } catch (IOException e) {
            Log.d(TAG, "Error!");
            e.printStackTrace();
        }
    }
}
