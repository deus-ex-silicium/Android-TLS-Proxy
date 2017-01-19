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
    /**************************************CLASS FIELDS********************************************/
    protected final String TAG = getClass().getSimpleName();
    private ServerSocket serverSocket;
    private static final int SERVERPORT = 1337;
    private ProxyService ps;
    /**************************************CLASS METHODS*******************************************/
    public ProxyHTTPMainLoop(ProxyService x){
        ps = x;
    }

    /**
     * Runnable class that uses a thread pool to accept and handle client connection
     */
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
                if (ps.mPresenter != null) {
                    executor.execute(new ThreadProxy(serverSocket.accept(),
                            ps.mPresenter.getSharedObj()));
                    Log.d(TAG, "Accepted HTTP client");
                }
            }
        } catch (IOException e) {
            Log.e(TAG, "Error!");
            e.printStackTrace();
        } finally {
            if (ps != null) ps = null;
            if (serverSocket != null) try {
                serverSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
