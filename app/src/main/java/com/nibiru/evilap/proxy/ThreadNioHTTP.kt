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


class ThreadNioHTTP(val hostAddress: String, val port: Int) : Runnable{
    /**************************************CLASS FIELDS********************************************/
    private val TAG = javaClass.simpleName
    /**
     * Declares if the server is active to serve and create new connections.
     */
    private var active: Boolean = false
    /**
     * A part of Java NIO that will be used to serve all connections to the server in one thread.
     */
    private var selector: Selector

    protected lateinit var peerData: ByteBuffer
    protected lateinit var myData: ByteBuffer

    private val DEBUG = false
    /**************************************CLASS METHODS*******************************************/
    /**
     *
     * @param hostAddress - the IP address this server will listen to.
     * @param port - the port this server will listen to.
     */
    init{
        selector = SelectorProvider.provider().openSelector()
        active = true
        myData = ByteBuffer.allocate(256)
        peerData = ByteBuffer.allocate(256)
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
        }
        peerData.flip()
        //Log.d(TAG,"[IN]:\n${String(peerData.array())}")

        val inData = DataInputStream(ByteArrayInputStream(peerData.array()))
        val req = getOkhttpRequest(inData)
        if (req == null) {
            Log.e(TAG, "Cannot read request, closing")
            return
        }

        //TODO: async call, what if socketChannel changes in the meantime?
        EvilApApp.instance.httpClient.newCall(req).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) { e.printStackTrace() }
            @Throws(IOException::class)
            override fun onResponse(call: Call, response: Response) {
                sendResponseHeaders(response, socketChannel)
                response.body()?.apply { write(socketChannel, this.bytes())}
            }
        })

        /*val out = "HTTP/1.1 200 OK\r\nContent-Length: 18\r\n\r\nHTTP  Proxy Hello!"
        write(socketChannel, out.toByteArray())*/

    }

    @Throws(IOException::class)
    fun write(socketChannel: SocketChannel, message: ByteArray) {

        if(message.size > myData.capacity()){
            myData = ByteBuffer.allocate(message.size)
        }
        myData.clear()
        myData.put(message)
        myData.flip()
        socketChannel.write(myData)
        Log.d(TAG,"[SENT]")
        //Log.d(TAG,"[OUT]:\n${message.toString()}")

    }

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
                "Connection" -> { val keepAlive = header[1] != "close" }
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

    private fun sendResponseHeaders(res: Response, socketChannel: SocketChannel){
        val version = res.protocol().toString().toUpperCase().toByteArray()
        val code = res.code().toString().toByteArray()
        val msg = res.message().toByteArray()
        val headers = res.headers().toString().replace("\n","\r\n").toByteArray()

        val outClient = ByteArrayOutputStream(version.size + 1 + code.size + 1 + msg.size + 2 + headers.size + 2)
        outClient.write(version)
        outClient.write(0x20) // space
        outClient.write(code)
        outClient.write(0x20) // space
        outClient.write(msg)
        outClient.write(byteArrayOf(0x0d, 0x0a)) //CRLF
        outClient.write(headers)
        outClient.write(byteArrayOf(0x0d, 0x0a)) //CRLF

        write(socketChannel, outClient.toByteArray())
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