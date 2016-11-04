package com.nibiru.evil_ap;

import java.util.HashMap;
import java.util.List;
import java.util.Vector;

/**
 * Created by Nibiru on 2016-11-04.
 */

public class Client {
    private final static String TAG = "Client";
    private final String ip;
    private final String mac;
    private Log log;
    /*********************************************************************************************/
    public Client (String ip, String mac){
        this.ip = ip;
        this.mac = mac;
    }

    public String getIp(){return ip;}
    public String getMac() {return mac;}

    private class Log {
        public Vector<String> hosts;
        public HashMap<String, List<String>> hostsRequests;
        public HashMap<String, List<String>> hostsCookies;
    }
}
