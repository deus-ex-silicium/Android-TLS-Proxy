package com.nibiru.tlsproxy.crypto

import android.util.Log
import com.nibiru.tlsproxy.proxy.ThreadNioBase
import java.io.*
import java.nio.BufferOverflowException
import java.nio.ByteBuffer
import java.nio.channels.SocketChannel
import java.util.concurrent.ExecutorService
import javax.net.ssl.*

// https://github.com/alkarn/sslengine.example
/**
 * A class that represents an SSL/TLS peer, and can be extended to create a client or a server.
 *
 *
 * It makes use of the JSSE framework, and specifically the [SSLEngine] logic, which
 * is described by Oracle as "an advanced API, not appropriate for casual use", since
 * it requires the user to implement much of the communication establishment procedure himself.
 * More information about it can be found here: http://docs.oracle.com/javase/8/docs/technotes/guides/security/jsse/JSSERefGuide.html#SSLEngine
 *
 *
 * [SslPeer] implements the handshake protocol, required to establish a connection between two peers,
 * which is common for both client and server and provides the abstract [SslPeer.read] and
 * [SslPeer.write] methods, that need to be implemented by the specific SSL/TLS peer
 * that is going to extend this class.
 *
 * @author [Alex Karnezis](mailto:alex.a.karnezis@gmail.com)
 */
