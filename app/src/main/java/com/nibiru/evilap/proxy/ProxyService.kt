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
import com.nibiru.evilap.crypto.EvilKeyManager
import io.reactivex.disposables.Disposable
import java.io.IOException
import java.net.ServerSocket
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLServerSocket

class ProxyService : Service(){
    /**************************************CLASS FIELDS********************************************/
    protected val TAG = javaClass.simpleName
    private lateinit var  mSocketProxy: ServerSocket
    private lateinit var  mSocketPortal: ServerSocket
    private lateinit var  mNioHTTPS: ThreadNioHTTPS
    private lateinit var  mProxyHTTP: ThreadNioHTTP
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
            mSocketProxy = ServerSocket()
            //mSocketProxy = getEvilSSLSocket(EvilApApp.instance.ekm)
            mSocketPortal = ServerSocket()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        //start the proxy socket thread
        val proxy = Thread(MainLoopProxy(mSocketProxy, EvilApApp.instance.PORT_PROXY_HTTPS,
                EvilApApp.instance.sslCtx, EvilApApp.instance.ekm))
        proxy.start()

        mProxyHTTP = ThreadNioHTTP("0.0.0.0", EvilApApp.instance.PORT_PROXY_HTTP)
        //Thread(mProxyHTTP).start()

        mNioHTTPS = ThreadNioHTTPS("0.0.0.0", EvilApApp.instance.PORT_PROXY_HTTPS,
                EvilApApp.instance.ekm, EvilApApp.instance.exec)
        //Thread(mNioHTTPS).start()

        //start the captive portal thread
        Thread(MainLoopCaptivePortal(mSocketPortal)).start()
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
            mSocketProxy.close()
            mProxyHTTP.exit()
            mNioHTTPS.stop()
            mSocketPortal.close()

        } catch (e: IOException) {
            e.printStackTrace()
        }
        stopSelf()
    }

    private fun getEvilSSLSocket(ekm: EvilKeyManager): ServerSocket {
        val sc = SSLContext.getInstance("TLS")
        // key manager[], trust manager[], SecureRandom generator
        sc.init(arrayOf(ekm), null, null)
        val ssf = sc.serverSocketFactory
        val sock = ssf.createServerSocket() as SSLServerSocket
        sock.useClientMode = false
        // Use only TLSv1.2 to get access to ExtendedSSLSession
        // https://docs.oracle.com/javase/8/docs/technotes/guides/security/jsse/JSSERefGuide.html#SSLSession
        //val params = sock.sslParameters
        //params.serverNames = listOf(SNIHostName("somename.com"))
        //params.protocols = arrayOf("TLSv1.2")
        //params.sniMatchers = listOf(EvilSniMatcher())
        //sock.sslParameters = params
        //sock.enabledProtocols = arrayOf("TLSv1.2")
        return sock
    }
}