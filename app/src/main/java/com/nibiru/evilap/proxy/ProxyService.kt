package com.nibiru.evilap.proxy

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.util.Log
import com.nibiru.evilap.EvilApService
import com.nibiru.evilap.RxEventBus
import io.reactivex.disposables.Disposable
import java.io.IOException
import java.net.ServerSocket

class ProxyService : Service(){
    /**************************************CLASS FIELDS********************************************/
    protected val TAG = javaClass.simpleName
    private lateinit var  mSocketHTTP: ServerSocket
    // This service is only bound from inside the same process and never uses IPC.
    internal inner class LocalBinder : Binder() {
        val service = this@ProxyService
    }
    private val mBinder = LocalBinder()
    private var mDispService: Disposable? = null
    /**************************************CLASS METHODS*******************************************/
    override fun onBind(intent: Intent?): IBinder = mBinder

    override fun onCreate() {
        Log.d(TAG, "onCreate()")
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
        setupEventBus()
        // If this service really do get killed, there is no point restarting it automatically
        return Service.START_NOT_STICKY
    }

    private fun setupEventBus(){
        if (mDispService != null && !mDispService!!.isDisposed) return
        mDispService = RxEventBus.INSTANCE.getBackEndObservable().subscribe({
            Log.d(TAG, "got event = $it")
            when (it) {
                is EvilApService.EventExit -> exit()
            }
        })
    }

    private fun exit(){
        try {
            Log.e(TAG,"Closing server socket!")
            mSocketHTTP.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
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