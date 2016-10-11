package com.nibiru.evil_ap;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;


public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    //CLASS FIELDS
    final static String TAG = "MainActivity";
    ApManager ApMan;
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

        // only for marshmallow and newer versions, we need user to explicitly grant us WRITE_SETTINGS
        // permissions to be able to change hotspot configuration
        //TODO: what about other versions ?
        //http://stackoverflow.com/questions/32083410/cant-get-write-settings-permission/32083622#32083622
        if (Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS);
            intent.setData(Uri.parse("package:" + this.getPackageName()));
            startActivity(intent);
        }
        //Find the current state of AP
        ApMan = new ApManager(this);
        setBtnUI(ApMan.isApOn());

        //Register BroadcastReceiver, filer specific intents
        registerReceiver(new ApBroadcastReceiver(), new IntentFilter("android.net.wifi.WIFI_AP_STATE_CHANGED"));
    }

    private void setBtnUI(boolean wifiON) {
        if (wifiON)
            bAP.setText("Stop AP");
        else
            bAP.setText("Start AP");
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
                //TODO: PROXY SERVICE
                ApMan.configApState();
                setBtnUI(ApMan.isApOn());
                break;
        }
    }

    private class ApBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            //something about AP changed so update the UI button
            setBtnUI(ApMan.isApOn());
        }
    }

    //function for debugging etc. (shows toast with msg text)
    public void toastMessage(String msg){
        Context context = getApplicationContext();
        int duration = Toast.LENGTH_SHORT;
        Toast toast = Toast.makeText(context, msg, duration);
        toast.show();
    }

}
