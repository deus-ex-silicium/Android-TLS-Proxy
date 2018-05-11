package com.nibiru.evilap.proxy

import android.util.Log
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

            val reqFile = readRequestedFile(inData) ?: return
            when(reqFile) {
                "/" -> {
                    outStream.write(SharedClass.INSTANCE.indexFile)
                }
                else -> {
                    outStream.write(SharedClass.INSTANCE.notFoundFile)
                }
            }
            outStream.flush()
            Log.d(TAG, "[SEND]")
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

    private fun readRequestedFile(inR: BufferedReader): String?{
        val requestLine = inR.readLine() ?: return null
        Log.d("$TAG[REQUEST LINE]", requestLine)
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