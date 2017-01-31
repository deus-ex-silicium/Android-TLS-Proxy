package com.nibiru.evil_ap.proxy;

import android.util.Log;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLServerSocket;

/**
 * Created by Nibiru on 2016-10-29.
 */

class ProxyHTTPSMainLoop implements Runnable {
    private final String TAG = getClass().getSimpleName();
    private SSLServerSocket serverSocket;
    private static final int SERVERPORT = 1338;
    /*********************************************************************************************/
    ProxyHTTPSMainLoop(SSLServerSocket socket){
        this.serverSocket = socket;
    }
    /**
     * Runnable class that uses a thread pool to accept and handle client connection
     * This class uses a self-signed certificate during SSL handshake
     */
    //http://www.herongyang.com/JDK/SSL-Socket-Server-Example-SslReverseEchoer.html
    //http://www.bouncycastle.org/wiki/display/JA1/Frequently+Asked+Questions
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
                Log.d(TAG, "Accepted HTTPS client");
            }
        } catch (IOException e ) {
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
