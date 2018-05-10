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
        var inData: DataInputStream?= null
        var outStream: OutputStream? = null
        var res: Response? = null
        try {
            inData = DataInputStream(sClient.getInputStream())
            outStream = sClient.getOutputStream()
            //outStream = ByteArrayOutputStream()

            //HTTP KEEP ALIVE LOOP!
            while (keepAlive) {
                //get client request string as okhttp request
                val req = getOkhttpRequest(inData) ?: return
                res = SharedClass.INSTANCE.httpClient.newCall(req).execute()

                sendResponseHeaders(res, outStream)
                res.body()?.apply { outStream.write(this.bytes()) }
                outStream.flush()
                Log.d(TAG, "[SEND]")
            }
        } catch (e: IOException) {
            if (e is SocketTimeoutException)
                Log.e(TAG, "TIMEOUT!")
            else
                e.printStackTrace()
        } finally {
            try { //clean up
                Log.e(TAG, "Cleaning client resources")
                if (res != null) res.body()?.close()
                if (outStream != null) outStream.close()
                if (inData != null) inData.close()
                sClient.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    // "In practice, most HTTP header field values use only a subset of the
    // US-ASCII charset [USASCII]. Newly defined header fields SHOULD limit
    // their field values to US-ASCII octets.  A recipient SHOULD treat other
    // octets in field content (obs-text) as opaque data."
    //  - https://tools.ietf.org/html/rfc7230
    // So using DataInputSteam.readLine() should work well since multibyte
    // character sets are not to be expected
    // see https://stackoverflow.com/questions/21754078/datainputstream-readline-deprecated
    @Throws(IOException::class)
    private fun getOkhttpRequest(inD: DataInputStream): Request? {
        val builder = Request.Builder()
        var offset = 0
        val requestLine = inD.readLine() ?: return null
        if(DEBUG) Log.d("$TAG[LINE]", requestLine)
        offset += requestLine.length
        val requestLineValues = requestLine.split("\\s+".toRegex())

        var host: String? = null
        var mime: String? = null
        var len = 0

        var line: String
        loop@ while(true){
            line = inD.readLine()
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