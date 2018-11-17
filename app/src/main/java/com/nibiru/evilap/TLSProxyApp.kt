package com.nibiru.evilap

import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import android.util.Log
import com.nibiru.evilap.crypto.CaManager
import com.nibiru.evilap.crypto.EvilKeyManager
import com.nibiru.evilap.proxy.InterceptorRequest
import com.nibiru.evilap.proxy.InterceptorResponse
import okhttp3.OkHttpClient
import java.security.KeyStore
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import javax.net.ssl.*


// https://stackoverflow.com/questions/708012/how-to-declare-global-variables-in-android
// https://stackoverflow.com/questions/9445661/how-to-get-the-context-from-anywhere
class TLSProxyApp : Application() {
    /**************************************CLASS FIELDS********************************************/
    private val TAG = javaClass.simpleName
    val PORT_CAPTIVE_PORTAL = 8000
    val PORT_PROXY_HTTP = 8080
    val PORT_PROXY_HTTPS = 8443
    companion object {
        lateinit var instance: TLSProxyApp
    }
    lateinit var ca: CaManager
    lateinit var ekm: EvilKeyManager
    lateinit var trustManager: TrustManager
    lateinit var sslCtx: SSLContext
    lateinit var sf: SSLSocketFactory
    lateinit var exec: ExecutorService
    var indexFile = ByteArray(2048)
    var notFoundFile = ByteArray(2048)
    private lateinit var _connMan: ConnectivityManager
    var wifiConnected: Boolean = false
        get() { return checkWiFiConnectivity() }
    var internetConnected: Boolean = false
        get() { return checkInternetConnectivity() }
    private var _httpClient: OkHttpClient? = null
    val httpClient: OkHttpClient
        get() {
            if(_httpClient==null){
                //make client not follow redirects!
                _httpClient = OkHttpClient().newBuilder().followRedirects(false)
                        .sslSocketFactory(sf, trustManager as X509TrustManager) // trust fake CA
                        .connectTimeout(10, TimeUnit.SECONDS)
                        .writeTimeout(10, TimeUnit.SECONDS)
                        .readTimeout(30, TimeUnit.SECONDS)
                        .addInterceptor(InterceptorRequest())
                        .addInterceptor(InterceptorResponse())
                        .followSslRedirects(false).build()
            }
            return _httpClient ?: throw AssertionError("Set to null by another thread")
        }

    /**************************************CLASS METHODS*******************************************/
    // future update to https://stackoverflow.com/questions/48080336/how-to-handle-network-change-between-wifi-and-mobile-data?
    override fun onCreate() {
        instance = this
        // Initialize fields
        if (!::_connMan.isInitialized)
            _connMan = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        ca = CaManager(resources.openRawResource(R.raw.attacker), "password")
        ekm = EvilKeyManager(ca)
        exec = Executors.newSingleThreadExecutor()
        // Read captive portal HTML files
        val idx = resources.openRawResource(R.raw.index)
        val notFound = resources.openRawResource(R.raw.not_found)
        idx.use { stream -> stream.read(indexFile) }
        notFound.use { stream -> stream.read(notFoundFile) }

        //initialize SSLContext
        if (!::sslCtx.isInitialized) {
            sslCtx = SSLContext.getInstance("TLSv1.2")
            //ca.generateAndSignCert("example.com")

            // trust our CA
            val keyStore = KeyStore.getInstance(KeyStore.getDefaultType())
            keyStore.load(null, null)
            keyStore.setCertificateEntry("i-trust-this-ca", ca.root)
            val trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
            trustManagerFactory.init(keyStore)
            trustManager = trustManagerFactory.trustManagers[0]

            // key manager[], trust manager[], SecureRandom generator
            // trust fake CA
            sslCtx.init(arrayOf(ekm), trustManagerFactory.trustManagers, null)
            // dont trust fake CA
            //sslCtx.init(arrayOf(ekm), null, null)
            sf = sslCtx.socketFactory
        }

        super.onCreate()
    }

    private fun checkWiFiConnectivity(): Boolean {
        val netInfo = _connMan.activeNetworkInfo
        return if (netInfo == null || netInfo.type != ConnectivityManager.TYPE_WIFI){
            Log.v(TAG, "Not connected to WiFi!")
            false
        }
        else {
            Log.v(TAG, "Connected to WiFi!")
            true
        }
    }

    private fun checkInternetConnectivity(): Boolean {
        val netInfo = _connMan.activeNetworkInfo
        return if (netInfo == null || !netInfo.isConnected){
            Log.v(TAG, "No Internet connectivity!")
            false
        }
        else{
            Log.v(TAG, "Have Internet connectivity!")
            true
        }
    }


}