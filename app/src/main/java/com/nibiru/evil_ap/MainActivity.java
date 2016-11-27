package com.nibiru.evil_ap;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.support.design.widget.TabLayout;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.nibiru.evil_ap.fragments.ACFragment;
import com.nibiru.evil_ap.fragments.ACHTTPFragment;
import com.nibiru.evil_ap.fragments.ACHTTPSFragment;
import com.nibiru.evil_ap.fragments.ClientsFragment;
import com.nibiru.evil_ap.fragments.MainFragment;
import com.nibiru.evil_ap.manager.Ap;
import com.nibiru.evil_ap.manager.Root;
import com.nibiru.evil_ap.manager.Routing;
import com.nibiru.evil_ap.proxy.ProxyService;

public class MainActivity extends AppCompatActivity implements MainFragment
        .OnFragmentInteractionListener,ClientsFragment
        .OnFragmentInteractionListener,ACFragment
        .OnFragmentInteractionListener, ACHTTPFragment.OnFragmentInteractionListener,
        ACHTTPSFragment.OnFragmentInteractionListener {
    /**************************************CLASS FIELDS********************************************/
    final static String TAG = "MainActivity";
    public ProxyService proxyService;
    private boolean psIsBound;
    private ServiceConnection mConnection;

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
        FrameLayout r = (FrameLayout)findViewById(R.id.activity_main);
        r.setBackground((getResources().getDrawable(R.drawable.bground)));
        //check if device is rooted
        if (!Root.isDeviceRooted()){
            toastMessage("Application will only function properly on rooted phones with root " +
                    "permissions");
        }
        else {
            startService(new Intent(this, ProxyService.class));
            mConnection = new ServiceConnection() {
                public void onServiceConnected(ComponentName className, IBinder service) {
                    // This is called when the connection with the service has been
                    // established, giving us the service object we can use to
                    // interact with the service.  Because we have bound to a explicit
                    // service that we know is running in our own process, we can
                    // cast its IBinder to a concrete class and directly access it.
                    proxyService = ((ProxyService.LocalBinder)service).getService();
                }
                public void onServiceDisconnected(ComponentName className) {
                    // This is called when the connection with the service has been
                    // unexpectedly disconnected -- that is, its process crashed.
                    // Because it is running in our same process, we should never
                    // see this happen.
                    proxyService = null;
                }
            };
            doBindService();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        doUnbindService();
    }

    public void onPowerBtnPressed(View v) {
        //if AP button was pressed turn on/off hotspot && proxy service
        boolean isApOn = Ap.isApOn(this);
        if(!isApOn){
            Ap.turnOnAp("AP", "pa$$word", this);
            (v.findViewById(R.id.editText)).setFocusable(false);
            (v.findViewById(R.id.editText2)).setFocusable(false);
            //startService(new Intent(this, ProxyService.class));
            //routingMan.redirectHTTP(rootMan, true);
            //routingMan.redirectHTTPS(rootMan, true);
            //routingMan.redirectDNS(rootMan, true);
        }
        else {
            Ap.turnOffAp(this);
            stopService(new Intent(this, ProxyService.class));
            (v.findViewById(R.id.editText)).setFocusableInTouchMode(true);
            (v.findViewById(R.id.editText2)).setFocusableInTouchMode(true);
            //routingMan.redirectHTTP(rootMan, false);
            //routingMan.redirectHTTPS(rootMan, false);
            //routingMan.redirectDNS(rootMan, false);
        }
    }

    void doBindService() {
        // Establish a connection with the service.  We use an explicit
        // class name because we want a specific service implementation that
        // we know will be running in our own process (and thus won't be
        // supporting component replacement by other applications).
        bindService(new Intent(this, ProxyService.class), mConnection, Context.BIND_AUTO_CREATE);
        psIsBound = true;
    }

    void doUnbindService() {
        if (psIsBound) {
            // Detach our existing connection.
            unbindService(mConnection);
            psIsBound = false;
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
