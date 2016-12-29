package com.nibiru.evil_ap;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.support.design.widget.TabLayout;
import android.support.v4.util.Pair;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.Toast;

import com.nibiru.evil_ap.fragments.ACFragment;
import com.nibiru.evil_ap.fragments.ACHTTPFragment;
import com.nibiru.evil_ap.fragments.ACHTTPSFragment;
import com.nibiru.evil_ap.fragments.ClientsFragment;
import com.nibiru.evil_ap.fragments.MainFragment;
import com.nibiru.evil_ap.fragments.ServerDetailsFragment;
import com.nibiru.evil_ap.fragments.ServerItemFragment;
import com.nibiru.evil_ap.log.Client;
import com.nibiru.evil_ap.proxy.ProxyService;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements
        MainFragment.OnMainFragmentInteraction,ClientsFragment.onClientsFragmentInteraction,
        ACFragment.OnFragmentInteractionListener, ACHTTPFragment.onAcFragmentInteraction,
        ACHTTPSFragment.onAcFragmentInteraction, ServerItemFragment.onClientsFragmentInteraction,
        ServerDetailsFragment.OnFragmentInteractionListener, IMVP.RequiredViewOps {
    /**************************************CLASS FIELDS********************************************/
    protected final String TAG = getClass().getSimpleName();
    private ProxyService.IProxyService mProxyService;
    private boolean psIsBound; //?
    private ServiceConnection mConnection; //?
    // Responsible for maintaining objects state during changing configuration
    public final StateMaintainer mStateMaintainer =
            new StateMaintainer( this.getFragmentManager(), TAG );
    // Presenter operations
    private IMVP.PresenterOps mPresenter;
    /**************************************CLASS METHODS*******************************************/

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        startMVPOps();
        setContentView(R.layout.activity_main);
        setUpGUI();
        mPresenter.checkIfDeviceRooted();

        startService(new Intent(this, ProxyService.class));
        mConnection = new ServiceConnection() {
            public void onServiceConnected(ComponentName className, IBinder service) {
                mProxyService = ((ProxyService.IProxyService)service);
                mProxyService.setSharedObj(mPresenter.getSharedObj());
            }
            public void onServiceDisconnected(ComponentName className) {
                mProxyService = null;
            }
        };
        doBindService();

        //reset some settings
        mPresenter.setSharedPrefsString(ConfigTags.imgPath.toString(),
                "android.resource://" + getPackageName() + "/" + R.raw.pixel_skull);
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
    public View getView(int x){ return this.findViewById(x); }

    public void onImgReplaceChosen(Uri uri){
        mPresenter.onLoadReplaceImg(uri, this);
    }

    @Override
    public void onJsPayloadApply(List<Pair<Integer, String>> payloads) {
        mPresenter.onJsPayloadApply(payloads);
    }

    public void onSwitchToggle(boolean on, String tag){
        if (on)
            mPresenter.setSharedPrefsBool(tag, true);
        else
            mPresenter.setSharedPrefsBool(tag, false);
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

    public IMVP.PresenterOps getPresenter(){
        return mPresenter;
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
        mPresenter = new Presenter(view, this.getApplicationContext());
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

    @Override
    public ArrayList<String> getClientServers() {
        return null;
    }
}
