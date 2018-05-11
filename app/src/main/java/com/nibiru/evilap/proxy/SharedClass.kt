package com.nibiru.evilap.proxy

import android.content.SharedPreferences
import okhttp3.OkHttpClient
import java.io.BufferedInputStream
import java.io.InputStream
import java.io.InputStreamReader
import java.util.concurrent.TimeUnit

enum class SharedClass {
    INSTANCE;

    public var indexFile = ByteArray(2048)
    public var notFoundFile =  ByteArray(2048)
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

    public fun init(idx: InputStream, notFound: InputStream){
        idx.use { stream -> stream.read(indexFile) }
        notFound.use { stream -> stream.read(notFoundFile) }
    }
}