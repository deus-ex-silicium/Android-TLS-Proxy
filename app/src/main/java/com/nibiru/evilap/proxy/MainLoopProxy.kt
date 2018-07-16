package com.nibiru.evilap.proxy

import android.util.Log
import java.io.IOException
import java.net.*
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

//Runnable class that uses a thread pool to accept and handle client connection
internal class MainLoopProxy(private val serverSocket: ServerSocket, private val port: Int) : Runnable {
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
            Log.d(TAG, "Listening on port: $port")
            serverSocket.reuseAddress = true
            serverSocket.bind(InetSocketAddress(port))
            while (true) {
                executor.execute(ThreadHandleProxyClient(serverSocket.accept()))
                Log.d(TAG, "Accepted HTTP connection")
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