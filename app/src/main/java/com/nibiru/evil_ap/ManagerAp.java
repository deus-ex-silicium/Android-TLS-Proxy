package com.nibiru.evil_ap;

import android.content.Context;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Created by Nibiru on 2016-10-11.
 * ManagerAp class takes care of checking the state of hotspot
 * it can also toggle the hotspot on and off
 */

class ManagerAp {

    //CLASS FIELDS
    final static String TAG = "ManagerAp";
    WifiManager wifiManager;
    /*********************************************************************************************/
    ManagerAp(Context ctx){
        wifiManager = (WifiManager) ctx.getSystemService(ctx.WIFI_SERVICE);
    }

    //check whether WiFI hotspot is on or off
    public boolean isApOn() {
        try {
            Method method = wifiManager.getClass().getDeclaredMethod("isWifiApEnabled");
            method.setAccessible(true);
            return (Boolean) method.invoke(wifiManager);
        }
        catch (Throwable ignored) {}
        return false;
    }

    // toggle WiFi hotspot on
    public boolean turnOnAp(String SSID, String PSK ) {
        WifiConfiguration wifiConfig = new WifiConfiguration();
        wifiConfig.SSID = "AP";
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
            if(wifiManager.isWifiEnabled()) {
                wifiManager.setWifiEnabled(false);
            }
            Method method = wifiManager.getClass()
                    .getMethod("setWifiApEnabled", WifiConfiguration.class, boolean.class);
            method.invoke(wifiManager, wifiConfig, true);
            return true;
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    // toggle WiFi hotspot off
    public boolean turnOffAp() {
        Method method = null;
        try {
            method = wifiManager.getClass()
                    .getMethod("setWifiApEnabled", WifiConfiguration.class, boolean.class);
            method.invoke(wifiManager, null, false);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }
}
