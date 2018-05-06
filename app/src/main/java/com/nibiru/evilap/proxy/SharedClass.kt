package com.nibiru.evilap.proxy

import android.content.SharedPreferences
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

enum class SharedClass {
    INSTANCE;

    public lateinit var config: SharedPreferences
    private var _httpClient: OkHttpClient? = null
    public val httpClient: OkHttpClient
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
}