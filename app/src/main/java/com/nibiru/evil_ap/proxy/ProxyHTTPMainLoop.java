package com.nibiru.evil_ap.proxy;

import android.util.Log;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.SocketException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Created by Nibiru on 2016-10-14.
 */

class ProxyHTTPMainLoop implements Runnable{
    /**************************************CLASS FIELDS********************************************/
    private final String TAG = getClass().getSimpleName();
    private ServerSocket serverSocket;
    private static final int SERVERPORT = 1337;
    /**************************************CLASS METHODS*******************************************/
    ProxyHTTPMainLoop(ServerSocket socket) {
        this.serverSocket = socket;
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
            serverSocket.setReuseAddress(true);
            serverSocket.bind(new InetSocketAddress(SERVERPORT));
            while (true) {
                    executor.execute(new ThreadProxy(serverSocket.accept()));
                    Log.d(TAG, "Accepted HTTP client");
            }
        } catch (IOException e) {
            //SocketException means ProxyServer closed socket and we should quit
            if (!(e instanceof SocketException)) {
                Log.e(TAG, "Error!");
                e.printStackTrace();
            }
        } finally {
            Log.e(TAG, "Stopping!");
            if (serverSocket != null) try {
                serverSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
