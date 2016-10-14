package com.nibiru.evil_ap;

import android.content.Context;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import java.lang.reflect.Method;

/**
 * Created by Nibiru on 2016-10-11.
 * ApManager class takes care of checking the state of hotspot
 * it can also toggle the hotspot on and off
 */

class ApManager {

    //CLASS FIELDS
    final static String TAG = "ApManager";
    WifiManager wifiManager;
    /*************************************************************/

    ApManager(Context ctx){
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

    // toggle WiFi hotspot on or off
    public boolean configApState(String SSID, String PSK ) {
        WifiConfiguration wifiConfig = new WifiConfiguration();
        wifiConfig.SSID = "MyAP";
        wifiConfig.preSharedKey = PSK;
        wifiConfig.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN);
        wifiConfig.allowedProtocols.set(WifiConfiguration.Protocol.RSN);
        wifiConfig.allowedProtocols.set(WifiConfiguration.Protocol.WPA);
        wifiConfig.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
        try {
            // if WiFi is on, turn it off
            if(wifiManager.isWifiEnabled()) {
                wifiManager.setWifiEnabled(false);
            }

            Method method = wifiManager.getClass()
                    .getMethod("setWifiApEnabled", WifiConfiguration.class, boolean.class);
            method.invoke(wifiManager, wifiConfig, !isApOn());
            Method setConfigMethod = wifiManager.getClass().getMethod("setWifiApConfiguration", WifiConfiguration.class);
            setConfigMethod.invoke(wifiManager, wifiConfig);
            return true;
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }
}
