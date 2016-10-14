package com.nibiru.evil_ap.Proxy;

import android.util.Log;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;

/**
 * Created by Nibiru on 2016-10-14.
 */

public class ProxyRunnable implements Runnable {
    final static String TAG = "ProxyRunnable";
    private Socket client = null;
    /*********************************************************************************************/
    public ProxyRunnable(Socket socket) {
        super();
        this.client = socket;
        Log.d(TAG, "<==================Accepted client==================>");
    }

    public void run() {
        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()));
            BufferedWriter out = new BufferedWriter(new OutputStreamWriter(client.getOutputStream()));

            String request = "";
            String line;
            while ((line = in.readLine()) != null) {
                Log.d(TAG+"[IN]", line );
                request += line + '\n';
                if (line.isEmpty()) {
                    break;
                }
            }
            Log.d(TAG, "<==================Sending response==================>");
            out.write(request);
        } catch (IOException e) {
            Log.d(TAG, "<================ SHIT FUCK, EXCEPTION !!! ================>");
            e.printStackTrace();
        }
    }


}