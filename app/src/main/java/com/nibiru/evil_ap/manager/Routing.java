package com.nibiru.evil_ap.manager;

/**
 * Created by Nibiru on 2016-11-02.
 * Manager class that takes care of routing and redirecting different types fo traffic.
 */

public class Routing {
    final private static String TAG = "Routing";
    final private static String HTTP_LISTNER_PORT = "1337";
    final private static String HTTPS_LISTNER_PORT = "1338";
    final private static String DNS_LISTNER_PORT = "1339";
    /*********************************************************************************************/
    public Routing() {}

    private boolean isPortReceiving(String port, Root mRootMan){
        return mRootMan.RunAsRoot("iptables -t nat -L | grep \"redir ports "
                + port + "\"" );
    }

    public void filterMAC(Root mRootMan, String MAC, boolean ban){
        if (ban) {
            mRootMan.RunAsRoot("iptables -A INPUT -p all -m mac --mac-source " + MAC + " -j DROP");
        }
        else{
            mRootMan.RunAsRoot("iptables -D INPUT -p all -m mac --mac-source " + MAC + " -j DROP");
        }
    }

    //add rule only if it doesn't exist, delete only if it exists
    public void redirectHTTP(boolean on, Root mRootMan){
        if (on && !isPortReceiving(HTTP_LISTNER_PORT, mRootMan)) {
            mRootMan.RunAsRoot("iptables -t nat -I PREROUTING -i wlan0 -p tcp --dport 80 -j " +
                    "REDIRECT --to-port " + HTTP_LISTNER_PORT);
        }
        else if (!on && isPortReceiving(HTTP_LISTNER_PORT, mRootMan)){
            mRootMan.RunAsRoot("iptables -t nat -D PREROUTING -i wlan0 -p tcp --dport 80 -j " +
                    "REDIRECT --to-port " + HTTP_LISTNER_PORT);
        }
    }

    public void redirectHTTPS(boolean on, Root mRootMan){
        if (on && !isPortReceiving(HTTPS_LISTNER_PORT, mRootMan)) {
            mRootMan.RunAsRoot("iptables -t nat -I PREROUTING -i wlan0 -p tcp --dport 443 -j " +
                    "REDIRECT --to-port " + HTTPS_LISTNER_PORT);
        }
        else if (!on && isPortReceiving(HTTPS_LISTNER_PORT, mRootMan)){
            mRootMan.RunAsRoot("iptables -t nat -D PREROUTING -i wlan0 -p tcp --dport 443 -j " +
                    "REDIRECT --to-port " + HTTPS_LISTNER_PORT);
        }
    }

    public void redirectDNS(boolean on, Root mRootMan){
        if (on && !isPortReceiving(DNS_LISTNER_PORT, mRootMan)){
            mRootMan.RunAsRoot("iptables -t nat -I PREROUTING -i wlan0 -p udp --dport 53 -j " +
                    "REDIRECT --to-port " + DNS_LISTNER_PORT);
        }
        else if (!on && isPortReceiving(DNS_LISTNER_PORT, mRootMan)){
            mRootMan.RunAsRoot("iptables -t nat -D PREROUTING -i wlan0 -p udp --dport 53 -j " +
                    "REDIRECT --to-port " + DNS_LISTNER_PORT);
        }
    }
}
