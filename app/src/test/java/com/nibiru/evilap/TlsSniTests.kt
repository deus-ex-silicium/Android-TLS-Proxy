package com.nibiru.evilap

import com.nibiru.evilap.pki.EvilKeyManager
import com.nibiru.evilap.proxy.MainLoopProxyHTTP
import okhttp3.OkHttpClient
import okhttp3.Request
import org.hamcrest.CoreMatchers
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Proxy
import java.net.ServerSocket
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext

/**
 * echo "127.0.0.1  example.com" >> sudo tee -a /etc/hosts
 *
 */
class TlsSniTests {
    private lateinit var client: OkHttpClient
    private lateinit var ss: ServerSocket

    @Before
    fun init(){
        client = OkHttpClient.Builder()
                .connectTimeout(0, TimeUnit.SECONDS)
                .writeTimeout(0, TimeUnit.SECONDS)
                .readTimeout(0, TimeUnit.SECONDS)
                .followRedirects(false)
                .build()
        try {
            ss = getEvilSSLSocket()
            //ss = ServerSocket()
            val proxyHTTP = Thread(MainLoopProxyHTTP(ss))
            proxyHTTP.start()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    @Test
    fun oneTest() {
        val request = Request.Builder()
                .url("https://example.com:1337")
                .build()
        client.newCall(request).execute().use { res ->
            Assert.assertTrue(res.isSuccessful)
            val b = res.body()?.string()
            Assert.assertNotNull(b)
            Assert.assertThat(b, CoreMatchers.containsString("<title>Example Domain</title>"))
            res.close()
        }
        Assert.assertTrue(true)
    }

    @Throws(Exception::class)
    private fun getEvilSSLSocket(): ServerSocket {
        val sc = SSLContext.getInstance("TLS")
        // key manager[], trust manager[], SecureRandom generator
        sc.init(arrayOf(EvilKeyManager()), null, null)
        val ssf = sc.serverSocketFactory
        return ssf.createServerSocket()
    }
}
