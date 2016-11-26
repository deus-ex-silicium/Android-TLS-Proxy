package com.nibiru.evil_ap.proxy;

import android.preference.PreferenceActivity;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.zip.GZIPOutputStream;

import javax.net.ssl.SSLProtocolException;

import okhttp3.Headers;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.internal.framed.Header;
import okio.ByteString;

/**
 * Created by Nibiru on 2016-11-15.
 */

public class RequestTransparent implements Runnable{
/**************************************CLASS FIELDS********************************************/
    private final static String TAG = "RequestTransparent";
    private Socket client = null;
    private OkHttpParser rp;
    private OkHttpClient okhttp;
/**************************************CLASS METHODS*******************************************/
    public RequestTransparent(Socket socket){
        super();
        this.client = socket;
        rp = new OkHttpParser();
        okhttp = new OkHttpClient();
    }

    public void run() {
        try {
            //set timeout on persistent connection
            this.client.setSoTimeout(5000);
            BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()));
            BufferedWriter out = new BufferedWriter(new OutputStreamWriter(client.getOutputStream()));
            OutputStream outIMG = client.getOutputStream();

            //read request string from client
            String request = getRequestString(in);
            if (request == null) { in.close(); out.close(); outIMG.close(); client.close(); return;}
            //parse string request (remove some security headers)
            Request req = rp.parse(request);
            //make request and get response
            Response res = okhttp.newCall(req).execute();
            String resToClient = "";
            Log.d(TAG, "<==================Sending response==================>");
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

            if (client.isConnected()) outIMG.write(resToClient.getBytes());
            //attach response
            //resToClient += res.body().string();
            if (client.isConnected()) res.body().byteStream();
            String body = "<head></head><body>Poprawny html</body>";

            //compress body string
            resToClient +=

            //byte[] bodyBytes = res.body().bytes();
            //byte[] destination = new byte[resToClient.getBytes().length + bodyBytes.length];
            //copy headers into start of destination (from pos 0, copy headers.length bytes)
            //System.arraycopy(resToClient.getBytes(),0,destination,0,resToClient.getBytes().length);
            //copy body into end of destination (from pos headers.length, copy body.length bytes)
            //System.arraycopy(bodyBytes,0,destination,resToClient.getBytes().length,bodyBytes.length);

            Log.d(TAG, "<==================Closing connection==================>");
            out.close();
            outIMG.close();
            in.close();
            client.close();
        } catch (IOException e) {
            Log.d(TAG, "<================ SHIT FUCK, EXCEPTION !!! ================>");
            if (e instanceof SSLProtocolException){
                Log.d(TAG, "ERROR: client doesn't like our self signed cert");
            }
            else if (e instanceof SocketTimeoutException){
                Log.e(TAG, "ERROR: socket timed out!");
            }
            else e.printStackTrace();
            //make sure conn is closed
            if (client != null) try {
                client.close();
            } catch (IOException e1) {
                Log.e(TAG, "exception in exception...");
                e1.printStackTrace();
            }
        }
    }

    private byte[] compress(String str) throws Exception {
        if (str == null || str.length() == 0) {
            return null;
        }
        System.out.println("String length : " + str.length());
        ByteArrayOutputStream obj = new ByteArrayOutputStream();
        GZIPOutputStream gzip = new GZIPOutputStream(obj);
        gzip.write(str.getBytes("UTF-8"));
        gzip.close();
        String outStr = obj.toString("UTF-8");
        System.out.println("Output String length : " + outStr.length());
        return obj.toByteArray();
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
                Log.d(TAG + "[IN]", "Accept-Encoding: identity");
                request += "Accept-Encoding: identity\r\n";
            }
            else {
                Log.d(TAG + "[IN]", line);
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
