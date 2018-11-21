package com.nibiru.tlsproxy.proxy

import android.util.Log
import com.nibiru.tlsproxy.crypto.EvilKeyManager
import com.nibiru.tlsproxy.crypto.SslPeer
import com.nibiru.tlsproxy.crypto.SSLCapabilities
import com.nibiru.tlsproxy.crypto.SSLExplorer
import java.io.ByteArrayInputStream
import java.io.DataInputStream
import java.io.IOException
import java.io.OutputStream
import java.lang.IllegalArgumentException
import java.lang.NullPointerException
import java.nio.BufferUnderflowException
import java.nio.ByteBuffer
import java.nio.channels.SelectionKey
import java.nio.channels.ServerSocketChannel
import java.nio.channels.SocketChannel
import java.util.concurrent.ExecutorService
import javax.net.ssl.*


class ThreadNioHTTPS(hostAddress: String, port: Int, val ekm: EvilKeyManager, executor: ExecutorService)
    : SslPeer(hostAddress, port, executor){
    /**************************************CLASS FIELDS********************************************/
    private val TAG = javaClass.simpleName
    private val DEBUG = false
    /**
     * The sslCtx will be initialized with a specific SSL/TLS protocol and will then be used
     * to create [SSLEngine] classes for each new connection that arrives to the server.
     */
    private var sslCtx: SSLContext = SSLContext.getInstance("TLS")
    /**************************************CLASS METHODS*******************************************/
    /**
     * Server is designed to apply an SSL/TLS protocol and listen to an IP address and port.
     *
     * @param hostAddress - the IP address this server will listen to.
     * @param port - the port this server will listen to.
     */
    init{
        sslCtx.init(arrayOf(ekm), null, null)

        val dummySession = sslCtx.createSSLEngine().session
        myAppData = ByteBuffer.allocate(dummySession.applicationBufferSize)
        myNetData = ByteBuffer.allocate(dummySession.packetBufferSize)
        peerAppData = ByteBuffer.allocate(dummySession.applicationBufferSize)
        peerNetData = ByteBuffer.allocate(dummySession.packetBufferSize)
        dummySession.invalidate()
    }

    /**
     * Will be called after a new connection request arrives to the server. Creates the [SocketChannel] that will
     * be used as the network layer link, and the [SSLEngine] that will encrypt and decrypt all the data
     * that will be exchanged during the session with this specific client.
     *
     * @param key - the key dedicated to the [ServerSocketChannel] used by the server to listen to new connection requests.
     * @throws Exception
     */
    @Throws(Exception::class)
    override fun accept(key: SelectionKey) {

        if (DEBUG) Log.d(TAG,"New connection request!")

        val socketChannel = (key.channel() as ServerSocketChannel).accept()
        socketChannel.configureBlocking(false)
        val engine = sslCtx.createSSLEngine()
        engine.useClientMode = false

        // WARNING: SHIT CODE AHEAD DUE TO JSSE BEING A BITCH AND NOT WORKING AS INTENDED
        // PARSE THE INITIAL CLIENT HELLO TO GET SNI
        // WORKAROUND TO ENGINE/SOCKET HAVING NULL HANDSHAKE SESSION WHEN CALLING chooseServerAlias
        val clientHello = parseSslClientHello(socketChannel, engine)
        // STARTING HANDSHAKE FORCES KEY MANAGER TO CHOOSE ALIAS WITH NULL HANDSHAKE SESSION
        // HAVE TO PARSE ClientHello B4 IN ORDER TO KNOW WHAT SERVER_NAME WAS REQUESTED
        // COULD HAVE USED createSocket(Socket s, InputStream consumed, boolean autoClose)
        // FUNCTION FROM SSLSocketFactory BUT IT'S NOT PRESENT IN ANDROID EVEN IN JDK 1.8
        // AGRR !!!
        //engine.beginHandshake()
        // PATCHED doHandshake TO ACCOUNT FOR CONSUMED ClientHello
        try{
            if (clientHello != null && doHandshake(socketChannel, engine, clientHello)) {
                socketChannel.register(selector, SelectionKey.OP_READ, engine)
            } else {
                //socketChannel.close()
                handleEndOfStream(socketChannel, engine)

                Log.e(TAG,"Connection closed due to handshake failure.")
            }
        } catch (e: Exception){
            //socketChannel.close()
            handleEndOfStream(socketChannel, engine)
            Log.e(TAG,"EXCEPTION (${e.cause})Connection closed due to handshake failure.")
        }
    }

    /**
     * Will be called by the selector when the specific socket channel has data to be read.
     * As soon as the server reads these data, it will call [NioSslServer.write]
     * to send back a trivial response.
     *
     * @param socketChannel - the transport link used between the two peers.
     * @param engine - the engine used for encryption/decryption of the data exchanged between the two peers.
     * @throws IOException if an I/O error occurs to the socket channel.
     */
    @Throws(IOException::class, NullPointerException::class)
    override fun read(socketChannel: SocketChannel, engine: SSLEngine?) {
        synchronized(this) {
            if (engine == null) throw IOException("HTTPS NEEDS SSL ENGINE!")

            peerNetData.clear()
            val bytesRead = socketChannel.read(peerNetData)
            if (bytesRead > 0) {
                peerNetData.flip()
                while (peerNetData.hasRemaining()) {
                    peerAppData.clear()
                    // sporadically throws java.lang.NullPointerException
                    val result = engine.unwrap(peerNetData, peerAppData)
                    when (result.status) {
                        SSLEngineResult.Status.OK -> {
                            peerAppData.flip()
                            if (DEBUG) Log.d(TAG, "[IN]:\n" + String(peerAppData.array()))
                        }
                        SSLEngineResult.Status.BUFFER_OVERFLOW -> {
                            peerAppData = enlargeApplicationBuffer(engine, peerAppData)
                        }
                        SSLEngineResult.Status.BUFFER_UNDERFLOW -> {
                            peerNetData = handleBufferUnderflow(engine, peerNetData)
                        }
                        SSLEngineResult.Status.CLOSED -> {
                            Log.e(TAG, "Client wants to close connection...")
                            closeConnection(socketChannel, engine)
                            Log.e(TAG, "Goodbye client!")
                            return
                        }
                        else -> {
                            throw IllegalStateException("Invalid SSL status: " + result.status)
                        }
                    }
                }
                val decryptedStream = DataInputStream(ByteArrayInputStream(peerAppData.array(), 0, peerAppData.limit()))
                //serveStaticResponse(socketChannel, engine)
                serveResponse(socketChannel, decryptedStream, engine)
            } else if (bytesRead < 0) {
                if (DEBUG) Log.e(TAG, "EOF. Will try to close connection with client...")
                handleEndOfStream(socketChannel, engine)
                //Log.e(TAG,"Goodbye client!")
            }
        }
    }

    /**
     * Will send a message back to client                                                                                                                                                                                                                                                               o a client.
     *
     * @param key - the key dedicated to the socket channel that will be used to write to the client.
     * @param message - the message to be sent.
     * @throws IOException if an I/O error occurs to the socket channel.
     */
    @Throws(IOException::class, IllegalArgumentException::class, BufferUnderflowException::class)
    override fun write(socketChannel: SocketChannel, message: ByteArray, engine: SSLEngine?) {
        synchronized(this) {
            if (engine == null) throw IllegalArgumentException("HTTPS NEEDS SSL ENGINE!")
            myAppData.clear()
            var current = 0
            val end = message.size - 1
            val step = myAppData.limit() - 1

            while (current < end) {
                val next = if (end - current > step) current + step else end
                if (DEBUG) Log.i(TAG, "current:$current, next:$next, end:$end")
                val slice = message.sliceArray(IntRange(current, next))
                if (current != 0) myAppData.flip()
                myAppData.put(slice)
                myAppData.flip()

                while (myAppData.hasRemaining()) {
                    // The loop has a meaning for (outgoing) messages larger than 16KB.
                    // Every wrap call will remove 16KB from the original message and send it to the remote peer.
                    myNetData.clear()
                    val result = engine.wrap(myAppData, myNetData)
                    when (result.status) {
                        SSLEngineResult.Status.OK -> {
                            myNetData.flip()
                            //while (myNetData.hasRemaining()) {
                            socketChannel.write(myNetData)
                            //}
                            //Log.e(TAG,"[OUT]:\n $message")
                        }
                        SSLEngineResult.Status.BUFFER_OVERFLOW -> myNetData = enlargePacketBuffer(engine, myNetData)
                        SSLEngineResult.Status.BUFFER_UNDERFLOW -> throw SSLException("Buffer underflow occured after a wrap. I don't think we should ever get here.")
                        SSLEngineResult.Status.CLOSED -> {
                            closeConnection(socketChannel, engine)
                            return
                        }
                        else -> throw IllegalStateException("Invalid SSL status: " + result.status)
                    }
                }

                current = next + 1
                if (DEBUG) Log.d(TAG, "[SENT] ${String(slice)}")
            }
        }
    }

    @Throws(SSLHandshakeException::class)
    private fun parseSslClientHello(socketChannel: SocketChannel, engine: SSLEngine): ByteBuffer? {

        var buffer = ByteBuffer.allocate(0xFF)
        val capabilities: SSLCapabilities?

        try {
            // Read the header of TLS record
            while (buffer.position() < SSLExplorer.RECORD_HEADER_SIZE) {
                if (socketChannel.read(buffer) < 0){ return null }
            }
            // Get the required size to explore the SSL capabilities
            val recordLength = SSLExplorer.getRequiredSize(buffer.array(), 0, SSLExplorer.RECORD_HEADER_SIZE)
            if (buffer.capacity() < recordLength) { //TODO: test?
                val copy = ByteBuffer.allocate(recordLength)
                buffer.rewind()
                copy.put(buffer)
                buffer = copy
            }
            // Read entire Client Hello
            while (buffer.position() < recordLength) {
                if (socketChannel.read(buffer) < 0) { return null }
            }
            buffer.flip()
            capabilities = SSLExplorer.explore(buffer.asReadOnlyBuffer()) ?: return null
            if (DEBUG) Log.d(TAG, "PARSED CLIENT_HELLO: Server names: ${capabilities.serverNames}")
            if (capabilities.serverNames.size == 0){
                Log.e(TAG, "Could not find SNI, will serve CA certificate...")
                ekm.engine2Alias[engine] = ekm.caAlias
            }
            else {
                val sni = (capabilities.serverNames[0] as SNIHostName).asciiName
                ekm.engine2Alias[engine] = sni
            }
            return buffer
        }
        catch (e: Exception){
            e.printStackTrace()
            return null
        }
    }

    private fun serveStaticResponse(socketChannel: SocketChannel, engine: SSLEngine){
        var out = "HTTP/1.1 200 OK\r\nContent-Length: 191\r\n\r\n"
        out += "=== HTTPS PROXY ===\n"
        out += "Connection was accepted...\n"
        out += "TLS Handshake was parsed for SNI...\n"
        out += "TLS Handshake was completed...\n"
        out += "X509 Certificate was generated and signed...\n"
        out += "And static response was served!\n"
        write(socketChannel, out.toByteArray(), engine)
    }

    override fun read(inData: DataInputStream, outSteam: OutputStream, engine: SSLEngine): DataInputStream? {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun write(inData: DataInputStream, outSteam: OutputStream, engine: SSLEngine, message: ByteArray) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

}