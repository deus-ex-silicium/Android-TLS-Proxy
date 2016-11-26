package com.nibiru.evil_ap.proxy;

import android.util.Log;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.List;
import java.util.Map;

import fi.iki.elonen.NanoHTTPD;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Created by Nibiru on 2016-11-17.
 */

public class NanoServer extends NanoHTTPD {
    /**************************************CLASS FIELDS********************************************/
    private final static String TAG = "NanoServer";
    private static final int SERVERPORT = 1337;
    /**************************************CLASS METHODS*******************************************/
    public NanoServer() throws IOException {
        super(SERVERPORT);
        start(NanoHTTPD.SOCKET_READ_TIMEOUT, false);
        Log.d(TAG, "NanoServer started!");
    }
    @Override
    public Response serve(IHTTPSession session) {
        OkHttpClient okhttp = new OkHttpClient();
        String req = "";
        String url = "";
        Log.d(TAG, "<==================Got request==================>");
        req += session.getMethod() + " " + session.getUri() + " HTTP/1.1\r\n";
        Request.Builder builder = new Request.Builder();
        for(Map.Entry<String, String> header: session.getHeaders().entrySet()){
            //get url for request
            if (header.getKey().startsWith("Host") || header.getKey().startsWith("host")){
                url = "http://" + header.getValue().trim() + session.getUri();
                builder.url(url);
            }
            //append header to request if we want it
            if (header.getKey().startsWith("accept-encoding")){
                req += header.getKey() + " : " + "identity" + "\r\n";
                builder.addHeader(header.getKey(), "identity");
            }
            else if (!header.getKey().startsWith("http-client-ip") //added by server
                    && !header.getKey().startsWith("remote-addr") //added by server
                    && !header.getKey().startsWith("upgrade-insecure-requests") //security
                    && !header.getKey().startsWith("strict-transport-security") //security
                    && !header.getKey().startsWith("user-agent")){
                req += header.getKey() + " : " + header.getValue() + "\r\n";
                builder.addHeader(header.getKey(), header.getValue());
            }
        }
        Log.d(TAG, req);
        Log.d(TAG, url);
        if (session.getMethod() == Method.POST) return newFixedLengthResponse("");
        Request okHttpReq = builder.build();
        try {
            Response.Status status;
            okhttp3.Response res = okhttp.newCall(okHttpReq).execute();
            String StrResponse = res.body().string();
            //find status line
            switch (res.code()) {
                case 200: status = Response.Status.ACCEPTED; break;
                case 204: status = Response.Status.NO_CONTENT; break;
                case 304: status = Response.Status.NOT_MODIFIED; break;
                case 403: status = Response.Status.FORBIDDEN; break;
                case 404: status = Response.Status.NOT_FOUND; break;
                default: Log.e(TAG, "UNKNOWN STATUS CODE = " + res.code());
                    status = Response.Status.INTERNAL_ERROR;
            }
            //long len = Long.parseLong(res.header("Content-Length"));
            Log.d(TAG, "<==================Sending response==================>");
            //return newFixedLengthResponse(status, res.header("Content-Type"), res.body()
                    //.byteStream(), len);
            Log.d(TAG, StrResponse);
            return newFixedLengthResponse(StrResponse);
        } catch (IOException e) {
            e.printStackTrace();
            return newFixedLengthResponse("Proxy Error");
        }
    }
}
