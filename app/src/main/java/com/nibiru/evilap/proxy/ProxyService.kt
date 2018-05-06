package com.nibiru.evilap.proxy

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.util.Log
import com.nibiru.evilap.EvilApService
import java.net.ServerSocket
import javax.net.ssl.SSLServerSocket

class ProxyService : Service(){
    /**************************************CLASS FIELDS********************************************/
    protected val TAG = javaClass.simpleName
    private lateinit var  mSocketHTTP: ServerSocket
    private lateinit var mSocketHTTPS: SSLServerSocket
    // This service is only bound from inside the same process and never uses IPC.
    internal inner class LocalBinder : Binder() {
        val service = this@ProxyService
    }
    private val mBinder = LocalBinder()
    /**************************************CLASS METHODS*******************************************/
    override fun onBind(intent: Intent?): IBinder = mBinder

    override fun onCreate() {
        // Start up the thread running the servers.  Note that we create a separate thread because
        // the services normally runs in the process's main thread, which we don't want to block.
        try {
            mSocketHTTP = ServerSocket()
            //mSocketHTTPS = getSSLSocket(resources.openRawResource(R.raw.evil_ap))
        } catch (e: Exception) {
            e.printStackTrace()
        }

        //start the HTTP proxy socket thread
        val proxyHTTP = Thread(ProxyHTTPMainLoop(mSocketHTTP))
        proxyHTTP.start()
        //start the HTTPS proxy socket thread
        //val proxyHTTPS = Thread(ProxyHTTPSMainLoop(mSocketHTTPS))
        //proxyHTTPS.start()
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        Log.d(TAG, "got intent = ${intent.action}")
        when(intent.action){
            EvilApService.service.ACTION_STOP_SERVICE.action -> exit()
        }
        setupEventBus()
        // If this service really do get killed, there is no point restarting it automatically
        return Service.START_NOT_STICKY
    }

    private fun setupEventBus(){

    }

    private fun exit(){
        stopSelf()
    }

    /*
    @Throws(Exception::class)
    private fun getSSLSocket(keyStore: InputStream): SSLServerSocket {

        val ks = KeyStore.getInstance("BKS") //Bouncy Castle Key Store
        ks.load(keyStore, ksPass) //authenticate with keystore
        val kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm())
        kmf.init(ks, ctPass) //authenticate with certificate
        val sc = SSLContext.getInstance("TLS")
        sc.init(kmf.getKeyManagers(), null, null)
        val ssf = sc.getServerSocketFactory()
        return ssf.createServerSocket() as SSLServerSocket
    }*/

}