abstract class SslPeer(hostAddress: String, port: Int, private val executor: ExecutorService)
    : ThreadNioBase(hostAddress, port) {

    private val TAG = javaClass.simpleName
    /**
     * Will contain this peer's application data in plaintext, that will be later encrypted
     * using [SSLEngine.wrap] and sent to the other peer. This buffer can typically
     * be of any size, as long as it is large enough to contain this peer's outgoing messages.
     * If this peer tries to send a message bigger than buffer's capacity a [BufferOverflowException]
     * will be thrown.
     */
    override lateinit var myAppData: ByteBuffer

    /**
     * Will contain this peer's encrypted data, that will be generated after [SSLEngine.wrap]
     * is applied on [SslPeer.myAppData]. It should be initialized using [SSLSession.getPacketBufferSize],
     * which returns the size up to which, SSL/TLS packets will be generated from the engine under a session.
     * All SSLEngine network buffers should be sized at least this large to avoid insufficient space problems when performing wrap and unwrap calls.
     */
    protected lateinit var myNetData: ByteBuffer

    /**
     * Will contain the other peer's (decrypted) application data. It must be large enough to hold the application data
     * from any peer. Can be initialized with [SSLSession.getApplicationBufferSize] for an estimation
     * of the other peer's application data and should be enlarged if this size is not enough.
     */
    override lateinit var peerAppData: ByteBuffer

    /**
     * Will contain the other peer's encrypted data. The SSL/TLS protocols specify that implementations should produce packets containing at most 16 KB of plaintext,
     * so a buffer sized to this value should normally cause no capacity problems. However, some implementations violate the specification and generate large records up to 32 KB.
     * If the [SSLEngine.unwrap] detects large inbound packets, the buffer sizes returned by SSLSession will be updated dynamically, so the this peer
     * should check for overflow conditions and enlarge the buffer using the session's (updated) buffer size.
     */
    protected lateinit var peerNetData: ByteBuffer

    @Throws(Exception::class)
    abstract override fun read(socketChannel: SocketChannel, engine: SSLEngine?)
    @Throws(Exception::class)
    protected abstract fun read(inData: DataInputStream, outSteam: OutputStream,
                                engine: SSLEngine): DataInputStream?
    @Throws(Exception::class)
    protected abstract fun write(inData: DataInputStream, outSteam: OutputStream,
                                 engine: SSLEngine, message: ByteArray)


    /**
     * Implements the handshake protocol between two peers, required for the establishment of the SSL/TLS connection.
     * During the handshake, encryption configuration information - such as the list of available cipher suites - will be exchanged
     * and if the handshake is successful will lead to an established SSL/TLS session.
     *
     *
     *
     * A typical handshake will usually contain the following steps:
     *
     *
     *  * 1. wrap:     ClientHello
     *  * 2. unwrap:   ServerHello/Cert/ServerHelloDone
     *  * 3. wrap:     ClientKeyExchange
     *  * 4. wrap:     ChangeCipherSpec
     *  * 5. wrap:     Finished
     *  * 6. unwrap:   ChangeCipherSpec
     *  * 7. unwrap:   Finished
     *
     *
     *
     * Handshake is also used during the end of the session, in order to properly close the connection between the two peers.
     * A proper connection close will typically include the one peer sending a CLOSE message to another, and then wait for
     * the other's CLOSE message to close the transport link. The other peer from his perspective would read a CLOSE message
     * from his peer and then enter the handshake procedure to send his own CLOSE message as well.
     *
     * @param socketChannel - the socket channel that connects the two peers.
     * @param engine - the engine that will be used for encryption/decryption of the data exchanged with the other peer.
     * @return True if the connection handshake was successful or false if an error occurred.
     * @throws IOException - if an error occurs during read/write to the socket channel.
     */
    @Throws(IOException::class, BufferOverflowException::class)
    protected fun doHandshake(socketChannel: SocketChannel, engine: SSLEngine, clientHello: ByteBuffer?): Boolean {

        //Log.d(TAG,"About to do handshake...")
        var engineReadClientHello = false
        var result: SSLEngineResult
        var handshakeStatus: SSLEngineResult.HandshakeStatus

        // SslPeer's fields myAppData and peerAppData are supposed to be large enough to hold all message data the peer
        // will send and expects to receive from the other peer respectively. Since the messages to be exchanged will usually be less
        // than 16KB long the capacity of these fields should also be smaller. Here we initialize these two local buffers
        // to be used for the handshake, while keeping client's buffers at the same size.
        val appBufferSize = engine.session.applicationBufferSize
        val myAppData = ByteBuffer.allocate(appBufferSize)
        var peerAppData = ByteBuffer.allocate(appBufferSize)
        myNetData.clear()
        peerNetData.clear()

        handshakeStatus = SSLEngineResult.HandshakeStatus.NEED_UNWRAP
        loop@ while (handshakeStatus !== SSLEngineResult.HandshakeStatus.FINISHED
                && handshakeStatus !== SSLEngineResult.HandshakeStatus.NOT_HANDSHAKING) {
            //Log.d(TAG, "hs status:$handshakeStatus")
            when (handshakeStatus) {
                SSLEngineResult.HandshakeStatus.NEED_UNWRAP -> {
                    //WARNING: PATCH FOR CONSUMED CLIENT HELLO
                    if(clientHello != null && !engineReadClientHello){
                        peerNetData.put(clientHello)
                        engineReadClientHello = true
                    }
                    else {
                        if (socketChannel.read(peerNetData) < 0) {
                            if (engine.isInboundDone && engine.isOutboundDone) return false
                            try { engine.closeInbound() }
                            catch (e: SSLException) {
                                Log.e(TAG, "This engine was forced to close inbound, without having received the proper SSL/TLS close notification message from the peer, due to end of stream.")
                            }
                            engine.closeOutbound()
                            // After closeOutbound the engine will be set to WRAP state, in order to try to send a close message to the client.
                            handshakeStatus = engine.handshakeStatus
                            continue@loop
                        }
                    }
                    peerNetData.flip()
                    try {
                        result = engine.unwrap(peerNetData, peerAppData)
                        peerNetData.compact()
                        handshakeStatus = result.handshakeStatus
                    } catch (sslException: SSLException) {
                        Log.e(TAG,"(${sslException.message}) Will try to properly close connection...")
                        engine.closeOutbound()
                        handshakeStatus = engine.handshakeStatus
                        continue@loop
                    }

                    when (result.status) {
                        SSLEngineResult.Status.OK -> { }
                        SSLEngineResult.Status.BUFFER_OVERFLOW -> {
                            // Will occur when peerAppData's capacity is smaller than the data derived from peerNetData's unwrap.
                            peerAppData = enlargeApplicationBuffer(engine, peerAppData)
                        }
                        SSLEngineResult.Status.BUFFER_UNDERFLOW -> {
                            // Will occur either when no data was read from the peer or when the peerNetData buffer was too small to hold all peer's data.
                            peerNetData = handleBufferUnderflow(engine, peerNetData)
                        }
                        SSLEngineResult.Status.CLOSED -> {
                            if (engine.isOutboundDone) {
                                return false
                            }
                            else {
                                engine.closeOutbound()
                                handshakeStatus = engine.handshakeStatus
                                continue@loop
                            }
                        }
                        else -> throw IllegalStateException("Invalid SSL status: " + result.status)
                    }
                }
                SSLEngineResult.HandshakeStatus.NEED_WRAP -> {
                    myNetData.clear()
                    try {
                        result = engine.wrap(myAppData, myNetData)
                        handshakeStatus = result.handshakeStatus
                    } catch (sslException: SSLException) {
                        Log.e(TAG,"(${sslException.message}) Will try to properly close connection...")
                        engine.closeOutbound()
                        handshakeStatus = engine.handshakeStatus
                        continue@loop
                    }

                    when (result.status) {
                        SSLEngineResult.Status.OK -> {
                            myNetData.flip()
                            while (myNetData.hasRemaining()) {
                                socketChannel.write(myNetData)
                            }
                        }
                        SSLEngineResult.Status.BUFFER_OVERFLOW ->
                            // Will occur if there is not enough space in myNetData buffer to write all the data that would be generated by the method wrap.
                            // Since myNetData is set to session's packet size we should not get to this point because SSLEngine is supposed
                            // to produce messages smaller or equal to that, but a general handling would be the following:
                            myNetData = enlargePacketBuffer(engine, myNetData)
                        SSLEngineResult.Status.BUFFER_UNDERFLOW -> throw SSLException("Buffer underflow occured after a wrap. I don't think we should ever get here.")
                        SSLEngineResult.Status.CLOSED -> try {
                            myNetData.flip()
                            while (myNetData.hasRemaining()) {
                                socketChannel.write(myNetData)
                            }
                            // At this point the handshake status will probably be NEED_UNWRAP so we make sure that peerNetData is clear to read.
                            peerNetData.clear()
                        } catch (e: Exception) {
                            Log.e(TAG,"Failed to send server's CLOSE message due to socket channel's failure.")
                            handshakeStatus = engine.handshakeStatus
                        }

                        else -> throw IllegalStateException("Invalid SSL status: " + result.status)
                    }
                }
                SSLEngineResult.HandshakeStatus.NEED_TASK -> {
                    var task = engine.delegatedTask
                    while (task != null) {
                        executor.execute(task)
                        task = engine.delegatedTask
                    }
                    handshakeStatus = engine.handshakeStatus
                }
                SSLEngineResult.HandshakeStatus.FINISHED -> { }
                SSLEngineResult.HandshakeStatus.NOT_HANDSHAKING -> { }
                else -> throw IllegalStateException("Invalid SSL status: $handshakeStatus")
            }
        }
        return true
    }
    @Throws(IOException::class)
    protected fun doHandshake(inData: DataInputStream, outSteam: OutputStream,
                              engine: SSLEngine, clientHello: ByteBuffer?): Boolean {

        Log.d(TAG,"About to do handshake...")
        var engineReadClientHello = false
        var result: SSLEngineResult
        var handshakeStatus: SSLEngineResult.HandshakeStatus

        // SslPeer's fields myAppData and peerAppData are supposed to be large enough to hold all message data the peer
        // will send and expects to receive from the other peer respectively. Since the messages to be exchanged will usually be less
        // than 16KB long the capacity of these fields should also be smaller. Here we initialize these two local buffers
        // to be used for the handshake, while keeping client's buffers at the same size.
        val appBufferSize = engine.session.applicationBufferSize
        val myAppData = ByteBuffer.allocate(appBufferSize)
        var peerAppData = ByteBuffer.allocate(appBufferSize)
        myNetData.clear()
        peerNetData.clear()

        handshakeStatus = SSLEngineResult.HandshakeStatus.NEED_UNWRAP
        loop@ while (handshakeStatus !== SSLEngineResult.HandshakeStatus.FINISHED
                && handshakeStatus !== SSLEngineResult.HandshakeStatus.NOT_HANDSHAKING) {
            when (handshakeStatus) {
                SSLEngineResult.HandshakeStatus.NEED_UNWRAP -> {
                    //WARNING: PATCH FOR CONSUMED CLIENT HELLO
                    if(clientHello != null && !engineReadClientHello){
                        peerNetData.put(clientHello)
                        engineReadClientHello = true
                    }
                    else {
                        var read = 0
                        if (peerNetData.position() == 0) {
                            read = inData.read(peerNetData.array())
                        }
                        if (read < 0) {
                            if (engine.isInboundDone && engine.isOutboundDone) return false
                            try { engine.closeInbound() }
                            catch (e: SSLException) {
                                Log.e(TAG, "This engine was forced to close inbound, without having received the proper SSL/TLS close notification message from the peer, due to end of stream.")
                            }
                            engine.closeOutbound()
                            // After closeOutbound the engine will be set to WRAP state, in order to try to send a close message to the client.
                            handshakeStatus = engine.handshakeStatus
                            continue@loop
                        }
                        else {
                            peerNetData.position(read)
                        }
                    }
                    peerNetData.flip()
                    try {
                        result = engine.unwrap(peerNetData, peerAppData)
                        peerNetData.compact()
                        handshakeStatus = result.handshakeStatus
                    } catch (sslException: SSLException) {
                        Log.e(TAG,"(${sslException.message}) Will try to properly close connection...")
                        engine.closeOutbound()
                        handshakeStatus = engine.handshakeStatus
                        continue@loop
                    }

                    when (result.status) {
                        SSLEngineResult.Status.OK -> { }
                        SSLEngineResult.Status.BUFFER_OVERFLOW -> {
                            // Will occur when peerAppData's capacity is smaller than the data derived from peerNetData's unwrap.
                            peerAppData = enlargeApplicationBuffer(engine, peerAppData)
                        }
                        SSLEngineResult.Status.BUFFER_UNDERFLOW -> {
                            // Will occur either when no data was read from the peer or when the peerNetData buffer was too small to hold all peer's data.
                            peerNetData = handleBufferUnderflow(engine, peerNetData)
                        }
                        SSLEngineResult.Status.CLOSED -> {
                            if (engine.isOutboundDone) {
                                return false
                            }
                            else {
                                engine.closeOutbound()
                                handshakeStatus = engine.handshakeStatus
                                continue@loop
                            }
                        }
                        else -> throw IllegalStateException("Invalid SSL status: " + result.status)
                    }
                }
                SSLEngineResult.HandshakeStatus.NEED_WRAP -> {
                    myNetData.clear()
                    try {
                        result = engine.wrap(myAppData, myNetData)
                        handshakeStatus = result.handshakeStatus
                    } catch (sslException: SSLException) {
                        Log.e(TAG,"(${sslException.message}) Will try to properly close connection...")
                        engine.closeOutbound()
                        handshakeStatus = engine.handshakeStatus
                        continue@loop
                    }

                    when (result.status) {
                        SSLEngineResult.Status.OK -> {
                            myNetData.flip()
                            while (myNetData.hasRemaining()) {
                                outSteam.write(myNetData.array(), 0, myNetData.limit())
                                myNetData.position(myNetData.limit())
                            }
                        }
                        SSLEngineResult.Status.BUFFER_OVERFLOW ->
                            // Will occur if there is not enough space in myNetData buffer to write all the data that would be generated by the method wrap.
                            // Since myNetData is set to session's packet size we should not get to this point because SSLEngine is supposed
                            // to produce messages smaller or equal to that, but a general handling would be the following:
                            myNetData = enlargePacketBuffer(engine, myNetData)
                        SSLEngineResult.Status.BUFFER_UNDERFLOW -> throw SSLException("Buffer underflow occured after a wrap. I don't think we should ever get here.")
                        SSLEngineResult.Status.CLOSED -> try {
                            myNetData.flip()
                            while (myNetData.hasRemaining()) {
                                outSteam.write(myNetData.array(), 0, myNetData.limit())
                                myNetData.position(myNetData.limit())
                            }
                            // At this point the handshake status will probably be NEED_UNWRAP so we make sure that peerNetData is clear to read.
                            peerNetData.clear()
                        } catch (e: Exception) {
                            Log.e(TAG,"Failed to send server's CLOSE message due to socket channel's failure.")
                            handshakeStatus = engine.handshakeStatus
                        }

                        else -> throw IllegalStateException("Invalid SSL status: " + result.status)
                    }
                }
                SSLEngineResult.HandshakeStatus.NEED_TASK -> {
                    var task = engine.delegatedTask
                    while (task != null) {
                        executor.execute(task)
                        task = engine.delegatedTask
                    }
                    handshakeStatus = engine.handshakeStatus
                }
                SSLEngineResult.HandshakeStatus.FINISHED -> { }
                SSLEngineResult.HandshakeStatus.NOT_HANDSHAKING -> { }
                else -> throw IllegalStateException("Invalid SSL status: $handshakeStatus")
            }
        }
        return true
    }

    protected fun enlargePacketBuffer(engine: SSLEngine, buffer: ByteBuffer): ByteBuffer {
        return enlargeBuffer(buffer, engine.session.packetBufferSize)
    }

    protected fun enlargeApplicationBuffer(engine: SSLEngine, buffer: ByteBuffer): ByteBuffer {
        return enlargeBuffer(buffer, engine.session.applicationBufferSize)
    }

    /**
     * Compares `sessionProposedCapacity` with buffer's capacity. If buffer's capacity is smaller,
     * returns a buffer with the proposed capacity. If it's equal or larger, returns a buffer
     * with capacity twice the size of the initial one.
     *
     * @param buffer - the buffer to be enlarged.
     * @param sessionProposedCapacity - the minimum size of the new buffer, proposed by [SSLSession].
     * @return A new buffer with a larger capacity.
    `` */
    protected fun enlargeBuffer(buffer: ByteBuffer, sessionProposedCapacity: Int): ByteBuffer {
        return if (sessionProposedCapacity > buffer.capacity()) {
            ByteBuffer.allocate(sessionProposedCapacity)
        } else {
            ByteBuffer.allocate(buffer.capacity() * 2)
        }
    }

    /**
     * Handles [SSLEngineResult.Status.BUFFER_UNDERFLOW]. Will check if the buffer is already filled, and if there is no space problem
     * will return the same buffer, so the client tries to read again. If the buffer is already filled will try to enlarge the buffer either to
     * session's proposed size or to a larger capacity. A buffer underflow can happen only after an unwrap, so the buffer will always be a
     * peerNetData buffer.
     *
     * @param buffer - will always be peerNetData buffer.
     * @param engine - the engine used for encryption/decryption of the data exchanged between the two peers.
     * @return The same buffer if there is no space problem or a new buffer with the same data but more space.
     * @throws Exception
     */
    protected fun handleBufferUnderflow(engine: SSLEngine, buffer: ByteBuffer): ByteBuffer {
        return if (engine.session.packetBufferSize < buffer.limit()) {
            buffer
        } else {
            val replaceBuffer = enlargePacketBuffer(engine, buffer)
            buffer.flip()
            replaceBuffer.put(buffer)
            replaceBuffer
        }
    }

    /**
     * This method should be called when this peer wants to explicitly close the connection
     * or when a close message has arrived from the other peer, in order to provide an orderly shutdown.
     *
     *
     * It first calls [SSLEngine.closeOutbound] which prepares this peer to send its own close message and
     * sets [SSLEngine] to the `NEED_WRAP` state. Then, it delegates the exchange of close messages
     * to the handshake method and finally, it closes socket channel.
     *
     * @param socketChannel - the transport link used between the two peers.
     * @param engine - the engine used for encryption/decryption of the data exchanged between the two peers.
     * @throws IOException if an I/O error occurs to the socket channel.
     */
    @Throws(IOException::class)
    protected fun closeConnection(socketChannel: SocketChannel, engine: SSLEngine) {
        engine.closeOutbound()
        doHandshake(socketChannel, engine, null)
        socketChannel.close()
    }
    @Throws(IOException::class)
    protected fun closeConnection(inData: DataInputStream, outSteam: OutputStream, engine: SSLEngine) {
        engine.closeOutbound()
        doHandshake(inData, outSteam, engine, null)
        inData.close()
        outSteam.close()
    }

    /**
     * In addition to orderly shutdowns, an unorderly shutdown may occur, when the transport link (socket channel)
     * is severed before close messages are exchanged. This may happen by getting an -1 or [IOException]
     * when trying to read from the socket channel, or an [IOException] when trying to write to it.
     * In both cases [SSLEngine.closeInbound] should be called and then try to follow the standard procedure.
     *
     * @param socketChannel - the transport link used between the two peers.
     * @param engine - the engine used for encryption/decryption of the data exchanged between the two peers.
     * @throws IOException if an I/O error occurs to the socket channel.
     */
    @Throws(IOException::class)
    protected fun handleEndOfStream(socketChannel: SocketChannel, engine: SSLEngine) {
        try {
            engine.closeInbound()
        } catch (e: Exception) {
            Log.e(TAG,"This engine was forced to close inbound, without having received the proper SSL/TLS close notification message from the peer, due to end of stream.")
        }

        closeConnection(socketChannel, engine)
    }
    @Throws(IOException::class)
    protected fun handleEndOfStream(inData: DataInputStream, outSteam: OutputStream, engine: SSLEngine) {
        try {
            engine.closeInbound()
        } catch (e: Exception) {
            Log.e(TAG,"This engine was forced to close inbound, without having received the proper SSL/TLS close notification message from the peer, due to end of stream.")
        }

        closeConnection(inData, outSteam, engine)
    }
    /**
     * Additionally shut down executor
     */
    override fun exit() {
        super.exit()
        executor.shutdown()
    }

}