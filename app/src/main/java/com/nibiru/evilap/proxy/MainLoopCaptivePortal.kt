package com.nibiru.evilap.proxy

import android.util.Log
import com.nibiru.evilap.EvilApApp
import java.io.IOException
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.SocketException
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

//Runnable class that uses a thread pool to accept and handle client connection
internal class MainLoopCaptivePortal(private val serverSocket: ServerSocket) : Runnable {
    /**************************************CLASS FIELDS********************************************/
    private val TAG = javaClass.simpleName
    private val SERVERPORT = EvilApApp.instance.PORT_CAPTIVE_PORTAL
    /**************************************CLASS METHODS*******************************************/
    override fun run() {
        //http://codetheory.in/android-java-executor-framework/
        //int NUMBER_OF_CORES = Runtime.getRuntime().availableProcessors();
        val executor = ThreadPoolExecutor(
                5,
                5,
                60L,
                TimeUnit.SECONDS,
                LinkedBlockingQueue<Runnable>()
        )

        try {
            // listen for incoming clients
            serverSocket.reuseAddress = true
            serverSocket.bind(InetSocketAddress(SERVERPORT))
            Log.e(TAG, "Listening on port: $SERVERPORT")

            while (true) {
                executor.execute(ThreadCaptivePortal(serverSocket.accept()))
                Log.d(TAG, "Accepted HTTP connection on port $SERVERPORT")
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