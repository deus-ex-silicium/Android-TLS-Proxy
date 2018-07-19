package com.nibiru.evilap.proxy

import android.util.Log
import com.nibiru.evilap.EvilApApp
import com.nibiru.evilap.pki.EvilKeyManager
import com.nibiru.evilap.pki.NioSslPeer
import com.nibiru.evilap.pki.SSLCapabilities
import com.nibiru.evilap.pki.SSLExplorer
import java.io.IOException
import java.net.InetSocketAddress
import java.net.Socket
import java.nio.ByteBuffer
import java.nio.channels.SelectionKey
import java.nio.channels.Selector
import java.nio.channels.ServerSocketChannel
import java.nio.channels.SocketChannel
import java.nio.channels.spi.SelectorProvider
import javax.net.ssl.*


class ThreadNioProxy(val hostAddress: String, val port: Int, val ekm: EvilKeyManager) : Runnable, NioSslPeer(){
    /**************************************CLASS FIELDS********************************************/
    private val TAG = javaClass.simpleName
    /**
     * Declares if the server is active to serve and create new connections.
     */
    private var active: Boolean = false
    /**
     * The sslCtx will be initialized with a specific SSL/TLS protocol and will then be used
     * to create [SSLEngine] classes for each new connection that arrives to the server.
     */
    private var sslCtx: SSLContext
    /**
     * A part of Java NIO that will be used to serve all connections to the server in one thread.
     */
    private var selector: Selector
    /**************************************CLASS METHODS*******************************************/
    /**
     * Server is designed to apply an SSL/TLS protocol and listen to an IP address and port.
     *
     * @param hostAddress - the IP address this server will listen to.
     * @param port - the port this server will listen to.
     */
    init{
        sslCtx = SSLContext.getInstance("TLS")
        sslCtx.init(arrayOf(ekm), null, null)

        val dummySession = sslCtx.createSSLEngine().session
        myAppData = ByteBuffer.allocate(dummySession.applicationBufferSize)
        myNetData = ByteBuffer.allocate(dummySession.packetBufferSize)
        peerAppData = ByteBuffer.allocate(dummySession.applicationBufferSize)
        peerNetData = ByteBuffer.allocate(dummySession.packetBufferSize)
        dummySession.invalidate()

        selector = SelectorProvider.provider().openSelector()

        active = true
    }
    /**
     * Should be called in order the server to start listening to new connections.
     * This method will run in a loop as long as the server is active. In order to stop the server
     * you should use [NioSslServer.stop] which will set it to inactive state
     * and also wake up the listener, which may be in blocking select() state.
     *
     * @throws Exception
     */
    @Throws(Exception::class)
    override fun run() {
        val serverSocketChannel = ServerSocketChannel.open()
        serverSocketChannel.configureBlocking(false)
        serverSocketChannel.socket().bind(InetSocketAddress(hostAddress, port))
        serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT)

        Log.e(TAG,"Initialized and waiting for new connections...")

        while (isActive()) {
            selector.select()
            val selectedKeys = selector.selectedKeys().iterator()
            while (selectedKeys.hasNext()) {
                val key = selectedKeys.next()
                selectedKeys.remove()
                if (!key.isValid) {
                    continue
                }
                if (key.isAcceptable) {
                    accept(key)
                } else if (key.isReadable) {
                    read(key.channel() as SocketChannel, key.attachment() as SSLEngine)
                }
            }
        }

        Log.e(TAG,"Goodbye!")

    }

    /**
     * Sets the server to an inactive state, in order to exit the reading loop in [NioSslServer.start]
     * and also wakes up the selector, which may be in select() blocking state.
     */
    fun stop() {
        Log.e(TAG,"Will now close server...")
        active = false
        executor.shutdown()
        selector.wakeup()
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
    private fun accept(key: SelectionKey) {

        Log.e(TAG,"New connection request!")

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
        engine.beginHandshake()
        // PATCHED doHandshake TO ACCOUNT FOR CONSUMED ClientHello
        if (clientHello != null && doHandshake(socketChannel, engine, clientHello)) {
            socketChannel.register(selector, SelectionKey.OP_READ, engine)
        } else {
            socketChannel.close()
            Log.e(TAG,"Connection closed due to handshake failure.")
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
    @Throws(IOException::class)
    override fun read(socketChannel: SocketChannel, engine: SSLEngine) {

        Log.e(TAG,"About to read from a client...")

        peerNetData.clear()
        val bytesRead = socketChannel.read(peerNetData)
        if (bytesRead > 0) {
            peerNetData.flip()
            while (peerNetData.hasRemaining()) {
                peerAppData.clear()
                val result = engine.unwrap(peerNetData, peerAppData)
                when (result.status) {
                    SSLEngineResult.Status.OK -> {
                        peerAppData.flip()
                        Log.e(TAG,"Incoming message: " + String(peerAppData.array()))
                    }
                    SSLEngineResult.Status.BUFFER_OVERFLOW -> {
                        peerAppData = enlargeApplicationBuffer(engine, peerAppData)
                    }
                    SSLEngineResult.Status.BUFFER_UNDERFLOW -> {
                        peerNetData = handleBufferUnderflow(engine, peerNetData)
                    }
                    SSLEngineResult.Status.CLOSED -> {
                        Log.e(TAG,"Client wants to close connection...")
                        closeConnection(socketChannel, engine)
                        Log.e(TAG,"Goodbye client!")
                        return
                    }
                    else -> {
                        throw IllegalStateException("Invalid SSL status: " + result.status)
                    }
                }
            }
            write(socketChannel, engine, "HTTP/1.1 200 OK\r\n")
            write(socketChannel, engine, "Content-Length: 18\r\n")
            write(socketChannel, engine, "\r\n") //CRLF
            write(socketChannel, engine, "HTTPS Proxy Hello!")

        } else if (bytesRead < 0) {
            Log.e(TAG,"Received end of stream. Will try to close connection with client...")
            handleEndOfStream(socketChannel, engine)
            Log.e(TAG,"Goodbye client!")
        }
    }

    /**
     * Will send a message back to a client.
     *
     * @param key - the key dedicated to the socket channel that will be used to write to the client.
     * @param message - the message to be sent.
     * @throws IOException if an I/O error occurs to the socket channel.
     */
    @Throws(IOException::class)
    override fun write(socketChannel: SocketChannel, engine: SSLEngine, message: String) {

        Log.e(TAG,"About to write to a client...")

        myAppData.clear()
        myAppData.put(message.toByteArray())
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
                        socketChannel.write(myNetData)
                    }
                    Log.e(TAG,"Message sent to the client: $message")
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
            Log.d(TAG, "PARSED CLIENT_HELLO: Server names: ${capabilities.serverNames}")
            val sni = (capabilities.serverNames[0] as SNIHostName).asciiName
            ekm.engine2Alias[engine] = sni
            return buffer
        }
        catch (e: Exception){
            e.printStackTrace()
            return null
        }
    }



    /**
     * Determines if the the server is active or not.
     *
     * @return if the server is active or not.
     */
    private fun isActive(): Boolean {
        return active
    }
}