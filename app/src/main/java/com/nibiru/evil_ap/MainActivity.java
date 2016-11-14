package com.nibiru.evil_ap;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.nibiru.evil_ap.fragments.ACFragment;
import com.nibiru.evil_ap.fragments.ClientsFragment;
import com.nibiru.evil_ap.fragments.MainFragment;
import com.nibiru.evil_ap.log.Client;
import com.nibiru.evil_ap.proxy.ProxyService;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements MainFragment
        .OnFragmentInteractionListener,ClientsFragment
        .OnFragmentInteractionListener,ACFragment
        .OnFragmentInteractionListener {
    /**************************************CLASS FIELDS********************************************/
    final static String TAG = "MainActivity";
    ManagerRoot rootMan;
    ManagerRouting routingMan;
    /**************************************CLASS METHODS*******************************************/

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        TabLayout tabLayout = (TabLayout) findViewById(R.id.tab_layout);
        tabLayout.addTab(tabLayout.newTab().setText("Config"));
        tabLayout.addTab(tabLayout.newTab().setText("Clients"));
        tabLayout.addTab(tabLayout.newTab().setText("Action Center"));
        tabLayout.setTabGravity(TabLayout.GRAVITY_FILL);

        final ViewPager viewPager = (ViewPager) findViewById(R.id.pager);
        final com.nibiru.evil_ap.adapters.PagerAdapter adapter = new com.nibiru.evil_ap.adapters.PagerAdapter
                (getSupportFragmentManager(), tabLayout.getTabCount());
        viewPager.setAdapter(adapter);
        viewPager.addOnPageChangeListener(new TabLayout.TabLayoutOnPageChangeListener(tabLayout));
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                viewPager.setCurrentItem(tab.getPosition());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        });
        //check if device is rooted
        if (!ManagerRoot.isDeviceRooted()){
            toastMessage("Application will only function properly on rooted phones with root " +
                    "permissions");
        }
        routingMan = new ManagerRouting();
    }

    public void onPowerBtnPressed(View v) {
        //if AP button was pressed turn on/off hotspot && proxy service
        boolean isApOn = ManagerAp.isApOn(this);
        if(!isApOn){
            ManagerAp.turnOnAp("AP", "pa$$word", this);
            startService(new Intent(this, ProxyService.class));
            //routingMan.redirectHTTP(rootMan, true);
            //routingMan.redirectHTTPS(rootMan, true);
            //routingMan.redirectDNS(rootMan, true);
        }
        else {
            ManagerAp.turnOffAp(this);
            stopService(new Intent(this, ProxyService.class));
            //routingMan.redirectHTTP(rootMan, false);
            //routingMan.redirectHTTPS(rootMan, false);
            //routingMan.redirectDNS(rootMan, false);
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
