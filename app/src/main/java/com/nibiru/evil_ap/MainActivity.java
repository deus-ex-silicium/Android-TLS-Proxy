package com.nibiru.evil_ap;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.Toast;

import com.nibiru.evil_ap.fragments.ACFragment;
import com.nibiru.evil_ap.fragments.ClientsFragment;
import com.nibiru.evil_ap.fragments.MainFragment;
import com.nibiru.evil_ap.proxy.ProxyService;

public class MainActivity extends AppCompatActivity implements MainFragment
        .OnFragmentInteractionListener,ClientsFragment
        .OnFragmentInteractionListener,ACFragment
        .OnFragmentInteractionListener {
    //CLASS FIELDS
    final static String TAG = "MainActivity";
    Fragment MainFragment = new MainFragment();
    ManagerRoot RootMan;
    /*********************************************************************************************/

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
        tabLayout.setOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
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
            if (!RootMan.isPortRedirected(1337)){
                RootMan.RunAsRoot("iptables -t nat -I PREROUTING -i wlan0 -p tcp --dport 80 -j " +
                        "REDIRECT --to-port 1337");
            }
            if (!RootMan.isPortRedirected(1338)){
                RootMan.RunAsRoot("iptables -t nat -I PREROUTING -i wlan0 -p tcp --dport 443 -j " +
                        "REDIRECT --to-port 1338");
            }
            /*if (!RootMan.isPortRedirected(1339)){
                RootMan.RunAsRoot("iptables -t nat -I PREROUTING -i wlan0 -p udp --dport 53 -j " +
                        "REDIRECT --to-port 1339");
            }*/
        }
        else {
            ManagerAp.turnOffAp(this);
            stopService(new Intent(this, ProxyService.class));
            if (RootMan.isPortRedirected(1337)){
                RootMan.RunAsRoot("iptables -t nat -D PREROUTING -i wlan0 -p tcp --dport 80 -j " +
                        "REDIRECT --to-port 1337");
            }
            if (RootMan.isPortRedirected(1338)){
                RootMan.RunAsRoot("iptables -t nat -D PREROUTING -i wlan0 -p tcp --dport 443 -j " +
                        "REDIRECT --to-port 1338");
            }
            if (RootMan.isPortRedirected(1339)){
                RootMan.RunAsRoot("iptables -t nat -D PREROUTING -i wlan0 -p udp --dport 53 -j " +
                        "REDIRECT --to-port 1339");
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
