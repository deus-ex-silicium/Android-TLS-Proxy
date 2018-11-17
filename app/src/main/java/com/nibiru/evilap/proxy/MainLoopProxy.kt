package com.nibiru.evilap.proxy

import android.util.Log
import com.nibiru.evilap.crypto.EvilKeyManager
import java.io.IOException
import java.net.*
import java.util.concurrent.Executors
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext

//Runnable class that uses a thread pool to accept and handle client connection
internal class MainLoopProxy(private val serverSocket: ServerSocket, private val port: Int,
                             private val sslCtx: SSLContext, private val ekm: EvilKeyManager) : Runnable {
    /**************************************CLASS FIELDS********************************************/
    private val TAG = javaClass.simpleName
    /**************************************CLASS METHODS*******************************************/
    override fun run() {
        //http://codetheory.in/android-java-executor-framework/
        //int NUMBER_OF_CORES = Runtime.getRuntime().availableProcessors();
        val executor = ThreadPoolExecutor(
                140,
                140,
                60L,
                TimeUnit.SECONDS,
                LinkedBlockingQueue<Runnable>()
        )
        try {
            // listen for incoming clients
            Log.e(TAG, "Listening on port: $port")
            serverSocket.reuseAddress = true
            serverSocket.bind(InetSocketAddress(port))
            val executorService = Executors.newSingleThreadExecutor()
            while (true) {
                val sslEngine = sslCtx.createSSLEngine()
                executor.execute(ThreadBlockingHTTPS(serverSocket.accept(), sslEngine, ekm, executorService))
                Log.d(TAG, "Accepted HTTPS connection")
            }
        } catch (e: IOException) {
            //SocketException means ProxyService closed socket and we should quit normally
            if (e !is SocketException) {
                Log.e(TAG, "Error!")
                e.printStackTrace()
            }

        } finally {
            Log.e(TAG, "Stopping!")
            try {
                serverSocket.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }
}