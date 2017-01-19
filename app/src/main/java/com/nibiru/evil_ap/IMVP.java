package com.nibiru.evil_ap;

import android.app.Activity;
import android.content.Context;
import android.net.Uri;
import android.support.v4.util.Pair;

import com.nibiru.evil_ap.log.Client;
import com.nibiru.evil_ap.log.LogEntry;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Nibiru on 2016-12-06.
 * Aggregates all communication operations between MVP pattern layer:
 * Model, View and Presenter
 * Reference: http://www.tinmegali.com/en/model-view-presenter-mvp-in-android-part-2/
 */
public interface IMVP {

    /**
     * View mandatory methods. Available to Presenter
     *      Presenter -> View
     */
    interface RequiredViewOps {
        void showToast(String msg);
        void dieUI();
    }

    /**
     * Operations offered from Presenter to View
     *      View -> Presenter
     */
    interface PresenterOps{
        void onDestroy(boolean isChangingConfig);

        void checkIfDeviceRooted();
        boolean apBtnPressed(String SSID, String pass, Context ctx);

        boolean isApOn(Context ctx);
        void onTrafficRedirect(String traffic, boolean on);
        void onConfigurationChanged(RequiredViewOps view);
        void onLoadReplaceImg(Uri uri, Activity act);
        void onJsPayloadApply(List<Pair<Integer, String>> payloads);

        void setSharedPrefsInt(String tag, int val);
        void setSharedPrefsBool(String tag, boolean val);
        void setSharedPrefsString(String tag, String val);
        void setBan(Client c, boolean banned);

        ArrayList<Client> getCurrentClients();
        List<LogEntry> getClientLog(Client c);
        int getSharedPrefsInt(String tag);
        boolean getSharedPrefsBool(String tag);
        String getSharedPrefsString(String tag);
        SharedClass getSharedObj();
        void onClean();
        void dieUI();

        // any other ops to be called from View
    }

    /**
     * Operations offered from Presenter to Model
     *      Model -> Presenter
     */
    interface RequiredPresenterOps {
        void onError(String errorMsg);
        // Any other returning operation Model -> Presenter
    }

    /**
     * Model operations offered to Presenter
     *      Presenter -> Model
     */
    interface ModelOps {
        void onDestroy();

        void onTrafficRedirect(String traffic, boolean on);
        void onLoadReplaceImg(Uri uri, Activity act);
        void onJsPayloadApply(List<Pair<Integer, String>> payloads);
        boolean onApToggle(String SSID, String pass, Context ctx);
        boolean isApOn(Context ctx);
        boolean isDeviceRooted();

        void setSharedPrefsInt(String tag, int val);
        void setSharedPrefsBool(String tag, boolean val);
        void setSharedPrefsString(String tag, String val);
        void setBan(Client c, boolean banned);

        ArrayList<Client> getCurrentClients();
        int getSharedPrefsInt(String tag);
        boolean getSharedPrefsBool(String tag);
        String getSharedPrefsString(String tag);
        SharedClass getSharedObj();
        Client getClientByIp(String ip);
        List<LogEntry> getClientLog(Client c);
        void onClean();

        // Any other data operation
    }

}