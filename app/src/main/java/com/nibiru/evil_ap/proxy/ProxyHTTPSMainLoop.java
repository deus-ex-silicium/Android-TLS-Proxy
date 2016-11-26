package com.nibiru.evil_ap.proxy;

import android.util.Log;

import java.io.InputStream;
import java.security.KeyStore;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;

/**
 * Created by Nibiru on 2016-10-29.
 */

class ProxyHTTPSMainLoop implements Runnable {
    final static String TAG = "ProxyHTTPSMainLoop";
    private SSLServerSocket serverSocket;
    private static final int SERVERPORT = 1338;
    private Boolean work = true;
    private String keyStorePath;
    private InputStream keyStore;
    /*********************************************************************************************/
    public ProxyHTTPSMainLoop(InputStream file){
        keyStore = file;
    }
    //http://www.bouncycastle.org/wiki/display/JA1/Frequently+Asked+Questions
    @Override
    public void run() {
        char ksPass[] = "KeyStorePass".toCharArray();
        char ctPass[] = "KeyStorePass".toCharArray();

        try {
            KeyStore ks = KeyStore.getInstance("BKS"); //Bouncy Castle Key Store
            ks.load(keyStore, ksPass); //authenticate with keystore
            KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            kmf.init(ks, ctPass); //authenticate with certificate
            SSLContext sc = SSLContext.getInstance("TLS");
            sc.init(kmf.getKeyManagers(), null, null);
            SSLServerSocketFactory ssf = sc.getServerSocketFactory();
            serverSocket = (SSLServerSocket) ssf.createServerSocket(SERVERPORT);
            //http://codetheory.in/android-java-executor-framework/
            //int NUMBER_OF_CORES = Runtime.getRuntime().availableProcessors();
            ThreadPoolExecutor executor = new ThreadPoolExecutor(
                    140,
                    140,
                    60L,
                    TimeUnit.SECONDS,
                    new LinkedBlockingQueue<Runnable>()
            );
            // listen for incoming clients
            Log.d(TAG, "Listening on port: " + SERVERPORT);
            while (work) {
                executor.execute(new RequestRevEcho(serverSocket.accept()));
                Log.d(TAG, "Accepted HTTPS client");
            }
        } catch (Exception e ) {
            Log.d(TAG, "Error!");
            e.printStackTrace();
        } finally {
            //TODO: clean shit up?
        }
    }
}
