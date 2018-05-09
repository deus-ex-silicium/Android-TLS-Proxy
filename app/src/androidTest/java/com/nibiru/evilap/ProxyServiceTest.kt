package com.nibiru.evilap

import android.content.Intent
import android.support.test.InstrumentationRegistry
import android.support.test.rule.ServiceTestRule
import android.support.test.runner.AndroidJUnit4
import android.util.Log
import com.nibiru.evilap.proxy.ProxyService
import okhttp3.OkHttpClient
import okhttp3.Request
import org.hamcrest.CoreMatchers.containsString
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Proxy
import java.util.concurrent.TimeUnit


@RunWith(AndroidJUnit4::class)
class ProxyServiceTest {
    @Rule @JvmField
    val mServiceRule = ServiceTestRule()
    private lateinit var client: OkHttpClient

    @Before
    fun init(){
        mServiceRule.startService(Intent(InstrumentationRegistry.getTargetContext(), ProxyService::class.java))
        client = OkHttpClient.Builder()
                .connectTimeout(0, TimeUnit.SECONDS)
                .writeTimeout(0, TimeUnit.SECONDS)
                .readTimeout(0, TimeUnit.SECONDS)
                .proxy(Proxy(Proxy.Type.HTTP, InetSocketAddress(InetAddress.getByName("127.0.0.1"), 1337)))
                .build()
    }

    @Test
    fun simpleGET() {
        val request = Request.Builder()
                .url("http://example.com")
                //.url("http://www.icanhazip.com")
                //.url("https://publicobject.com/helloworld.txt")
                .build()
       client.newCall(request).execute().use { res ->
            if (!res.isSuccessful) throw IOException("Unexpected code $res")
            Log.d("[TEST]", res.headers().toString())
            val b = res.body()?.string()
            Assert.assertThat(b, containsString("<title>Example Domain</title>"))
            res.close()
        }
    }
}
