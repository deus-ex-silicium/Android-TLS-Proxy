package com.nibiru.evil_ap.proxy;

import android.util.Log;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;

import javax.net.ssl.SSLProtocolException;

/**
 * Created by Nibiru on 2016-10-14.
 */

class ThreadRevEcho implements Runnable {
    /**************************************CLASS FIELDS********************************************/
    protected final String TAG = getClass().getSimpleName();
    private Socket client = null;
    /**************************************CLASS METHODS*******************************************/
    ThreadRevEcho(Socket socket) {
        super();
        this.client = socket;
    }

    /**
     * Debug class that handles clients by echoing the request they are making
     */
    public void run() {
        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()));
            BufferedWriter out = new BufferedWriter(new OutputStreamWriter(client.getOutputStream()));

            String request = "";
            String line;
            while ((line = in.readLine()) != null) {
                Log.d(TAG + "[IN]", line );
                request += line + '\n';
                if (line.isEmpty()) {
                    break;
                }
            }
            Log.d(TAG, "<==================Sending response==================>");
            out.write(request);
            Log.d(TAG, "<==================Closing connection==================>");
            out.close();
            in.close();
            client.close();
        } catch (IOException e) {
            Log.d(TAG, "<================ SHIT FUCK, EXCEPTION !!! ================>");
            if (e instanceof SSLProtocolException){
                Log.d(TAG, "ERROR: client doesn't like our self signed cert");
            }
            else e.printStackTrace();
        }
    }
}