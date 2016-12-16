package com.nibiru.evil_ap;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import com.nibiru.evil_ap.manager.Ap;
import com.nibiru.evil_ap.manager.Root;
import com.nibiru.evil_ap.manager.Routing;
import com.nibiru.evil_ap.proxy.ProxyService;

import java.util.ArrayList;

/**
 * Created by Nibiru on 2016-12-06.
 */

public class Model implements IMVP.ModelOps{
    /**************************************CLASS FIELDS********************************************/
    protected final String TAG = getClass().getSimpleName();
    // Presenter reference
    private IMVP.RequiredPresenterOps mPresenter;
    // Manager helpers
    private Root mRootMan;
    private Ap mApMan;
    private Routing mRouteMan;
    private SharedPreferences mConfig;

    /**************************************CLASS METHODS*******************************************/
    public Model(IMVP.RequiredPresenterOps mPresenter, Context ctx) {
        this.mPresenter = mPresenter;
        mRootMan = new Root();
        mApMan = new Ap();
        mRouteMan = new Routing();
        this.mConfig = ctx.getSharedPreferences("Config", 0);
    }
    /**
     * Sent from {@link Presenter#onDestroy(boolean)}
     * Should stop/kill operations that could be running
     * and aren't needed anymore
     */
    @Override
    public void onDestroy() {
        // destroying actions
    }
    public void setSharedPrefsInt(String tag, int val){
        mConfig.edit().putInt(tag,val).apply();
    }
    public void setSharedPrefsBool(String tag, boolean val){
        mConfig.edit().putBoolean(tag,val).apply();
    }
    public void setSharedPrefsString(String tag, String val){
        mConfig.edit().putString(tag,val).apply();
    }
    public int getSharedPrefsInt(String tag){
        return mConfig.getInt(tag, -1);
    }
    public boolean getSharedPrefsBool(String tag){
        return mConfig.getBoolean(tag, false);
    }
    public String getSharedPrefsString(String tag){
        return mConfig.getString(tag,"");
    }
    public boolean checkIfSharedPrefsNull(){
        if(mConfig != null){
            return false;
        }
        return true;
    }
    public boolean checkIfSharedPrefsContain(String tag){
        return mConfig.contains(tag);
    }
    public void onTrafficRedirect(String traffic, boolean on){
        switch (traffic){
            case "HTTP":
                mRouteMan.redirectHTTP(on, mRootMan);
                break;
            case "HTTPS":
                mRouteMan.redirectHTTPS(on, mRootMan);
                break;
            case "DNS":
                mRouteMan.redirectDNS(on, mRootMan);
                break;
        }
    }

    public boolean apToggle(String SSID, String pass, Context ctx){
        if (!mApMan.isApOn(ctx)){
            if( SSID.equals("") || pass.equals("") ) {
                mApMan.turnOnAp("AP", "pa$$word", ctx);
            }
            else {
                mApMan.turnOnAp(SSID, pass, ctx);
            }
            ctx.startService(new Intent(ctx, ProxyService.class));
            return true;
        }
        else{
            mApMan.turnOffAp(ctx);
            ctx.stopService(new Intent(ctx, ProxyService.class));
            return false;
        }
    }

    public ArrayList<String> getCurrentClients(){
        return mRootMan.RunAsRootWithOutput("ip -4 neigh");
    }

    public boolean isApOn(Context ctx){
        return mApMan.isApOn(ctx);
    }

    public boolean isDeviceRooted(){
        return mRootMan.isDeviceRooted();
    }
}
