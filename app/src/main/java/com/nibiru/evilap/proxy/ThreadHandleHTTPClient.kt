package com.nibiru.evilap.proxy

import android.util.Log
import okhttp3.*
import java.io.*
import java.net.Socket
import java.net.SocketTimeoutException


internal class ThreadHandleHTTPClient(private val sClient: Socket) : Runnable {
    private val TAG = javaClass.simpleName
    private var keepAlive = true
    private val DEBUG = true

    override fun run() {
        //things we will eventually close
        var inReader: BufferedReader? = null
        var inStream: InputStream? = null
        var outStream: OutputStream? = null
        var res: Response? = null
        try {
            inStream = sClient.getInputStream()
            inReader = BufferedReader(InputStreamReader(inStream))
            outStream = sClient.getOutputStream()
            //outStream = ByteArrayOutputStream()

            //HTTP KEEP ALIVE LOOP!
            while (keepAlive) {
                //get client request string as okhttp request
                val req = getOkhttpRequest(inReader, inStream) ?: return
                //make request and get okhttp response
                res = SharedClass.INSTANCE.httpClient.newCall(req).execute()

                sendResponseHeaders(res, outStream)
                //val b = res.body()?.bytes()
                res.body()?.apply { outStream.write(this.bytes()) }
                outStream.write("WTF".toByteArray())
                //outStream.write(0x0d)
                outStream.write(0x0a)
                outStream.flush()
                Log.d(TAG, "[SEND]")
            }
        } catch (e: IOException) {
            if (e is SocketTimeoutException)
                Log.e(TAG, "TIMEOUT!")
            else
                e.printStackTrace()
        } finally {
            //clean up
            try {
                if (res != null) res.body()?.close()
                if (inStream != null) inStream.close()
                if (inReader != null) inReader.close()
                if (outStream != null) outStream.close()
                sClient.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    @Throws(IOException::class)
    private fun getOkhttpRequest(inR: BufferedReader, inS: InputStream): Request? {
        val builder = Request.Builder()
        var offset = 0
        val requestLine = inR.readLine()
        if(DEBUG && requestLine != null) Log.d("$TAG[LINE]", requestLine)
        offset += requestLine.length
        val requestLineValues = requestLine.split("\\s+".toRegex())

        var host: String? = null
        var mime: String? = null
        var len = 0

        loop@ for(line in inR.lineSequence().iterator()){
            if(DEBUG) Log.d("$TAG[LINE]", line)
            offset += line.length + 2 //CRLF
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

        // if there is Message body, read it
        var reqBody: RequestBody? = null
        if(len > 0){
            val buf = ByteArray(len)
            val tmp = inS.read(buf, offset, len)
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
}