package com.nibiru.evil_ap.proxy;

import android.util.Log;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

/**
 * Created by Nibiru on 2016-10-30.
 */

class ProxyDNSMainLoop implements Runnable{
    private final static String TAG = "ProxyDNSMainLoop";
    private DatagramSocket serverSocket;
    private static final int SERVERPORT = 1339;
    private Boolean work = true;
    /*********************************************************************************************/
    @Override
    public void run() {
        try {
            byte[] receiveData = new byte[1024];
            serverSocket = new DatagramSocket(SERVERPORT);
            DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
            while(work)
            {
                //get DNS request from client
                serverSocket.receive(receivePacket);
                String sentence = new String(receivePacket.getData(), 0, receivePacket.getLength());
                Log.d(TAG, sentence);
                //forward to real DNS server
                DatagramSocket clientSocket = new DatagramSocket();
                InetAddress dnsAddr = InetAddress.getByName("8.8.8.8");
                DatagramPacket dnsQuery = new DatagramPacket(receivePacket.getData(),
                        receivePacket.getLength(), dnsAddr, 53);
                clientSocket.send(dnsQuery);
                //get reply back from real DNS server
                byte[] receiveData1 = new byte[1024];
                DatagramPacket dnsReply = new DatagramPacket(receiveData1, receiveData1.length);
                clientSocket.receive(dnsReply);
                //send reply to client
                receivePacket.setData(dnsReply.getData());
                clientSocket.send(receivePacket);
                clientSocket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            //TODO: clean up?
        }
    }

}
