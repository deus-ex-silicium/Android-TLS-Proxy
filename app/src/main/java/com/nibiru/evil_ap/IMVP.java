package com.nibiru.evil_ap;

import android.content.Context;

import com.nibiru.evil_ap.log.Client;

import java.util.ArrayList;

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
        // any other ops
    }

    /**
     * Operations offered from Presenter to View
     *      View -> Presenter
     */
    interface PresenterOps{
        void checkIfDeviceRooted();
        boolean apBtnPressed(String SSID, String pass, Context ctx);
        boolean isApOn(Context ctx);
        void onTrafficRedirect(String traffic, boolean on);
        ArrayList<Client> getCurrentClients();
        void onConfigurationChanged(RequiredViewOps view);
        void onDestroy(boolean isChangingConfig);
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
        void onTrafficRedirect(String traffic, boolean on);
        ArrayList<String> getCurrentClients();
        boolean apToggle(String SSID, String pass, Context ctx);
        boolean isApOn(Context ctx);
        boolean isDeviceRooted();
        void onDestroy();
        // Any other data operation
    }
}