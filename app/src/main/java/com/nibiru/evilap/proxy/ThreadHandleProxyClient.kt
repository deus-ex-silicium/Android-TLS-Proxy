package com.nibiru.evilap.proxy

import android.util.Log
import com.nibiru.evilap.EvilApApp
import com.nibiru.evilap.pki.SSLCapabilities
import com.nibiru.evilap.pki.SSLExplorer
import com.nibiru.evilap.pki.NioSslPeer
import okhttp3.*
import java.io.*
import java.net.Socket
import java.net.SocketTimeoutException
import java.nio.ByteBuffer
import java.nio.channels.SocketChannel
import javax.net.ssl.*



internal class ThreadHandleProxyClient(private var sClient: Socket) : Runnable, NioSslPeer() {
    /**************************************CLASS FIELDS********************************************/
    private val TAG = javaClass.simpleName
    private var keepAlive = true
    private val DEBUG = false
    /**************************************CLASS METHODS*******************************************/

    override fun run() {
        // try and read ClientHello if this is start of TLS handshake
        try {
            val engine = EvilApApp.instance.sslCtx.createSSLEngine()
            engine.useClientMode = false
            //val clientHello = parseSslClientHello(sClient, engine)
            // Create byte buffers to use for holding application and encoded data
            val session = engine.session
            val myAppData = ByteBuffer.allocate(session.applicationBufferSize)
            val myNetData = ByteBuffer.allocate(session.packetBufferSize)
            val peerAppData = ByteBuffer.allocate(session.applicationBufferSize)
            val peerNetData = ByteBuffer.allocate(session.packetBufferSize)

            // Do initial handshake
            doHandshake(sClient, engine, myNetData, peerNetData, session.applicationBufferSize)
            val handshake = engine.handshakeSession
        }
        catch (e: SSLHandshakeException){
            e.printStackTrace()
        }

        //things we will eventually close
        var inData: DataInputStream?= null
        var outStream: OutputStream? = null
        var res: Response? = null
        try {
            inData = DataInputStream(sClient.getInputStream())
            outStream = sClient.getOutputStream()
            //outStream = ByteArrayOutputStream()

            //HTTP KEEP ALIVE LOOP!
            while (keepAlive) {
                //get client request string as okhttp request
                val req = getOkhttpRequest(inData)
                if (req == null) {
                    Log.e(TAG, "Cannot read request, closing")
                    return
                }
                res = EvilApApp.instance.httpClient.newCall(req).execute()

                sendResponseHeaders(res, outStream)
                res.body()?.apply { outStream.write(this.bytes()) }
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
                if (res != null) res.body()?.close()
                if (outStream != null) outStream.close()
                if (inData != null) inData.close()
                sClient.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    // "In practice, most HTTP header field values use only a subset of the
    // US-ASCII charset [USASCII]. Newly defined header fields SHOULD limit
    // their field values to US-ASCII octets.  A recipient SHOULD treat other
    // octets in field content (obs-text) as opaque data."
    //  - https://tools.ietf.org/html/rfc7230
    // So using DataInputSteam.readLine() should work well since multibyte
    // character sets are not to be expected
    // see https://stackoverflow.com/questions/21754078/datainputstream-readline-deprecated
    @Throws(IOException::class)
    private fun getOkhttpRequest(inD: DataInputStream): Request? {
        val builder = Request.Builder()
        val requestLine = inD.readLine() ?: return null
        Log.d("$TAG[REQUEST LINE]", requestLine)
        val requestLineValues = requestLine.split("\\s+".toRegex())

        var host: String? = null
        var mime: String? = null
        var len = 0

        var line: String
        loop@ while(true){
            line = inD.readLine()
            if(DEBUG) Log.d("$TAG[LINE]", line)
            val header = line.split(": ")
            when(header[0]) {
                "" -> break@loop
                "Host" -> host = header[1]
                "Content-Type" -> mime = header[1]
                "Content-Length" -> len = header[1].toInt()
                "Connection" -> keepAlive = header[1] != "close"
                else -> builder.addHeader(header[0], header[1])
            }
        }
        // https://www.w3.org/Protocols/rfc2616/rfc2616-sec5.html
        val url =  if(host!! in requestLineValues[1])
            requestLineValues[1]
        else
            "http://$host${requestLineValues[1]}"
        Log.d(TAG, url)
        builder.url(url)

        // read data, perhaps binary (so BufferedReader will not work)
        var reqBody: RequestBody? = null
        if(len > 0){
            val buf = ByteArray(len)
            val tmp = inD.read(buf, 0, len)
            if(tmp != len) Log.wtf(TAG, "Could not read all body bytes (-_-)")
            reqBody = RequestBody.create(MediaType.parse(mime), buf)
        }
        builder.method(requestLineValues[0], reqBody)
        return builder.build()
    }

    private fun sendResponseHeaders(res: Response, outClient: OutputStream){
        outClient.write(res.protocol().toString().toUpperCase().toByteArray())
        outClient.write(0x20) // space
        outClient.write(res.code().toString().toByteArray())
        outClient.write(0x20) // space
        outClient.write(res.message().toByteArray())
        outClient.write(byteArrayOf(0x0d, 0x0a)) //CRLF
        outClient.write(res.headers().toString().replace("\n","\r\n").toByteArray())
        outClient.write(byteArrayOf(0x0d, 0x0a)) //CRLF
    }


    override fun read(socketChannel: SocketChannel, engine: SSLEngine) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun write(socketChannel: SocketChannel, engine: SSLEngine, message: String) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }



    @Throws(Exception::class)
    private fun doHandshake(sock: Socket, engine: SSLEngine,
                            myNetData: ByteBuffer, peerNetData: ByteBuffer, appBufSize: Int) {
        val inS = sock.getInputStream()
        val outS = sock.getOutputStream()
        // Create byte buffers to use for holding application data
        val myAppData = ByteBuffer.allocate(appBufSize)
        val peerAppData = ByteBuffer.allocate(appBufSize)

        // Begin handshake
        engine.beginHandshake()
        var hs = engine.handshakeStatus

        // Process handshaking message
        while (hs !== SSLEngineResult.HandshakeStatus.FINISHED
                && hs !== SSLEngineResult.HandshakeStatus.NOT_HANDSHAKING) {
            when (hs) {
                SSLEngineResult.HandshakeStatus.NEED_UNWRAP -> {
                    // Receive handshaking data from peer
                    if (inS.read(peerNetData.array()) < 0) {
                        // The channel has reached end-of-stream
                    }
                    // Process incoming handshaking data
                    peerNetData.flip()
                    val res = engine.unwrap(peerNetData, peerAppData)
                    peerNetData.compact()
                    hs = res.handshakeStatus

                    // Check status
                    when (res.status) {
                        SSLEngineResult.Status.OK -> {}
                        SSLEngineResult.Status.BUFFER_OVERFLOW -> TODO()
                        SSLEngineResult.Status.BUFFER_UNDERFLOW -> TODO()
                        SSLEngineResult.Status.CLOSED -> TODO()
                    }
                }
                SSLEngineResult.HandshakeStatus.NEED_WRAP -> {
                    // Empty the local network packet buffer.
                    myNetData.clear()
                    // Generate handshaking data
                    val res = engine.wrap(myAppData, myNetData)
                    hs = res.handshakeStatus
                    // Check status
                    when (res.status) {
                        SSLEngineResult.Status.OK -> {
                            myNetData.flip()
                            // Send the handshaking data to peer
                            while (myNetData.hasRemaining()) {
                                outS.write(myNetData.array())
                            }
                        }
                        SSLEngineResult.Status.BUFFER_OVERFLOW -> TODO()
                        SSLEngineResult.Status.BUFFER_UNDERFLOW -> TODO()
                        SSLEngineResult.Status.CLOSED -> TODO()
                    }
                }
                SSLEngineResult.HandshakeStatus.NEED_TASK -> { }
                SSLEngineResult.HandshakeStatus.FINISHED -> TODO()
                SSLEngineResult.HandshakeStatus.NOT_HANDSHAKING -> TODO()
            }
        }
    }

    @Throws(SSLHandshakeException::class)
    private fun parseSslClientHello(socket: Socket, engine: SSLEngine): ByteArrayInputStream {
        val inS = socket.getInputStream()


        var buffer = ByteArray(0xFF)
        var position = 0
        val capabilities: SSLCapabilities?

        // Read the header of TLS record
        while (position < SSLExplorer.RECORD_HEADER_SIZE) {
            val count = SSLExplorer.RECORD_HEADER_SIZE - position
            val n = inS.read(buffer, position, count)
            if (n < 0) {
                throw SSLHandshakeException("unexpected end of stream!")
            }
            position += n
        }

        // Get the required size to explore the SSL capabilities
        val recordLength = SSLExplorer.getRequiredSize(buffer, 0, position)
        if (buffer.size < recordLength) {
            buffer = buffer.copyOf(recordLength)
        }

        while (position < recordLength) {
            val count = recordLength - position
            val n = inS.read(buffer, position, count)
            if (n < 0) {
                throw SSLHandshakeException("unexpected end of stream!")
            }
            position += n
        }

        // Explore
        capabilities = SSLExplorer.explore(buffer, 0, recordLength)
        if (capabilities != null) {
            Log.d(TAG, "Record version: ${capabilities.recordVersion}")
            Log.d(TAG, "Hello version: ${capabilities.helloVersion}")
            Log.d(TAG, "Server names: ${capabilities.serverNames}")
            val sni = (capabilities.serverNames[0] as SNIHostName).asciiName
            EvilApApp.instance.ekm.engine2Alias[engine] = sni
        }

        // wrap the buffered bytes
        return ByteArrayInputStream(buffer, 0, position)
        //val bais = ByteArrayInputStream(buffer, 0, position)
        //sClient = EvilApApp.instance.sf.createSocket(socket,"127.0.0.1",EvilApApp.instance.PORT_PROXY,true)
    }

    /*
    private fun getEvilSSLSocket(): ServerSocket {
        //val sock = ssf.createServerSocket() as SSLServerSocket
        //sock.useClientMode = false
        // Use only TLSv1.2 to get access to ExtendedSSLSession
        // https://docs.oracle.com/javase/8/docs/technotes/guides/security/jsse/JSSERefGuide.html#SSLSession
        //val params = sock.sslParameters
        //params.protocols = arrayOf("TLSv1.2")
        //params.sniMatchers = listOf(EvilSniMatcher())
        //sock.sslParameters = params
        //sock.enabledProtocols = arrayOf("TLSv1.2")
        //return sock

    }
    */

}