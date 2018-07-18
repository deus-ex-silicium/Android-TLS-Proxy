package com.nibiru.evilap.proxy

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.support.annotation.RequiresApi
import android.util.Log
import com.nibiru.evilap.EvilApApp
import com.nibiru.evilap.EvilApService
import com.nibiru.evilap.RxEventBus
import com.nibiru.evilap.pki.EvilKeyManager
import com.nibiru.evilap.pki.EvilSniMatcher
import io.reactivex.disposables.Disposable
import java.io.IOException
import java.net.ServerSocket
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLServerSocket

class ProxyService : Service(){
    /**************************************CLASS FIELDS********************************************/
    protected val TAG = javaClass.simpleName
    private lateinit var  mSocketHTTP: ServerSocket
    private lateinit var  mSocketHTTPS: ServerSocket
    private lateinit var  mSocketPortal: ServerSocket
    // This service is only bound from inside the same process and never uses IPC.
    internal inner class LocalBinder : Binder() {
        val service = this@ProxyService
    }
    private val mBinder = LocalBinder()
    private var mDispService: Disposable? = null
    /**************************************CLASS METHODS*******************************************/
    override fun onBind(intent: Intent?): IBinder = mBinder

    @RequiresApi(Build.VERSION_CODES.N)
    override fun onCreate() {
        Log.d(TAG, "onCreate()")
        // Spin up the threads running the servers.  Note that we create a separate thread because
        // the services normally runs in the process's main thread, which we don't want to block.
        try {
            mSocketHTTP = ServerSocket()
            mSocketHTTPS = getEvilSSLSocket()
            mSocketPortal = ServerSocket()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        //start the HTTP proxy socket thread
        //val proxyHTTP = Thread(MainLoopProxy(mSocketHTTP, EvilApApp.instance.PORT_PROXY_HTTP))
        //proxyHTTP.start()
        //start the HTTPS proxy socket thread
        val proxyHTTPS = Thread(MainLoopProxy(mSocketHTTPS, EvilApApp.instance.PORT_PROXY_HTTPS))
        proxyHTTPS.start()
        //start the captive portal thread
        //val portal = Thread(MainLoopCaptivePortal(mSocketPortal))
        //portal.start()
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        Log.v(TAG, "got intent = ${intent.action}")
        setupEventBus()
        // If this service really do get killed, there is no point restarting it automatically
        return Service.START_NOT_STICKY
    }

    private fun setupEventBus(){
        if (mDispService != null && !mDispService!!.isDisposed) return
        mDispService = RxEventBus.INSTANCE.getBackEndObservable().subscribe({
            Log.v(TAG, "got event = $it")
            when (it) {
                is EvilApService.EventExit -> exit()
            }
        })
    }

    private fun exit(){
        try {
            Log.e(TAG,"Closing server sockets!")
            mSocketHTTP.close()
            mSocketHTTPS.close()
            mSocketPortal.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
        stopSelf()
    }

    @RequiresApi(Build.VERSION_CODES.N)
    @Throws(Exception::class)
    private fun getEvilSSLSocket(): ServerSocket {
        val sc = SSLContext.getInstance("TLS")
        // key manager[], trust manager[], SecureRandom generator
        sc.init(arrayOf(EvilKeyManager(EvilApApp.instance.ca)), null, null)
        //sc.init(null, null, null)
        val ssf = sc.serverSocketFactory
        val sock = ssf.createServerSocket() as SSLServerSocket
        sock.useClientMode = false
        // Use only TLSv1.2 to get access to ExtendedSSLSession
        // https://docs.oracle.com/javase/8/docs/technotes/guides/security/jsse/JSSERefGuide.html#SSLSession
        val params = sock.sslParameters
        params.protocols = arrayOf("TLSv1.2")
        params.sniMatchers = listOf(EvilSniMatcher())
        sock.sslParameters = params
        sock.enabledProtocols = arrayOf("TLSv1.2")
        return sock
    }

}