package com.nibiru.evil_ap.log;

import java.util.HashMap;
import java.util.List;
import java.util.Vector;

/**
 * Created by Nibiru on 2016-11-04.
 */

public class Client {
    /**************************************CLASS FIELDS********************************************/
    private final static String TAG = "Client";
    private final String ip;
    private final String mac;
    private Log log;
    /**************************************CLASS METHODS*******************************************/
    public Client (String ip, String mac){
        this.ip = ip;
        this.mac = mac;
    }
    public String getIp(){return ip;}
    public String getMac() {return mac;}
}
