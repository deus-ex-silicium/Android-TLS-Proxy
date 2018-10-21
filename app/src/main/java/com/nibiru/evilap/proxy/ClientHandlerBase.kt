package com.nibiru.evilap.proxy

import android.util.Log
import com.nibiru.evilap.EvilApApp
import okhttp3.*
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.IOException
import java.nio.channels.SocketChannel
import javax.net.ssl.SSLEngine

open class ClientHandlerBase{
    /**************************************CLASS FIELDS********************************************/
    private val TAG = javaClass.simpleName
    private val DEBUG = false
    /**************************************CLASS METHODS*******************************************/
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
                "Connection" -> { val keepAlive = header[1] != "close" }
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

        return outClient.toByteArray()
    }

    private fun serveResponse(socketChannel: SocketChannel, inData: DataInputStream, engine: SSLEngine?=null){

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
                    //write(socketChannel, engine, resHeaders)
                    //response.body()?.apply { write(socketChannel, engine, this.bytes()) }
                }
                catch (e: IOException){
                    Log.e(TAG, "Whops!!!")
                    e.printStackTrace()
                }
            }
        })
    }


}