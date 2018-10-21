package com.nibiru.evilap.proxy

import android.util.Log
import com.nibiru.evilap.EvilApApp
import okhttp3.*
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.SelectionKey
import java.nio.channels.Selector
import java.nio.channels.ServerSocketChannel
import java.nio.channels.SocketChannel
import java.nio.channels.spi.SelectorProvider
import java.io.*
import java.net.StandardSocketOptions


class ThreadNioHTTP(val hostAddress: String, val port: Int) : Runnable, ClientHandlerBase() {
    /**************************************CLASS FIELDS********************************************/
    private val TAG = javaClass.simpleName
    private val DEBUG = false
    /**
     * Declares if the server is active to serve and create new connections.
     */
    private var active: Boolean = false
    /**
     * A part of Java NIO that will be used to serve all connections to the server in one thread.
     */
    private var selector: Selector

    protected var peerData: ByteBuffer
    protected var myData: ByteBuffer

    /**************************************CLASS METHODS*******************************************/
    /**
     *
     * @param hostAddress - the IP address this server will listen to.
     * @param port - the port this server will listen to.
     */
    init{
        selector = SelectorProvider.provider().openSelector()
        active = true
        myData = ByteBuffer.allocate(0x1000)
        peerData = ByteBuffer.allocate(0x10000)
    }
    /**
     * Should be called in order the server to start listening to new connections.
     * This method will run in a loop as long as the server is active. In order to exit the server
     * you should use [this.stop] which will set it to inactive state
     * and also wake up the listener, which may be in blocking select() state.
     *
     * @throws Exception
     */
    @Throws(Exception::class)
    override fun run() {
        val serverSocketChannel = ServerSocketChannel.open()
        serverSocketChannel.configureBlocking(false)
        serverSocketChannel.setOption(StandardSocketOptions.SO_REUSEADDR, true)

        try { serverSocketChannel.socket().bind(InetSocketAddress(hostAddress, port)) }
        catch(e: IOException){exit()}

        serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT)

        Log.e(TAG, "Listening on port: $port")

        while (isActive()) {
            selector.select()
            val selectedKeys = selector.selectedKeys().iterator()
            while (selectedKeys.hasNext()) {
                val key = selectedKeys.next()
                selectedKeys.remove()
                if (!key.isValid) {
                    continue
                }
                try {
                    if (key.isAcceptable) {
                        accept(key)
                    } else if (key.isReadable) {
                        read(key.channel() as SocketChannel)
                    }
                } catch (e: IOException){
                    key.cancel()
                    Log.e(TAG, "(${e.message}) IO Exception while communicating with peer...")
                }
            }
        }

        Log.e(TAG,"Goodbye!")
    }

    /**
     * Sets the server to an inactive state, in order to exit the reading loop in [this.run]
     * and also wakes up the selector, which may be in select() blocking state.
     */
    fun exit() {
        Log.e(TAG,"Will now close server...")
        active = false
        selector.wakeup()
    }

    /**
     * Will be called after a new connection request arrives to the server.
     * Creates the [SocketChannel] that will be used as the network layer link
     *
     * @param key - the key dedicated to the [ServerSocketChannel] used by the server to listen to new connection requests.
     * @throws Exception
     */
    @Throws(Exception::class)
    private fun accept(key: SelectionKey) {

        Log.e(TAG,"New connection request!")

        val socketChannel = (key.channel() as ServerSocketChannel).accept()
        socketChannel.configureBlocking(false)
        socketChannel.register(selector, SelectionKey.OP_READ)
    }


    @Throws(IOException::class)
    fun read(socketChannel: SocketChannel) {
        peerData.clear()
        val bytesRead = socketChannel.read(peerData)
        if (bytesRead < 0 ) {
            Log.e(TAG,"Received end of stream. Will try to close connection with client...")
            socketChannel.close()
            Log.e(TAG,"Goodbye client!")
            return
        }
        // WARNING: the request could be segmented
        // in multiple read calls if it's greater then peerData.capacity()
        peerData.flip()
        if (DEBUG) Log.d(TAG,"[IN]:\n${String(peerData.array())}")
        val inData = DataInputStream(ByteArrayInputStream(peerData.array()))
        serveResponse(socketChannel, inData)
    }

    @Throws(IOException::class)
    fun write(socketChannel: SocketChannel, message: ByteArray) {
        myData.clear()
        var current = 0
        val end = message.size - 1
        val step = myData.limit() - 1

        while (current < end){
            val next = if(end-current > step) current + step else end
            Log.i(TAG, "current:$current, next:$next, end:$end")
            val slice = message.sliceArray(IntRange(current, next))
            if (current != 0) myData.flip()
            myData.put(slice)
            myData.flip()
            socketChannel.write(myData)
            current = next + 1
            if (DEBUG) Log.d(TAG,"[SENT] ${String(slice)}")
        }
    }

    private fun serveResponse(socketChannel: SocketChannel, inData: DataInputStream){
        val req = getOkhttpRequest(inData, "http")
        if (req == null) {
            Log.e(TAG, "Cannot read request, closing")
            return
        }
        EvilApApp.instance.httpClient.newCall(req).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) { e.printStackTrace() }
            override fun onResponse(call: Call, response: Response) {
                try {
                    val resHeaders = getResponseHeaders(response)
                    write(socketChannel, resHeaders)
                    response.body()?.apply { write(socketChannel, this.bytes()) }
                }
                catch (e: IOException){
                    Log.e(TAG, "Whops!")
                    e.printStackTrace()
                }
            }
        })
    }


    private fun serveStaticResponse(socketChannel: SocketChannel){
        var out = "HTTP/1.1 200 OK\r\nContent-Length: 78\r\n\r\n"
        out += "=== HTTP PROXY ===\n"
        out += "Connection was accepted...\n"
        out += "And static response was served!\n"
        write(socketChannel, out.toByteArray())
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