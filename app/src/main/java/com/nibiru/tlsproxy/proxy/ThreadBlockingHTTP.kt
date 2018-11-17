package com.nibiru.tlsproxy.proxy

import android.util.Log
import com.nibiru.tlsproxy.TLSProxyApp
import okhttp3.*
import java.io.*
import java.net.Socket
import java.net.SocketTimeoutException


internal class ThreadBlockingHTTP(private val sClient: Socket) : Runnable {
    /**************************************CLASS FIELDS********************************************/
    private val TAG = javaClass.simpleName
    private var keepAlive = true
    private val DEBUG = false
    /**************************************CLASS METHODS*******************************************/

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
                val req = getOkhttpRequest(inData)
                if (req == null) {
                    Log.e(TAG, "Cannot read request, closing")
                    return
                }
                res = TLSProxyApp.instance.httpClient.newCall(req).execute()

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

    /*
    private fun getEvilSSLSocket(): ServerSocket {
        //val sock = ssf.createServerSocket() as SSLServerSocket
        //sock.useClientMode = false
        // Use only TLSv1.2 to get access to ExtendedSSLSession
        // https://docs.oracle.com/javase/8/docs/technotes/guides/security/jsse/JSSERefGuide.html#SSLSession
        //val params = sock.sslParameters
        //params.protocols = arrayOf("TLSv1.2")
        //params.sniMatchers = listOf(EvilSniMatcher())
        //sock.sslParameters = params
        //sock.enabledProtocols = arrayOf("TLSv1.2")
        //return sock

    }
    */

}