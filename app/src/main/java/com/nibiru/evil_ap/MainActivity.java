package com.nibiru.evil_ap;

import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.nibiru.evil_ap.Fragments.MainFragment;


public class MainActivity extends AppCompatActivity implements MainFragment
        .OnFragmentInteractionListener {
    private static final int CONTENT_VIEW_ID = 10101010;
    //CLASS FIELDS
    final static String TAG = "MainActivity";
    Fragment MainFragment = new MainFragment();
    ApManager ApMan;
    /*************************************************************/

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
        ApMan = new ApManager(this);
    }

    public void onPowerBtnPressed(View v) {
        //if AP button was pressed turn on/off hotspot && proxy service
        //TODO: PROXY SERVICE
        boolean isApOn = ApMan.isApOn();
        ApMan.configApState("AP2", "pa$$word");
        toastMessage("btn clicked");
    }

    @Override
    public void onFragmentInteraction(Uri uri) {


    }

    //function for debugging etc. (shows toast with msg text)
    public void toastMessage(String msg){
        Context context = getApplicationContext();
        int duration = Toast.LENGTH_SHORT;
        Toast toast = Toast.makeText(context, msg, duration);
        toast.show();
    }

}
