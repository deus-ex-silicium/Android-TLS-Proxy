package com.nibiru.evilap.proxy

import android.util.Log
import okhttp3.*
import java.io.*
import java.net.Socket
import java.net.SocketTimeoutException

internal class ThreadHandleHTTPClient(private val sClient: Socket) : Runnable {
    private val TAG = javaClass.simpleName
    private val DEBUG = false

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

            //HTTP KEEP ALIVE LOOP!
            while (true) {
                //get client request string as okhttp request
                val req = makeOkhttpRequest(inReader, inStream) ?: return
                //make request and get okhttp response
                res = SharedClass.INSTANCE.httpClient.newCall(req).execute()

                /*
                //get and send response headers
                val headers = getResponseHeaders(res)
                //send headers
                sendBytes(headers.toByteArray(), outStream)
                //get and send response bytes
                val bytesBody = res.body()?.bytes()
                sendBytes(bytesBody!!, outStream)
                if (DEBUG) Log.d("$TAG[OUT]", headers)
                outStream.flush()
                */
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
    private fun sendBytes(msg: ByteArray, out: OutputStream) {
        out.write(msg, 0, msg.size)
        if (DEBUG) Log.d("$TAG[OUT]", "sent chunk!")
    }

    @Throws(IOException::class)
    private fun makeOkhttpRequest(inR: BufferedReader, inS: InputStream): Request? {
        val builder = Request.Builder()
        var offset = 0
        val requestLine = inR.readLine()
        offset += requestLine.length
        val requestLineValues = requestLine.split("\\s+".toRegex())

        var host: String? = null
        var mime: String? = null
        var len: Int = 0

        inR.useLines {
            it.map { line ->
                Log.d("LINE", line)
                val header = line.split(": ")
                when(header[0]) {
                    "" -> return@map
                    "Host" -> host = header[1]
                    "Content-Type" -> mime = header[1]
                    "Content-Length" -> len = header[1].toInt()
                    else -> builder.addHeader(header[0], header[1])
                }
            }
        }
        val url = "http://$host${requestLineValues[1]}"
        Log.d(TAG, url)
        builder.url(url)

        var reqBody: RequestBody? = null
        // if there is Message body, read it
        if(len > 0){
            val buf = inR.readText()
            reqBody = RequestBody.create(MediaType.parse(mime), buf)
        }
        builder.method(requestLineValues[0], reqBody)
        return builder.build()
    }
}