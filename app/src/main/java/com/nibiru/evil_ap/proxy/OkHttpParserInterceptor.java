package com.nibiru.evil_ap.proxy;

import android.support.v4.util.Pair;
import android.util.Log;

import com.nibiru.evil_ap.SharedClass;
import com.nibiru.evil_ap.log.Client;

import java.io.BufferedReader;
import java.io.StringReader;
import java.util.Vector;

import okhttp3.Request;

/**
 * Created by Nibiru on 2016-11-03.
 */

class OkHttpParserInterceptor {
    /**************************************CLASS FIELDS********************************************/
    protected final String TAG = getClass().getSimpleName();
    private Vector<Pair<String,String>> requestHeaders;
    private String requestLine;
    private StringBuffer messageBody;
    /**************************************CLASS METHODS*******************************************/
    OkHttpParserInterceptor(){
        requestHeaders = new Vector<>();
        messageBody = new StringBuffer();
    }
    Request parse(String request, SharedClass shrObj, Client c){
        BufferedReader reader = new BufferedReader(new StringReader(request));
        try {
            //read request line
            requestLine = reader.readLine(); // Request-Line ; Section 5.1
            //read headers
            String header = reader.readLine();
            while (header != null && header.length() > 0) {
                appendHeaderParameter(header);
                header = reader.readLine();
            }
            //read body
            String bodyLine = reader.readLine();
            while (bodyLine != null) {
                appendMessageBody(bodyLine);
                bodyLine = reader.readLine();
            }
            return buildOkHTTPRequest(requestLine, shrObj, c);

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

    private Request buildOkHTTPRequest(String requestLine, SharedClass shrObj, Client c)
            throws NullPointerException{
        String[] requestLineValues = requestLine.split("\\s+");
        String url;
        Request.Builder builder = new Request.Builder();
        for(Pair<String, String> header : requestHeaders){
            if (header.first.equals("Host") || header.first.equals("host")){
                String host = header.second.trim();
                url = "http://" + host + requestLineValues[1];
                Log.d(TAG, url);
                builder.url(url);
                //TODO: testing logging
                //shrObj.addRequest(c, host, requestLine);
            }
            else builder.addHeader(header.first, header.second);
        }
        return builder.build();
    }
}
