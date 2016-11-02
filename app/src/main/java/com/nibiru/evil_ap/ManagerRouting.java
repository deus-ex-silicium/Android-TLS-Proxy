package com.nibiru.evil_ap;

/**
 * Created by Nibiru on 2016-11-02.
 * Manager class that takes care of routing and redirecting different types fo traffic.
 */

public class ManagerRouting {
    final static String TAG = "ManagerRouting";
    final private String HTTP_LISTNER_PORT = "1337";
    final private String HTTPS_LISTNER_PORT = "1338";
    final private String DNS_LISTNER_PORT = "1339";
    /*********************************************************************************************/
    ManagerRouting() {}

    private boolean isPortReceiving(ManagerRoot rootMan, String port){
        return rootMan.RunAsRoot("iptables -t nat -L | grep \"redir ports "
                + port + "\"" );
    }

    //TODO: TEST!
    public void filterMAC(ManagerRoot rootMan, String MAC, boolean ban){
        if (ban) {
            rootMan.RunAsRoot("iptables -t nat -A PREROUTING -p all -m mac --mac-source " +
                    MAC + " -j DROP");
        }
        else{
            rootMan.RunAsRoot("iptables -t nat -D PREROUTING -p all -m mac --mac-source " +
                    MAC + " -j DROP");
        }
    }

    //add rule only if it doesn't exist, delete only if it exists
    void redirectHTTP(ManagerRoot rootMan, boolean on){
        if (on && !isPortReceiving(rootMan, HTTP_LISTNER_PORT)) {
            rootMan.RunAsRoot("iptables -t nat -I PREROUTING -i wlan0 -p tcp --dport 80 -j " +
                    "REDIRECT --to-port " + HTTP_LISTNER_PORT);
        }
        else if (!on && isPortReceiving(rootMan, HTTP_LISTNER_PORT)){
            rootMan.RunAsRoot("iptables -t nat -D PREROUTING -i wlan0 -p tcp --dport 80 -j " +
                    "REDIRECT --to-port " + HTTP_LISTNER_PORT);
        }
    }

    void redirectHTTPS(ManagerRoot rootMan, boolean on){
        if (on && !isPortReceiving(rootMan, HTTPS_LISTNER_PORT)) {
            rootMan.RunAsRoot("iptables -t nat -I PREROUTING -i wlan0 -p tcp --dport 443 -j " +
                    "REDIRECT --to-port " + HTTPS_LISTNER_PORT);
        }
        else if (!on && isPortReceiving(rootMan, HTTPS_LISTNER_PORT)){
            rootMan.RunAsRoot("iptables -t nat -D PREROUTING -i wlan0 -p tcp --dport 443 -j " +
                    "REDIRECT --to-port " + HTTPS_LISTNER_PORT);
        }
    }

    void redirectDNS(ManagerRoot rootMan, boolean on){
        if (on && !isPortReceiving(rootMan, DNS_LISTNER_PORT)){
            rootMan.RunAsRoot("iptables -t nat -I PREROUTING -i wlan0 -p udp --dport 53 -j " +
                    "REDIRECT --to-port " + DNS_LISTNER_PORT);
        }
        else if (!on && isPortReceiving(rootMan, DNS_LISTNER_PORT)){
            rootMan.RunAsRoot("iptables -t nat -D PREROUTING -i wlan0 -p udp --dport 53 -j " +
                    "REDIRECT --to-port " + DNS_LISTNER_PORT);
        }
    }
}
