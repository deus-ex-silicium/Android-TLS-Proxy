package com.nibiru.evil_ap.proxy;

import android.util.Log;

import com.nibiru.evil_ap.SharedClass;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
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
                Socket client = serverSocket.accept();
                if (ps.mPresenter != null && client != null) {
                    executor.execute(new ThreadProxy(client, ps.config,
                            ps.mPresenter.getSharedObj()));
                    Log.d(TAG, "Accepted HTTP client");
                }
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
