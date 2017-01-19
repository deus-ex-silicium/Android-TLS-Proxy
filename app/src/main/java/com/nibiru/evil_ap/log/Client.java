package com.nibiru.evil_ap.log;

/**
 * Created by Nibiru on 2016-11-04.
 */

public class Client {
    /**************************************CLASS FIELDS********************************************/
    private final static String TAG = "Client";
    private final String ip;
    private final String mac;
    private boolean banned;

    /**************************************CLASS METHODS*******************************************/
    public Client (String ip, String mac, boolean banned){
        this.ip = ip;
        this.mac = mac;
        this.banned = banned;
    }
    public String getIp(){return ip;}
    public String getMac() {return mac;}
    public boolean getBanned(){return banned;}
}
