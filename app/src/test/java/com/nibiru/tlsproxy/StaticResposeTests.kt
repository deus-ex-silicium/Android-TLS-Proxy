package com.nibiru.tlsproxy

import android.util.Log
import com.nibiru.tlsproxy.crypto.CaManager
import com.nibiru.tlsproxy.crypto.EvilKeyManager
import com.nibiru.tlsproxy.proxy.ThreadNioHTTP
import com.nibiru.tlsproxy.proxy.ThreadNioHTTPS
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
import java.util.concurrent.*
import javax.net.ssl.*


/**
 * Quickly MITM connection by using below command (needed for this test)
 * echo "127.0.0.1  example.com" | sudo tee -a /etc/hosts
 */

class StaticResposeTests {
    private lateinit var client: OkHttpClient
    private lateinit var ss: ServerSocket
    private lateinit var ekm: EvilKeyManager
    private lateinit var exec: ExecutorService

    @Before
    fun init(){
        // Create a new CA and make okhttp trust it (^_^)
        // https://jebware.com/blog/?p=340
        val ca = CaManager()
        ekm = EvilKeyManager(ca)
        val sc = SSLContext.getInstance("TLS")
        sc.init(arrayOf(ekm), null, null)
        exec = Executors.newSingleThreadExecutor()

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
            Assert.fail()
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
    fun HTTPS() {
        // Start mocked proxy server
        try {
            //ss = getEvilSSLSocket(ekm)
            //ss = ServerSocket()
            //val proxy = Thread(MainLoopProxy(ss, 8443, sc, ekm))
            //proxy.start()
            val proxy = Thread(ThreadNioHTTPS("0.0.0.0", 8443, ekm, exec))
            proxy.start()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        Log.d("Testing", "Making HTTPS request to example.com:8443")
        val request = Request.Builder()
                .url("https://example.com:8443") // HTTPS !
                .build()
        try {
            client.newCall(request).execute().use { res ->
                Assert.assertTrue(res.isSuccessful)
                val b = res.body()?.string()
                Assert.assertNotNull(b)
                var properRes = "=== HTTPS PROXY ===\n"
                properRes += "Connection was accepted...\n"
                properRes += "TLS Handshake was parsed for SNI...\n"
                properRes += "TLS Handshake was completed...\n"
                properRes += "X509 Certificate was generated and signed...\n"
                properRes += "And static response was served!\n"
                Assert.assertThat(b, CoreMatchers.containsString(properRes))
                res.close()
            }
        } catch (e: Exception){
            e.printStackTrace()
            Assert.fail()
        }
    }

    @Test
    fun HTTP() {
        // Start mocked proxy server
        try {
            val mProxyHTTP = ThreadNioHTTP("0.0.0.0", 8080)
            Thread(mProxyHTTP).start()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        Log.d("Testing", "Making HTTP request to example.com:8080")
        val request = Request.Builder()
                .url("http://example.com:8080") // HTTP !
                .build()
        try {
            client.newCall(request).execute().use { res ->
                Assert.assertTrue(res.isSuccessful)
                val b = res.body()?.string()
                Assert.assertNotNull(b)
                var properRes = "=== HTTP PROXY ===\n"
                properRes += "Connection was accepted...\n"
                properRes += "And static response was served!\n"
                Assert.assertThat(b, CoreMatchers.containsString(properRes))
                res.close()
            }
        } catch (e: Exception){
            e.printStackTrace()
            Assert.fail()
        }
    }

    @Throws(Exception::class)
    private fun getEvilSSLSocket(ekm: EvilKeyManager): ServerSocket {
        val sc = SSLContext.getInstance("TLS")
        // key manager[], trust manager[], SecureRandom generator
        sc.init(arrayOf(ekm), null, null)
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
                    Log.d(TAG, "Accepted HTTPS connection")
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
                outClient.write("HTTPS/1.1 200 OK\r\n".toByteArray())
                outClient.write("Content-Length: 18\r\n".toByteArray())
                outClient.write(byteArrayOf(0x0d, 0x0a)) //CRLF
            }

        }
    }

}
