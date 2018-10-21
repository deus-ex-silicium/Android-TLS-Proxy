package com.nibiru.evilap.proxy

import android.util.Log
import java.nio.channels.SelectionKey
import java.nio.channels.ServerSocketChannel
import java.nio.channels.SocketChannel
import java.io.*
import java.nio.ByteBuffer
import javax.net.ssl.SSLEngine


class ThreadNioHTTP(hostAddress: String, port: Int) : ThreadNioBase(hostAddress, port) {
    /**************************************CLASS FIELDS********************************************/
    private val TAG = javaClass.simpleName
    private val DEBUG = false
    override var myAppData: ByteBuffer = ByteBuffer.allocate(0x1000)
    override var peerAppData: ByteBuffer = ByteBuffer.allocate(0x10000)

    /**
     * Will be called after a new connection request arrives to the server.
     * Creates the [SocketChannel] that will be used as the network layer link
     *
     * @param key - the key dedicated to the [ServerSocketChannel] used by the server to listen to new connection requests.
     * @throws Exception
     */
    @Throws(Exception::class)
    override fun accept(key: SelectionKey) {

        Log.e(TAG,"New connection request!")

        val socketChannel = (key.channel() as ServerSocketChannel).accept()
        socketChannel.configureBlocking(false)
        socketChannel.register(selector, SelectionKey.OP_READ)
    }

    @Throws(IOException::class)
    override fun read(socketChannel: SocketChannel, engine: SSLEngine?) {
        peerAppData.clear()
        val bytesRead = socketChannel.read(peerAppData)
        if (bytesRead < 0 ) {
            Log.e(TAG,"Received end of stream. Will try to close connection with client...")
            socketChannel.close()
            Log.e(TAG,"Goodbye client!")
            return
        }
        // WARNING: the request could be segmented
        // in multiple read calls if it's greater then peerAppData.capacity()
        peerAppData.flip()
        if (DEBUG) Log.d(TAG,"[IN]:\n${String(peerAppData.array())}")
        val inData = DataInputStream(ByteArrayInputStream(peerAppData.array()))
        serveResponse(socketChannel, inData)
    }

    @Throws(IOException::class)
    override fun write(socketChannel: SocketChannel, message: ByteArray, engine: SSLEngine?) {
        myAppData.clear()
        var current = 0
        val end = message.size - 1
        val step = myAppData.limit() - 1

        while (current < end){
            val next = if(end-current > step) current + step else end
            Log.i(TAG, "current:$current, next:$next, end:$end")
            val slice = message.sliceArray(IntRange(current, next))
            if (current != 0) myAppData.flip()
            myAppData.put(slice)
            myAppData.flip()
            socketChannel.write(myAppData)
            current = next + 1
            if (DEBUG) Log.d(TAG,"[SENT] ${String(slice)}")
        }
    }

    private fun serveStaticResponse(socketChannel: SocketChannel){
        var out = "HTTP/1.1 200 OK\r\nContent-Length: 78\r\n\r\n"
        out += "=== HTTP PROXY ===\n"
        out += "Connection was accepted...\n"
        out += "And static response was served!\n"
        write(socketChannel, out.toByteArray())
    }


}