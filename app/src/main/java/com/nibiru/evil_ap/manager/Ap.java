package com.nibiru.evil_ap.manager;

import android.content.Context;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;

import java.lang.reflect.Method;

/**
 * Created by Nibiru on 2016-10-11.
 * Ap class takes care of checking the state of hotspot
 * it can also toggle the hotspot on and off
 */

public class Ap {

    /**************************************CLASS FIELDS********************************************/
    protected final String TAG = getClass().getSimpleName();
    /**************************************CLASS METHODS*******************************************/
    //check whether WiFI hotspot is on or off
    public boolean isApOn(Context ctx) {
        try {
            WifiManager wifiMan = (WifiManager) ctx.getSystemService(Context.WIFI_SERVICE);
            Method method = wifiMan.getClass().getDeclaredMethod("isWifiApEnabled");
            method.setAccessible(true);
            return (Boolean) method.invoke(wifiMan);
        }
        catch (Throwable ignored) {}
        return false;
    }

    // toggle WiFi hotspot on
    public boolean turnOnAp(String SSID, String PSK, Context ctx ) {
        WifiManager wifiMan = (WifiManager) ctx.getSystemService(Context.WIFI_SERVICE);
        WifiConfiguration wifiConfig = new WifiConfiguration();
        wifiConfig.SSID = SSID;
        //set password if given, open if null
        if (PSK!=null) {
            wifiConfig.preSharedKey = PSK;
            wifiConfig.hiddenSSID = false;
            wifiConfig.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN);
            wifiConfig.allowedProtocols.set(WifiConfiguration.Protocol.RSN);
            wifiConfig.allowedKeyManagement.set(4);
            wifiConfig.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
            wifiConfig.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
        }

        try {
            // if WiFi is on, turn it off
            if(wifiMan.isWifiEnabled()) {
                wifiMan.setWifiEnabled(false);
            }
            Method method = wifiMan.getClass()
                    .getMethod("setWifiApEnabled", WifiConfiguration.class, boolean.class);
            method.invoke(wifiMan, wifiConfig, true);
            return true;
        }
        catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    // toggle WiFi hotspot off
    public boolean turnOffAp(Context ctx) {
        WifiManager wifiMan = (WifiManager) ctx.getSystemService(Context.WIFI_SERVICE);
        Method method;
        try {
            method = wifiMan.getClass()
                    .getMethod("setWifiApEnabled", WifiConfiguration.class, boolean.class);
            method.invoke(wifiMan, null, false);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }
}
