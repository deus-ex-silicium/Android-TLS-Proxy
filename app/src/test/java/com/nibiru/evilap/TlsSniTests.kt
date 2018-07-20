package com.nibiru.evilap

import android.util.Log
import com.nibiru.evilap.pki.CaManager
import com.nibiru.evilap.pki.EvilKeyManager
import com.nibiru.evilap.pki.EvilSniMatcher
import com.nibiru.evilap.proxy.ThreadNioProxy
import okhttp3.OkHttpClient
import okhttp3.Request
import org.hamcrest.CoreMatchers
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import java.io.DataInputStream
import java.io.IOException
import java.io.OutputStream
import java.net.*
import java.security.KeyStore
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import javax.net.ssl.*


/**
 * Quickly MITM connection by using below command (needed for this test)
 * echo "127.0.0.1  example.com" >> sudo tee -a /etc/hosts
 */

class TlsSniTests {
    private lateinit var client: OkHttpClient
    private lateinit var ss: ServerSocket
    private lateinit var ca: CaManager

    @Before
    fun init(){
        // Create a new CA and make okhttp trust it (^_^)
        // https://jebware.com/blog/?p=340
        ca = CaManager()
        ca.saveKeyStore("evil-ap", "password")
        ca.saveRootCert("root.crt")
        val sslContext: SSLContext
        val trustManager: TrustManager
        try {
            val keyStore = KeyStore.getInstance(KeyStore.getDefaultType())
            keyStore.load(null, null)
            // check some failing scenarios
            //ca.generateAndSignCert("fail.com")
            //keyStore.setCertificateEntry("i-trust-this-ca", ca.getCertChain("fail.com")[0])
            keyStore.setCertificateEntry("i-trust-this-ca", ca.root)
            val trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
            trustManagerFactory.init(keyStore)
            sslContext = SSLContext.getInstance("TLS")
            trustManager = trustManagerFactory.trustManagers[0]
            sslContext.init(null, trustManagerFactory.trustManagers, null)
        } catch (e: Exception) {
            e.printStackTrace()
            return
        }
        client = OkHttpClient.Builder()
                .sslSocketFactory(sslContext.socketFactory, trustManager as X509TrustManager)
                .connectTimeout(0, TimeUnit.SECONDS)
                .writeTimeout(0, TimeUnit.SECONDS)
                .readTimeout(0, TimeUnit.SECONDS)
                .followRedirects(false)
                .build()
    }

    @Test
    fun oneTestToRuleThemAll() {
        // Start mocked proxy server
        try {
            //ss = getEvilSSLSocket()
            val ekm = EvilKeyManager(ca)
            val proxyHTTP = Thread(ThreadNioProxy("0.0.0.0", 1337, ekm))
            proxyHTTP.start()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        Log.d("Testing", "Making HTTPS request to example.com:1337")
        val request = Request.Builder()
                .url("https://example.com:1337") // HTTPS !
                .build()
        try {
            client.newCall(request).execute().use { res ->
                Assert.assertTrue(res.isSuccessful)
                val b = res.body()?.string()
                Assert.assertNotNull(b)
                Assert.assertThat(b, CoreMatchers.containsString("HTTPS Proxy Hello!"))
                res.close()
            }
        } catch (e: Exception){
            e.printStackTrace()
        }
    }

    @Throws(Exception::class)
    private fun getEvilSSLSocket(): ServerSocket {
        val sc = SSLContext.getInstance("TLS")
        // key manager[], trust manager[], SecureRandom generator
        sc.init(arrayOf(EvilKeyManager(ca)), null, null)
        val ssf = sc.serverSocketFactory
        val sock = ssf.createServerSocket() as SSLServerSocket
        sock.useClientMode = false
        //val params = sock.sslParameters
        //params.protocols = arrayOf("TLSv1.2")
        //params.sniMatchers = listOf(EvilSniMatcher())
        //sock.sslParameters = params
        return sock
    }

    internal class MainLoopProxyTest(private val serverSocket: ServerSocket) : Runnable {
        private val TAG = javaClass.simpleName
        private val SERVERPORT = 1337
        /**************************************CLASS METHODS*******************************************/
        override fun run() {
            val executor = ThreadPoolExecutor(140, 140, 60L, TimeUnit.SECONDS, LinkedBlockingQueue<Runnable>())
            try {
                // listen for incoming clients
                Log.d(TAG, "Listening on port: $SERVERPORT")
                serverSocket.reuseAddress = true
                serverSocket.bind(InetSocketAddress(SERVERPORT))
                while (true) {
                    executor.execute(ThreadHandleHTTPClientTest(serverSocket.accept()))
                    Log.d(TAG, "Accepted HTTP connection")
                }
            } catch (e: IOException) {
                if (e !is SocketException) {
                    Log.e(TAG, "Error!")
                    e.printStackTrace()
                }
            } finally {
                Log.e(TAG, "Stopping!")
                try {
                    serverSocket.close()
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
        }

        internal class ThreadHandleHTTPClientTest(private val sClient: Socket) : Runnable {
            private val TAG = javaClass.simpleName
            private var keepAlive = true

            override fun run() {
                var inData: DataInputStream?= null
                var outStream: OutputStream? = null
                try {
                    inData = DataInputStream(sClient.getInputStream())
                    outStream = sClient.getOutputStream()
                    while (keepAlive) {
                        readReq(inData)
                        sendResponseHeaders(outStream)
                        outStream.write("HTTPS Proxy Hello!".toByteArray())
                        outStream.flush()
                        Log.d(TAG, "[SEND]")
                    }
                } catch (e: IOException) {
                    if (e is SocketTimeoutException)
                        Log.e(TAG, "TIMEOUT!")
                    else
                        e.printStackTrace()
                } finally {
                    try { //clean up
                        Log.e(TAG, "Cleaning client resources")
                        if (outStream != null) outStream.close()
                        if (inData != null) inData.close()
                        sClient.close()
                    } catch (e: IOException) {
                        e.printStackTrace()
                    }
                }
            }

            private fun readReq(inD : DataInputStream) {
                val requestLine = inD.readLine() ?: return
                Log.d("$TAG[REQUEST LINE]", requestLine)
                var line: String
                loop@ while(true){
                    line = inD.readLine()
                    Log.d("$TAG[LINE]", line)
                    if(line == "") break@loop
                }
            }

            private fun sendResponseHeaders(outClient: OutputStream){
                outClient.write("HTTP/1.1 200 OK\r\n".toByteArray())
                outClient.write("Content-Length: 18\r\n".toByteArray())
                outClient.write(byteArrayOf(0x0d, 0x0a)) //CRLF
            }

        }
    }

}
