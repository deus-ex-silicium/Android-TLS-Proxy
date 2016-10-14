package com.nibiru.evil_ap;

import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.nibiru.evil_ap.fragments.MainFragment;
import com.nibiru.evil_ap.proxy.ProxyService;

public class MainActivity extends AppCompatActivity implements MainFragment
        .OnFragmentInteractionListener {
    private static final int CONTENT_VIEW_ID = 10101010;
    //CLASS FIELDS
    final static String TAG = "MainActivity";
    Fragment MainFragment = new MainFragment();
    ManagerRoot RootMan;
    /*********************************************************************************************/

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        TextView tv = (TextView) findViewById(R.id.sample_text);
        tv.setText("Welcome to Evil-AP");
        android.app.FragmentManager fragmentManager = getFragmentManager();
            fragmentManager.beginTransaction()
                    .replace(R.id.activity_main, MainFragment).addToBackStack(null)
                    .commit();
        //check if device is rooted
        RootMan = new ManagerRoot();
        if (!RootMan.isDeviceRooted()){
            toastMessage("Application will only function properly on rooted phones with root " +
                    "permissions");
        }
    }

    public void onPowerBtnPressed(View v) {
        //if AP button was pressed turn on/off hotspot && proxy service
        boolean isApOn = ManagerAp.isApOn(this);
        if(!isApOn){
            ManagerAp.turnOnAp("AP", "pa$$word", this);
            startService(new Intent(this, ProxyService.class));
            if (!RootMan.isHttpRedirected()){
                RootMan.RunAsRoot("iptables -t nat -I PREROUTING -i wlan0 -p tcp --dport 80 -j " +
                        "REDIRECT --to-port 1337");
            }
        }
        else {
            ManagerAp.turnOffAp(this);
            stopService(new Intent(this, ProxyService.class));
            if (RootMan.isHttpRedirected()){
                RootMan.RunAsRoot("iptables -t nat -D PREROUTING -i wlan0 -p tcp --dport 80 -j " +
                        "REDIRECT --to-port 1337");
            }
        }
    }

    @Override
    public void onFragmentInteraction(Uri uri) {}

    //function for debugging etc. (shows toast with msg text)
    public void toastMessage(String msg){
        Context context = getApplicationContext();
        int duration = Toast.LENGTH_SHORT;
        Toast toast = Toast.makeText(context, msg, duration);
        toast.show();
    }

}
