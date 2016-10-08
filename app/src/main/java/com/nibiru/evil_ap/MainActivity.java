package com.nibiru.evil_ap;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.wifi.WifiManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import static android.net.ConnectivityManager.EXTRA_NETWORK_TYPE;
import static android.net.ConnectivityManager.TYPE_WIFI;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    //CLASS FIELDS
    final static String TAG = "MainActivity";
    WifiManager wifiManager;
    boolean wifiON;
    Button bAP;
    /*************************************************************/

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        TextView tv = (TextView) findViewById(R.id.sample_text);
        tv.setText("Welcome to Evil-AP");

        //Set on click listeners
        bAP = (Button)findViewById(R.id.bAP);
        bAP.setOnClickListener(this);

        //Find the current state of WiFi
        //http://stackoverflow.com/questions/8863509/how-to-programmatically-turn-off-wifi-on-android-device
        wifiManager = (WifiManager) this.getSystemService(Context.WIFI_SERVICE);
        wifiON = wifiManager.isWifiEnabled();
        setBtnUI(wifiON);

        //Register BroadcastReceiver, filer specific intents
        registerReceiver(new WiFiBroadcastReceiver(), new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
    }

    private void setBtnUI(boolean wifiON) {
        if (wifiON)
            bAP.setText("Stop WiFI");
        else
            bAP.setText("Start WiFi");
    }

    /**
     * A native method that is implemented by the 'native-lib' native library,
     * which is packaged with this application.

    public native String stringFromJNI();

    // Used to load the 'native-lib' library on application startup.
    static {
        System.loadLibrary("native-lib");
    }*/
    public void onClick(View v) {
        // default method for handling onClick Events for our MainActivity
        switch (v.getId()) {
            case R.id.bAP:
                Log.d(TAG, "AP button pressed");
                //if AP button was pressed turn on/off hotspot && proxy service
                wifiON = !wifiON;
                setBtnUI(wifiON);
                wifiManager.setWifiEnabled(wifiON);
                break;
        }
    }

    private class WiFiBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            //check if network change is for WiFi
            //TODO: make sure intent really means interface went ON/OFF
            //TODO: or just read the WiFi state and update perhaps to the same value!
            if (intent.getIntExtra(EXTRA_NETWORK_TYPE, 0) == TYPE_WIFI){
                wifiON = wifiManager.isWifiEnabled();
                setBtnUI(wifiON);
            }
        }
    }

}
