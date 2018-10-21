package com.nibiru.evilap.proxy

import android.util.Log
import com.nibiru.evilap.EvilApApp
import okhttp3.*
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.IOException
import java.net.InetSocketAddress
import java.net.StandardSocketOptions
import java.nio.ByteBuffer
import java.nio.channels.SelectionKey
import java.nio.channels.Selector
import java.nio.channels.ServerSocketChannel
import java.nio.channels.SocketChannel
import java.nio.channels.spi.SelectorProvider
import javax.net.ssl.SSLEngine

abstract class ThreadNioBase(val hostAddress: String, val port: Int) : Runnable{
    /**************************************CLASS FIELDS********************************************/
    private val TAG = javaClass.simpleName
    private val DEBUG = true
    /**
     * Declares if the server is active to serve and create new connections.
     */
    private var active: Boolean = false
    /**
     * A part of Java NIO that will be used to serve all connections to the server in one thread.
     */
    protected var selector: Selector = SelectorProvider.provider().openSelector()
    protected abstract var peerAppData: ByteBuffer
    protected abstract var myAppData: ByteBuffer
    /**************************************CLASS METHODS*******************************************/
    /**
     * @param hostAddress - the IP address this server will listen to.
     * @param port - the port this server will listen to.
     */
    init{
        active = true
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
                        read(key.channel() as SocketChannel, key.attachment() as SSLEngine?)
                    }
                } catch (e: IOException){
                    key.cancel()
                    Log.e(TAG, "(${e.message}) IO Exception while communicating with peer...")
                }
            }
        }
        Log.e(TAG,"Goodbye!")
    }

    abstract fun accept(key: SelectionKey)
    abstract fun read(socketChannel: SocketChannel, engine: SSLEngine?=null)
    abstract fun write(socketChannel: SocketChannel, message: ByteArray, engine: SSLEngine?=null)

    /**
     * Sets the server to an inactive state, in order to exit the reading loop in [this.run]
     * and also wakes up the selector, which may be in select() blocking state.
     */
    open fun exit() {
        Log.e(TAG,"Will now close server...")
        active = false
        selector.wakeup()
    }

    @Throws(IOException::class)
    protected fun getOkhttpRequest(inD: DataInputStream, protocol: String): Request? {
        val builder = Request.Builder()
        val requestLine = inD.readLine() ?: return null
        if (DEBUG) Log.d("$TAG[REQUEST LINE]", requestLine)
        val requestLineValues = requestLine.split("\\s+".toRegex())

        var host: String? = null
        var mime: String? = null
        var len = 0

        var line: String
        loop@ while(true){
            line = inD.readLine() ?: return null
            if(DEBUG) Log.d("$TAG[LINE]", line)
            val header = line.split(": ")
            when(header[0]) {
                "" -> break@loop
                "Host" -> host = header[1]
                "Content-Type" -> mime = header[1]
                "Content-Length" -> len = header[1].toInt()
                //"Connection" -> { val keepAlive = header[1] != "close" }
                else -> builder.addHeader(header[0], header[1])
            }
        }
        Log.i(TAG, "===== host: $host, $requestLineValues =====")
        // https://www.w3.org/Protocols/rfc2616/rfc2616-sec5.html
        val url =  if(host!! in requestLineValues[1])
            requestLineValues[1]
        else
            "$protocol://$host${requestLineValues[1]}"
        if (DEBUG) Log.d(TAG, url)
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

    protected fun getResponseHeaders(res: Response): ByteArray {
        //val version = res.protocol().toString().toUpperCase().toByteArray()
        //TODO: !!!!
        val version = "HTTP/1.1".toByteArray()
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

        return outClient.toByteArray()
    }

    protected fun serveResponse(socketChannel: SocketChannel, inData: DataInputStream, engine: SSLEngine?=null){
        val req = if (engine != null)
            getOkhttpRequest(inData, "https")
        else
            getOkhttpRequest(inData, "http")

        if (req == null) {
            Log.e(TAG, "Cannot read request, closing")
            return
        }

        EvilApApp.instance.httpClient.newCall(req).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) { e.printStackTrace() }
            override fun onResponse(call: Call, response: Response) {
                try {
                    val resHeaders = getResponseHeaders(response)
                    write(socketChannel, resHeaders, engine)
                    response.body()?.apply { write(socketChannel, this.bytes(), engine) }
                }
                catch (e: IOException){
                    Log.e(TAG, "Whops!!!")
                    e.printStackTrace()
                }
            }
        })
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