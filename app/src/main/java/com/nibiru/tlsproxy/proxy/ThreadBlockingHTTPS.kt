package com.nibiru.tlsproxy.proxy

import android.util.Log
import com.nibiru.tlsproxy.crypto.EvilKeyManager
import com.nibiru.tlsproxy.crypto.SslPeer
import com.nibiru.tlsproxy.crypto.SSLCapabilities
import com.nibiru.tlsproxy.crypto.SSLExplorer
import okhttp3.Response
import java.io.ByteArrayInputStream
import java.io.DataInputStream
import java.io.IOException
import java.io.OutputStream
import java.net.Socket
import java.net.SocketTimeoutException
import java.nio.ByteBuffer
import java.nio.channels.SelectionKey
import java.nio.channels.SocketChannel
import java.util.concurrent.ExecutorService
import javax.net.ssl.*


class ThreadBlockingHTTPS(private val sClient: Socket,
                          private val engine: SSLEngine,
                          private val ekm: EvilKeyManager,
                          executor: ExecutorService) : SslPeer("", 6666, executor) {

    /**************************************CLASS FIELDS********************************************/
    private val TAG = javaClass.simpleName
    private var keepAlive = true
    private val DEBUG = false
    /**************************************CLASS METHODS*******************************************/

    @Throws(Exception::class)
    override fun run() {
        engine.useClientMode = false
        val sess = engine.session
        val clientHello = parseSslClientHello(sClient, engine)

        myAppData = ByteBuffer.allocate(sess.applicationBufferSize)
        myNetData = ByteBuffer.allocate(sess.packetBufferSize)
        peerAppData = ByteBuffer.allocate(sess.applicationBufferSize)
        peerNetData = ByteBuffer.allocate(sess.packetBufferSize)

        //things we will eventually close
        var inData: DataInputStream?= null
        var outStream: OutputStream? = null
        try {
            inData = DataInputStream(sClient.getInputStream())
            outStream = sClient.getOutputStream()
            if (clientHello != null && doHandshake(inData, outStream, engine, clientHello)) {
                val decrypted = read(inData, outStream, engine) ?: throw IOException("Could not get decrypted stream")
                processAppData(decrypted, inData, outStream, engine)
            } else {
                sClient.close()
                Log.e(TAG,"Connection closed due to handshake failure.")
            }
        } catch (e: IOException) {
            if (e is SocketTimeoutException)
                Log.e(TAG, "Timeout!")
            if (e is SSLHandshakeException)
                Log.e(TAG, "SSL Handshake Exception!")
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

    @Throws(IOException::class)
    override fun read(inData: DataInputStream, outStream: OutputStream, engine: SSLEngine): DataInputStream? {
        peerNetData.clear()
        var bytesRead = 0
        if (peerNetData.position() == 0) {
            bytesRead = inData.read(peerNetData.array())
        }
        if (bytesRead > 0) {
            peerNetData.position(bytesRead)
            peerNetData.flip()
            while (peerNetData.hasRemaining()) {
                peerAppData.clear()
                val result = engine.unwrap(peerNetData, peerAppData)
                when (result.status) {
                    SSLEngineResult.Status.OK -> {
                        peerAppData.flip()
                        if (DEBUG) Log.d(TAG,"[IN]:\n" + String(peerAppData.array()))
                    }
                    SSLEngineResult.Status.BUFFER_OVERFLOW -> {
                        peerAppData = enlargeApplicationBuffer(engine, peerAppData)
                    }
                    SSLEngineResult.Status.BUFFER_UNDERFLOW -> {
                        peerNetData = handleBufferUnderflow(engine, peerNetData)
                    }
                    SSLEngineResult.Status.CLOSED -> {
                        Log.e(TAG,"Client wants to close connection...")
                        closeConnection(inData, outStream, engine)
                        Log.e(TAG,"Goodbye client!")
                        return null
                    }
                    else -> {
                        throw IllegalStateException("Invalid SSL status: " + result.status)
                    }
                }
            }
            val out = DataInputStream(ByteArrayInputStream(peerAppData.array(), 0, peerAppData.limit()))
            return out

        } else if (bytesRead < 0) {
            Log.e(TAG,"Received end of stream. Will try to close connection with client...")
            handleEndOfStream(inData, outStream, engine)
            Log.e(TAG,"Goodbye client!")
        }
        return null
    }

    @Throws(IOException::class)
    override fun write(inData: DataInputStream, outStream: OutputStream, engine: SSLEngine, message: ByteArray) {
        myAppData.clear()
        myAppData.put(message)
        myAppData.flip()
        while (myAppData.hasRemaining()) {
            // The loop has a meaning for (outgoing) messages larger than 16KB.
            // Every wrap call will remove 16KB from the original message and send it to the remote peer.
            myNetData.clear()
            val result = engine.wrap(myAppData, myNetData)
            when (result.status) {
                SSLEngineResult.Status.OK -> {
                    myNetData.flip()
                    while (myNetData.hasRemaining()) {
                        outStream.write(myNetData.array(), 0, myNetData.limit())
                        myNetData.position(myNetData.limit())
                    }
                    Log.e(TAG,"[OUT]:\n $message")
                }
                SSLEngineResult.Status.BUFFER_OVERFLOW -> myNetData = enlargePacketBuffer(engine, myNetData)
                SSLEngineResult.Status.BUFFER_UNDERFLOW -> throw SSLException("Buffer underflow occured after a wrap. I don't think we should ever get here.")
                SSLEngineResult.Status.CLOSED -> {
                    closeConnection(inData, outStream, engine)
                    return
                }
                else -> throw IllegalStateException("Invalid SSL status: " + result.status)
            }
        }
    }
    @Throws(IOException::class)
    private fun processAppData(inDecrypted: DataInputStream, inRaw: DataInputStream,
                               outStream: OutputStream, engine: SSLEngine){
        var res: Response? = null
        //HTTP KEEP ALIVE LOOP!
        while (keepAlive) {
            //get client request string as okhttp request
            val req = getOkhttpRequest(inDecrypted, "https")
            if (req == null) {
                Log.e(TAG, "Cannot read request, closing")
                return
            }

            val res = "HTTP/1.1 200 OK\r\nContent-Length: 18\r\n\r\nHTTPS Proxy Hello!"
            write(inRaw, outStream, engine, res.toByteArray())
            //res = TLSProxyApp.instance.httpClient.newCall(req).execute()

            //sendResponseHeaders(res, outStream)
            //res.body()?.apply { write(inRaw, outStream, engine, this.bytes()) }
            outStream.flush()
            Log.d(TAG, "[SEND]")
        }
    }

    @Throws(SSLHandshakeException::class)
    private fun parseSslClientHello(socket: Socket, engine: SSLEngine): ByteBuffer? {
        val ins = socket.getInputStream()

        var buffer = ByteArray(0xFF)
        var position = 0
        val capabilities: SSLCapabilities?

        try {
            // Read the header of TLS record
            while (position < SSLExplorer.RECORD_HEADER_SIZE) {
                val count = SSLExplorer.RECORD_HEADER_SIZE - position
                val n = ins.read(buffer, position, count)
                if (n < 0) { return null }
                position += n
            }
            // Get the required size to explore the SSL capabilities
            val recordLength = SSLExplorer.getRequiredSize(buffer, 0, position)
            if (buffer.size < recordLength) {
                buffer = buffer.copyOf(recordLength)
            }
            // Read the entire Client Hello
            while (position < recordLength) {
                val count = recordLength - position
                val n = ins.read(buffer, position, count)
                if (n < 0) { return null }
                position += n
            }
            capabilities = SSLExplorer.explore(buffer, 0, recordLength) ?: return null
            Log.d(TAG, "PARSED CLIENT_HELLO: Server names: ${capabilities.serverNames}")
            val sni = (capabilities.serverNames[0] as SNIHostName).asciiName
            ekm.engine2Alias[engine] = sni
            val buffer = ByteBuffer.wrap(buffer, 0, recordLength)
            return buffer
        }
        catch (e: Exception){
            e.printStackTrace()
            return null
        }
    }

    override fun accept(key: SelectionKey) {
        TODO("not implemented")
    }
    override fun read(socketChannel: SocketChannel, engine: SSLEngine?) {
        TODO("not implemented")
    }
    override fun write(socketChannel: SocketChannel, message: ByteArray, engine: SSLEngine?) {
        TODO("not implemented")
    }


}