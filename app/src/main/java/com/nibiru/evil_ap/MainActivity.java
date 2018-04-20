package com.nibiru.evil_ap;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.design.widget.TabLayout;
import android.support.v4.content.ContextCompat;
import android.support.v4.util.Pair;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.Toast;


import com.nibiru.evil_ap.adapters.PagerAdapter;
import com.nibiru.evil_ap.fragments.ACFragment;
import com.nibiru.evil_ap.fragments.ClientsFragment;
import com.nibiru.evil_ap.fragments.MainFragment;
import com.nibiru.evil_ap.fragments.ServerDetailsFragment;
import com.nibiru.evil_ap.fragments.ServerItemFragment;
import com.nibiru.evil_ap.log.Client;
import com.nibiru.evil_ap.proxy.ProxyService;
import com.nibiru.evil_ap.ui.CustomViewPager;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements
        MainFragment.OnMainFragmentInteraction, ClientsFragment.onClientsFragmentInteraction,
         ACFragment.onAcFragmentInteraction,
         ServerItemFragment.onClientsFragmentInteraction,
        ServerDetailsFragment.OnFragmentInteractionListener, IMVP.RequiredViewOps {
    /**************************************CLASS FIELDS********************************************/
    private final String TAG = getClass().getSimpleName();
    private ProxyService.IProxyService mProxyService;
    private boolean psIsBound;
    private int selectedTab = 0;
    private ServiceConnection mConnection;
    // Responsible for maintaining objects state during changing configuration
    private final StateMaintainer mStateMaintainer =
            new StateMaintainer(this.getFragmentManager(), TAG);
    // Presenter operations
    private IMVP.PresenterOps mPresenter;

    /************************************ CLASS METHODS *******************************************/

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        startMVPOps();
        //set default skull pixel image
        SharedClass.getInstance().setImage(getResources().openRawResource(R.raw.pixel_skull));
        mPresenter.setSharedPrefsString(ConfigTags.imgPath.toString(),
                "android.resource://" + getPackageName() + "/" + R.raw.pixel_skull);
        //initialize fields in SharedClass
        SharedClass.getInstance().setDatabase(this);
        SharedClass.getInstance().setClient(getSharedPreferences("Config",0));

        setContentView(R.layout.activity_main);
        mPresenter.checkIfDeviceRooted();
        //start ProxyService and establish connection to it
        startService(new Intent(this, ProxyService.class));
        mConnection = new ServiceConnection() {
            public void onServiceConnected(ComponentName className, IBinder service) {
                mProxyService = ((ProxyService.IProxyService) service);
                mProxyService.setPresenter(mPresenter);
            }

            public void onServiceDisconnected(ComponentName className) {
                mProxyService = null;
            }
        };
        doBindService();

        setUpGUI();

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mPresenter = null;
        doUnbindService();
    }
    @Override
    public void onBackPressed() {
        int count = getFragmentManager().getBackStackEntryCount();
        if (count == 0) {
            super.onBackPressed();
            //additional code
        } else {
            ServerItemFragment myFragment = (ServerItemFragment) getFragmentManager()
                    .findFragmentByTag("ServerItem");
            ServerDetailsFragment myFragment2 = (ServerDetailsFragment) getFragmentManager()
                    .findFragmentByTag("ServerDetails");
            if ((myFragment != null && myFragment.isVisible() || (myFragment2 != null &&
                    myFragment2.isVisible())) && selectedTab == 1) {
                getFragmentManager().popBackStack();
            }

        }

    }

    private void doBindService() {
        // Establish a connection with the service.  We use an explicit
        // class name because we want a specific service implementation that
        // we know will be running in our own process (and thus won't be
        // supporting component replacement by other applications).
        bindService(new Intent(this, ProxyService.class), mConnection, Context.BIND_AUTO_CREATE);
        psIsBound = true;
    }

    private void doUnbindService() {
        if (psIsBound) {
            // Detach our existing connection.
            unbindService(mConnection);
            psIsBound = false;
        }
    }

    @Override
    public void onFragmentInteraction(Uri uri) {
    }

    /****************************** Action Center Fragment ****************************************/

    public View getView(int x) {
        return this.findViewById(x);
    }

    public void onImgReplaceChosen(Uri uri) {
        mPresenter.onLoadReplaceImg(uri, this);
    }

    @Override
    public void onJsPayloadApply(List<Pair<Integer, String>> payloads) {
        mPresenter.onJsPayloadApply(payloads);
    }

    public void onSwitchToggle(boolean on, String tag) {
        if (on)
            mPresenter.setSharedPrefsBool(tag, true);
        else
            mPresenter.setSharedPrefsBool(tag, false);
    }

    public void onTrafficRedirect(String traffic, boolean on) {
        mPresenter.onTrafficRedirect(traffic, on);
    }

    /********************************* Clients Fragment *******************************************/

    @Override
    public ArrayList<Client> getCurrentClients() {
        return mPresenter.getCurrentClients();
    }

    @Override
    public void setBan(Client c, boolean banned) {
        mPresenter.setBan(c, banned);
    }

    /********************************* Main Fragment **********************************************/

    @Override
    public boolean onApPressed(String SSID, String pass) {
        return mPresenter.apBtnPressed(SSID, pass, getApplicationContext());
    }

    @Override
    public boolean isApOn() {
        return mPresenter.isApOn(getApplicationContext());
    }

    /************************************** UI stuff***********************************************/

    public void enableTabLayout() {
        final TabLayout tabLayout = (TabLayout) findViewById(R.id.tab_layout);
        final CustomViewPager viewPager = (CustomViewPager) findViewById(R.id.pager);
        viewPager.setPagingEnabled(true);
        LinearLayout tabStrip = ((LinearLayout) tabLayout.getChildAt(0));
        for (int i = 0; i < tabStrip.getChildCount(); i++) {
            tabStrip.getChildAt(i).setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    return false;
                }
            });
        }
    }

    public void disableTabLayout() {
        final TabLayout tabLayout = (TabLayout) findViewById(R.id.tab_layout);
        final CustomViewPager viewPager = (CustomViewPager) findViewById(R.id.pager);
        viewPager.setPagingEnabled(false);
        LinearLayout tabStrip = ((LinearLayout) tabLayout.getChildAt(0));
        for (int i = 0; i < tabStrip.getChildCount(); i++) {
            tabStrip.getChildAt(i).setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    final Toast toast = Toast.makeText(getApplicationContext(), "AP must be ON!",
                            Toast.LENGTH_SHORT);
                    toast.show();
                    Handler handler = new Handler();
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            toast.cancel();
                        }
                    }, 500);
                    return true;
                }
            });
        }
    }

    private void setUpGUI() {
        final Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        final TabLayout tabLayout = (TabLayout) findViewById(R.id.tab_layout);
        tabLayout.addTab(tabLayout.newTab().setText("Config"));
        tabLayout.addTab(tabLayout.newTab().setText("Clients"));
        tabLayout.addTab(tabLayout.newTab().setText("Action Center"));
        tabLayout.setTabGravity(TabLayout.GRAVITY_FILL);
        final CustomViewPager viewPager = (CustomViewPager) findViewById(R.id.pager);
        final PagerAdapter adapter = new PagerAdapter
                        (getSupportFragmentManager(), tabLayout.getTabCount());
        viewPager.setAdapter(adapter);
        viewPager.addOnPageChangeListener(new TabLayout.TabLayoutOnPageChangeListener(tabLayout));
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                if (isApOn()) {
                    viewPager.setCurrentItem(tab.getPosition());
                    selectedTab = tab.getPosition();
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
            }

        });
        FrameLayout r = (FrameLayout) findViewById(R.id.activity_main);
        r.setBackground(ContextCompat.getDrawable(this, R.drawable.bground));
        disableTabLayout();
    }

    /************************************ MVP stuff ***********************************************/

    @Override
    public void showToast(String msg) {
        Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void dieUI() {
        finishAffinity();
    }

    public IMVP.PresenterOps getPresenter() {
        return mPresenter;
    }
    /**
     * Initialize and restart the Presenter.
     * This method should be called after {@link MainActivity#onCreate(Bundle)}
     */
    private void startMVPOps() {
        try {
            if (mStateMaintainer.firstTimeIn()) {
                Log.d(TAG, "onCreate() called for the first time");
                initialize(this);
            } else {
                Log.d(TAG, "onCreate() called more than once");
                reinitialize(this);
            }
        } catch (InstantiationException | IllegalAccessException e) {
            Log.d(TAG, "onCreate() " + e);
            throw new RuntimeException(e);
        }
    }
    /**
     * Initialize relevant MVP Objects.
     * Creates a Presenter instance, saves the presenter in {@link StateMaintainer}
     */
    private void initialize(IMVP.RequiredViewOps view) {
        mPresenter = new Presenter(view, this.getApplicationContext());
        mStateMaintainer.put(IMVP.PresenterOps.class.getSimpleName(), mPresenter);
    }
    /**
     * Recovers Presenter and informs Presenter that a config change occurred.
     * If Presenter has been lost, recreates an instance
     */
    private void reinitialize(IMVP.RequiredViewOps view)
            throws InstantiationException, IllegalAccessException {
        mPresenter = mStateMaintainer.get(IMVP.PresenterOps.class.getSimpleName());
        if (mPresenter == null) {
            Log.w(TAG, "recreating Presenter");
            initialize(view);
        } else {
            mPresenter.onConfigurationChanged(view);
        }
    }

}
