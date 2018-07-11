package com.nibiru.evilap

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.Network
import android.util.Log
import com.nibiru.evilap.proxy.InterceptorRequest
import com.nibiru.evilap.proxy.InterceptorResponse
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit
import android.support.v4.net.ConnectivityManagerCompat.isActiveNetworkMetered



// https://stackoverflow.com/questions/708012/how-to-declare-global-variables-in-android
// https://stackoverflow.com/questions/9445661/how-to-get-the-context-from-anywhere
class EvilApApp : Application() {
    /**************************************CLASS FIELDS********************************************/
    private val TAG = javaClass.simpleName
    companion object {
        lateinit var instance: EvilApApp
    }
    var indexFile = ByteArray(2048)
    var notFoundFile = ByteArray(2048)
    private lateinit var _connMan: ConnectivityManager
    var wifiConnected: Boolean = false
        get() {
            return checkWiFiConnectivity()
        }
    var internetConnected: Boolean = false
        get() {
            return checkInternetConnectivity()
        }
    private var _httpClient: OkHttpClient? = null
    val httpClient: OkHttpClient
        get() {
            if(_httpClient==null){
                //make client not follow redirects!
                _httpClient = OkHttpClient().newBuilder().followRedirects(false)
                        .connectTimeout(10, TimeUnit.SECONDS)
                        .writeTimeout(10, TimeUnit.SECONDS)
                        .readTimeout(30, TimeUnit.SECONDS)
                        .addInterceptor(InterceptorRequest())
                        .addInterceptor(InterceptorResponse())
                        .followSslRedirects(false).build()
            }
            return _httpClient ?: throw AssertionError("Set to null by another thread")
        }
    //lateinit var config: SharedPreferences
    /**************************************CLASS METHODS*******************************************/
    // future update to https://stackoverflow.com/questions/48080336/how-to-handle-network-change-between-wifi-and-mobile-data?
    override fun onCreate() {
        instance = this
        if (!::_connMan.isInitialized)
            _connMan = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val idx = resources.openRawResource(R.raw.index)
        val notFound = resources.openRawResource(R.raw.not_found)
        idx.use { stream -> stream.read(indexFile) }
        notFound.use { stream -> stream.read(notFoundFile) }
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