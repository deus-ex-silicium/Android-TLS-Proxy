package com.nibiru.evil_ap;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.support.design.widget.TabLayout;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.widget.FrameLayout;
import android.widget.Toast;

import com.nibiru.evil_ap.fragments.ACFragment;
import com.nibiru.evil_ap.fragments.ACHTTPFragment;
import com.nibiru.evil_ap.fragments.ACHTTPSFragment;
import com.nibiru.evil_ap.fragments.ClientsFragment;
import com.nibiru.evil_ap.fragments.MainFragment;
import com.nibiru.evil_ap.log.Client;
import com.nibiru.evil_ap.proxy.ProxyService;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements
        MainFragment.OnMainFragmentInteraction,ClientsFragment.onClientsFragmentInteraction,
        ACFragment.OnFragmentInteractionListener, ACHTTPFragment.onAcFragmentInteraction,
        ACHTTPSFragment.onAcFragmentInteraction, IMVP.RequiredViewOps {
    /**************************************CLASS FIELDS********************************************/
    protected final String TAG = getClass().getSimpleName();
    public ProxyService proxyService; //?
    private boolean psIsBound; //?
    private ServiceConnection mConnection; //?
    // Responsible for maintaining objects state during changing configuration
    public final StateMaintainer mStateMaintainer =
            new StateMaintainer( this.getFragmentManager(), TAG );
    // Presenter operations
    private IMVP.PresenterOps mPresenter;
    private SharedPreferences mConfig;
    /**************************************CLASS METHODS*******************************************/

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        startMVPOps();
        setContentView(R.layout.activity_main);
        setUpGUI();
        mConfig = getSharedPreferences("Config", 0);
        mPresenter.checkIfDeviceRooted();

        //TODO: refactor
        startService(new Intent(this, ProxyService.class));
        mConnection = new ServiceConnection() {
            public void onServiceConnected(ComponentName className, IBinder service) {
                proxyService = ((ProxyService.LocalBinder)service).getService();
            }
            public void onServiceDisconnected(ComponentName className) {
                proxyService = null;
            }
        };
        doBindService();

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        doUnbindService();
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
    /********************************Action Center Fragment****************************************/
    public void onSslStripToggle(){
        if (!mConfig.getBoolean("sslStrip", false))
            mConfig.edit().putBoolean("sslStrip", true).apply();
        else
            mConfig.edit().putBoolean("sslStrip", false).apply();
    }

    public void onTrafficRedirect(String traffic, boolean on) {
        mPresenter.onTrafficRedirect(traffic, on);
    }
    /**********************************Clients Fragment********************************************/
    @Override
    public ArrayList<Client> getCurrentClients() {
        return mPresenter.getCurrentClients();
    }
    /***********************************Main Fragment *********************************************/
    @Override
    public boolean onApPressed( String SSID, String pass ) {
        return mPresenter.apBtnPressed( SSID, pass, getApplicationContext() );
    }

    @Override
    public boolean isApOn() {
        return mPresenter.isApOn(getApplicationContext());
    }

    /**************************************UI stuff************************************************/
    private void setUpGUI(){
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        TabLayout tabLayout = (TabLayout) findViewById(R.id.tab_layout);
        tabLayout.addTab(tabLayout.newTab().setText("Config"));
        tabLayout.addTab(tabLayout.newTab().setText("Clients"));
        tabLayout.addTab(tabLayout.newTab().setText("Action Center"));
        tabLayout.setTabGravity(TabLayout.GRAVITY_FILL);

        final ViewPager viewPager = (ViewPager) findViewById(R.id.pager);
        final com.nibiru.evil_ap.adapters.PagerAdapter adapter =
                new com.nibiru.evil_ap.adapters.PagerAdapter
                        (getSupportFragmentManager(), tabLayout.getTabCount());
        viewPager.setAdapter(adapter);
        viewPager.addOnPageChangeListener(new TabLayout.TabLayoutOnPageChangeListener(tabLayout));
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                viewPager.setCurrentItem(tab.getPosition());
            }
            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}
            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });
        FrameLayout r = (FrameLayout)findViewById(R.id.activity_main);
        r.setBackground((getResources().getDrawable(R.drawable.bground)));
    }
    /*************************************MVP stuff ***********************************************/
    @Override
    public void showToast(String msg) {
        Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT).show();
    }

    /**
     * Initialize and restart the Presenter.
     * This method should be called after {@link MainActivity#onCreate(Bundle)}
     */
    public void startMVPOps() {
        try {
            if ( mStateMaintainer.firstTimeIn() ) {
                Log.d(TAG, "onCreate() called for the first time");
                initialize(this);
            } else {
                Log.d(TAG, "onCreate() called more than once");
                reinitialize(this);
            }
        } catch ( InstantiationException | IllegalAccessException e ) {
            Log.d(TAG, "onCreate() " + e );
            throw new RuntimeException( e );
        }
    }

    /**
     * Initialize relevant MVP Objects.
     * Creates a Presenter instance, saves the presenter in {@link StateMaintainer}
     */
    private void initialize( IMVP.RequiredViewOps view )
            throws InstantiationException, IllegalAccessException{
        mPresenter = new Presenter(view);
        mStateMaintainer.put(IMVP.PresenterOps.class.getSimpleName(), mPresenter);
    }

    /**
     * Recovers Presenter and informs Presenter that a config change occurred.
     * If Presenter has been lost, recreates an instance
     */
    private void reinitialize( IMVP.RequiredViewOps view )
            throws InstantiationException, IllegalAccessException {
        mPresenter = mStateMaintainer.get( IMVP.PresenterOps.class.getSimpleName() );
        if ( mPresenter == null ) {
            Log.w(TAG, "recreating Presenter");
            initialize( view );
        } else {
            mPresenter.onConfigurationChanged( view );
        }
    }

}
