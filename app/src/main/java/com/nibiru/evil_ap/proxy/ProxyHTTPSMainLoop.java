package com.nibiru.evil_ap.proxy;

import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
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
    protected final String TAG = getClass().getSimpleName();
    private SSLServerSocket serverSocket;
    private static final int SERVERPORT = 1338;
    private String keyStorePath;
    private InputStream keyStore;
    ProxyService ps;
    /*********************************************************************************************/
    public ProxyHTTPSMainLoop(InputStream file, ProxyService ps){
        keyStore = file;
        this.ps = ps;
    }
    //http://www.bouncycastle.org/wiki/display/JA1/Frequently+Asked+Questions
    @Override
    public void run() {
        // don't really care if this ends up in repo...
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
            while (ps.work) {
                Socket client = serverSocket.accept();

                if (ps.mPresenter != null && client != null &&
                        client.getInputStream().available()!=0) {
                    executor.execute(new ThreadProxy(client, ps.config,
                            ps.mPresenter.getSharedObj()));
                    Log.d(TAG, "Accepted HTTPS client");
                }
            }
        } catch (Exception e ) {
            Log.d(TAG, "Error!");
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
