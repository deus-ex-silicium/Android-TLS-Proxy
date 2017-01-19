package com.nibiru.evil_ap.proxy;

import android.content.SharedPreferences;
import android.util.Log;

import com.nibiru.evil_ap.SharedClass;
import com.nibiru.evil_ap.log.Client;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
        c = mSharedObj.getClientByIp(sClient.getInetAddress().toString().substring(1));
    }
    //scalable socket programming (2014)
    //http://www.javaworld.com/article/2853780/core-java/socket-programming-for-scalable-systems.html

    /**
     * UNDER-DEVELOPMENT class that uses raw socket programming and no library dependencies to
     * handle client requests, difficulties include HTTP chunked trasfer encoding/decoding.
     */
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
            String headers = readHeaders2(inFromServer);

            byte[] bodyBytes;
            if (headers.toLowerCase().contains("transfer-encoding: chunked")) {
                byte[] chunkBytes;
                ByteArrayOutputStream body = new ByteArrayOutputStream();
                while ( (chunkBytes = readChunk(inFromServer)) != null) {
                    body.write(chunkBytes);
                }
                bodyBytes = body.toByteArray();
                headers = replaceTransferEncodingWithContentLength(headers, bodyBytes.length);
            }
            else {
                bodyBytes = readStream(inFromServer);
            }
            //send response
            send(headers.getBytes(), outToClient);
            send(bodyBytes, outToClient);

            //HTTP KEEP ALIVE
            while (work) {
                //get client request string
                sRequest = getRequestString(inFromClient);
                //check if connection is keep-alive and update work
                work = sRequest.toLowerCase().contains("keep-alive");
                //send request
                outToServer.print(sRequest);
                //read headers
                headers = readHeaders2(inFromServer);

                if (headers.toLowerCase().contains("transfer-encoding: chunked")) {
                    byte[] chunkBytes;
                    ByteArrayOutputStream body = new ByteArrayOutputStream();
                    while ( (chunkBytes = readChunk(inFromServer)) != null) {
                        body.write(chunkBytes);
                    }
                    bodyBytes = body.toByteArray();
                    headers = replaceTransferEncodingWithContentLength(headers, bodyBytes.length);
                }
                else {
                    bodyBytes = readStream(inFromServer);
                }
                //send response
                send(headers.getBytes(), outToClient);
                send(bodyBytes, outToClient);
            }

        } catch (IOException e) {
            if (e instanceof SocketTimeoutException)
                Log.e(TAG, "TIMEOUT!");
            else
                e.printStackTrace();
        } finally {
            //clean up
            try {
                if (inFromServer != null) inFromServer.close();
                if (outToServer != null) outToServer.close();
                if (inFromClient != null) inFromClient.close();
                if (outToClient != null) outToClient.close();
                if (sClient != null) sClient.close();
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private String replaceTransferEncodingWithContentLength(String headers, int length) {
        if (headers.toLowerCase().contains("transfer-encoding: chunked")){
            headers = headers.replaceFirst("(?i)transfer-encoding: chunked",
                    "Content-Length: "+ length);
            //headers = headers.replaceAll("(?i)(?m)^keep-alive:.*(?:\\r?\\n)?", "");
            //headers = headers.replaceAll("(?i)(?m)^connection:.*", "Connection: close");
        }
        return headers;
    }

    private void send(byte[] msg, OutputStream out){
        try {
            out.write(msg, 0, msg.length);
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (debug) Log.d(TAG + "[OUT]", "sent headers!");
    }

    private void sendChunk(byte[] msg, OutputStream out){
        String hex = Integer.toHexString(msg.length);
        byte[] newLine = {13, 10}; // \r \n
        try {
            out.write(hex.getBytes());
            out.write(newLine);
            out.write(msg, 0, msg.length);
            out.write(newLine);
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
        String line = "";
        int x;
        boolean emptyLineReached = false;

        while (!emptyLineReached) {
            while ((x = is.read()) != 10) { // 10 = \n and x != -1 (-1 = end of stream)
                line = line + ((char) x);
                if (line.trim().isEmpty())
                    emptyLineReached = true;
            }
            //finished reading line
            result.append(line).append("\n");
            line = "";
        }
        return result.toString();
    }

    private String readHeaders3(InputStream is) throws IOException{
        //buffer for body bytes
        ByteArrayOutputStream body = new ByteArrayOutputStream();

        // Create a buffer large enough for the response header (and drop exception if it is bigger).
        byte[] headEnd = {13, 10, 13, 10}; // \r \n \r \n
        byte[] buffer = new byte[16384];
        int length = 0;
        // Read bytes to the buffer until you find `\r\n\r\n` in the buffer.
        int nRead, pos;
        while ((pos = arrayIndexOf(buffer, 0, length, headEnd)) == -1 &&
                (nRead = is.read(buffer, length, buffer.length - length)) > -1) {
            length += nRead;
            // buffer is full but have not found end signature
            if (length == buffer.length)
                throw new RuntimeException("Response header too long");
        }
        // pos contains the starting index of the end signature (\r\n\r\n) so we add 4 bytes
        pos += 4;
        // When you encounter the end of header, create a string from the first *n* bytes
        String headers = new String(buffer, 0, pos);

        // Read body bytes
        while ((nRead = is.read(buffer, 0, buffer.length)) != -1) {
            String test = new String(buffer, "UTF-8");
            body.write(buffer, 0, nRead);
        }

        return null;
    }

    private String readHeaders4(InputStream is) throws IOException{
        ByteArrayOutputStream headersBytes = new ByteArrayOutputStream();
        ByteArrayOutputStream bodyBytes = new ByteArrayOutputStream();
        byte[] headEnd = {13, 10, 13, 10}; // \r \n \r \n

        //read headers
        int readByte;
        byte[] tmpBuffer = new byte[4];
        while (true) {
            readByte = is.read();

        }
        //read body
        //int read;
        //while ((read = in.read()) != -1) {
        //    body.write(read);
        //}
        //return null;
    }

    private int arrayIndexOf(byte[] haystack, int offset, int length, byte[] needle) {
        for (int i=offset; i<offset+length-needle.length; i++) {
            boolean match = false;
            for (int j=0; j < needle.length; j++) {
                match = haystack[i + j] == needle[j];
                if (!match)
                    break;
            }
            if (match)
                return i;
        }
        return -1;
    }

    private byte[] readChunk(InputStream is) throws IOException {
        //read line to find number of bytes
        String line = "";
        int len, x;
        while ((x = is.read()) != 10) { // 10 = \n
            line = line + ((char)x);
        }
        line += (char)10;
        len = Integer.parseInt(line.trim(), 16);
        if (len == 0) {
            //consume new line before ending
            while ((x = is.read()) != 10) {}
            return null;
        }
        byte[] result = new byte[len];
        int nRead=0, toRead=len;
        while ( toRead != 0 )  {
            nRead = is.read(result, nRead, toRead);
            toRead -= nRead;
        }
        //consume new line before reading new chunk
        while ((x = is.read()) != 10) {}
        return result;
    }
}
