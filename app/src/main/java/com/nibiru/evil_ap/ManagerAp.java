package com.nibiru.evil_ap;

import android.content.Context;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import java.lang.reflect.Method;

/**
 * Created by Nibiru on 2016-10-11.
 * ManagerAp class takes care of checking the state of hotspot
 * it can also toggle the hotspot on and off
 */

public class ManagerAp {

    //CLASS FIELDS
    final static String TAG = "ManagerAp";
    /*********************************************************************************************/
    public ManagerAp(){}

    //check whether WiFI hotspot is on or off
    public static boolean isApOn(Context ctx) {
        try {
            WifiManager wifiMan = (WifiManager) ctx.getSystemService(ctx.WIFI_SERVICE);
            Method method = wifiMan.getClass().getDeclaredMethod("isWifiApEnabled");
            method.setAccessible(true);
            return (Boolean) method.invoke(wifiMan);
        }
        catch (Throwable ignored) {}
        return false;
    }

    // toggle WiFi hotspot on
    static boolean turnOnAp(String SSID, String PSK, Context ctx ) {
        WifiManager wifiMan = (WifiManager) ctx.getSystemService(ctx.WIFI_SERVICE);
        WifiConfiguration wifiConfig = new WifiConfiguration();
        wifiConfig.SSID = SSID;
        //network will be open if password not given
        if (!PSK.isEmpty()) {
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
        }
        return false;
    }

    // toggle WiFi hotspot off
    static boolean turnOffAp(Context ctx) {
        WifiManager wifiMan = (WifiManager) ctx.getSystemService(ctx.WIFI_SERVICE);
        Method method = null;
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
