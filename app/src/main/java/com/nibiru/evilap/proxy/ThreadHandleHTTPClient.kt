package com.nibiru.evilap.proxy

import android.util.Log
import okhttp3.*
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.io.OutputStream
import java.net.Socket
import java.net.SocketTimeoutException

internal class ThreadHandleHTTPClient(private val sClient: Socket) : Runnable {
    private val TAG = javaClass.simpleName
    private val DEBUG = false

    override fun run() {
        //things we will eventually close
        var inFromClient: BufferedReader? = null
        var outToClient: OutputStream? = null
        var res: Response? = null
        try {
            inFromClient = BufferedReader(InputStreamReader(sClient.getInputStream()))
            outToClient = sClient.getOutputStream()

            //HTTP KEEP ALIVE LOOP!
            while (true) {
                //get client request string as okhttp request
                val req = getOkhttpRequest(inFromClient) ?: return
                //make request and get okhttp response
                res = SharedClass.INSTANCE.httpClient.newCall(req).execute()

                /*
                //get and send response headers
                val headers = getResponseHeaders(res)
                //send headers
                sendBytes(headers.toByteArray(), outToClient)
                //get and send response bytes
                val bytesBody = res.body()?.bytes()
                sendBytes(bytesBody!!, outToClient)
                if (DEBUG) Log.d("$TAG[OUT]", headers)
                outToClient.flush()
                */
            }
        } catch (e: IOException) {
            if (e is SocketTimeoutException)
                Log.e(TAG, "TIMEOUT!")
            else
                e.printStackTrace()
        } finally {
            //clean up
            if (res != null) res.body()?.close()
            try {
                if (outToClient != null) outToClient.close()
                if (inFromClient != null) inFromClient.close()
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
    private fun getOkhttpRequest(inR: BufferedReader): Request? {
        val builder = Request.Builder()
        val requestLine = inR.readLine()

        return builder.build()
    }
}