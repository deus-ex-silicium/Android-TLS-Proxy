package com.nibiru.evil_ap;

import android.content.Context;
import android.content.Intent;

import com.nibiru.evil_ap.log.Client;
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
    // Configuration settings
    private String apSSID;
    private String apPass;
    private boolean redirectHTTP;
    private boolean redirectHTTPS;
    private boolean swapImgHTTP;
    private boolean swapImgHTTPS;
    private boolean sslStrip;
    private String imgPath;
    // Presenter reference
    private IMVP.RequiredPresenterOps mPresenter;
    // Manager helpers
    private Root mRootMan;
    private Ap mApMan;
    private Routing mRouteMan;
    /**************************************CLASS METHODS*******************************************/
    public Model(IMVP.RequiredPresenterOps mPresenter) {
        this.mPresenter = mPresenter;
        mRootMan = new Root();
        mApMan = new Ap();
        mRouteMan = new Routing();
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

    public void onTrafficRedirect(String traffic, boolean on){
        switch (traffic){
            case "HTTP": mRouteMan.redirectHTTP(on, mRootMan);
                break;
            case "HTTPS": mRouteMan.redirectHTTPS(on, mRootMan);
                break;
            case "DNS": mRouteMan.redirectDNS(on, mRootMan);
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
            //mRouteMan.redirectHTTP(true, mRootMan);
            return true;
        }
        else{
            mApMan.turnOffAp(ctx);
            //mRouteMan.redirectHTTP(false, mRootMan);
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
