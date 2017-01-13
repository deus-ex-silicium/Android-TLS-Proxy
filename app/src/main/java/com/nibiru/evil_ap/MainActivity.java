package com.nibiru.evil_ap;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Icon;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.support.design.widget.TabLayout;
import android.support.v4.app.NotificationCompat;
import android.support.v4.util.Pair;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.google.android.gms.appindexing.Action;
import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.appindexing.Thing;
import com.google.android.gms.common.api.GoogleApiClient;
import com.nibiru.evil_ap.adapters.PagerAdapter;
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
        MainFragment.OnMainFragmentInteraction, ClientsFragment.onClientsFragmentInteraction,
        ACFragment.OnFragmentInteractionListener, ACHTTPFragment.onAcFragmentInteraction,
        ACHTTPSFragment.onAcFragmentInteraction, ServerItemFragment.onClientsFragmentInteraction,
        ServerDetailsFragment.OnFragmentInteractionListener, IMVP.RequiredViewOps {
    /**************************************
     * CLASS FIELDS
     ********************************************/
    protected final String TAG = getClass().getSimpleName();
    private ProxyService.IProxyService mProxyService;
    private boolean psIsBound; //?
    private ServiceConnection mConnection; //?
    // Responsible for maintaining objects state during changing configuration
    public final StateMaintainer mStateMaintainer =
            new StateMaintainer(this.getFragmentManager(), TAG);
    // Presenter operations
    private IMVP.PresenterOps mPresenter;
    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */
    private GoogleApiClient client;

    /**************************************
     * CLASS METHODS
     *******************************************/

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        startMVPOps();
        setContentView(R.layout.activity_main);
        setUpGUI();
        mPresenter.checkIfDeviceRooted();
        IntentFilter filter = new IntentFilter();
        filter.addAction("tap");
        BroadcastReceiver receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.getAction().equals("tap")) {
                    Log.e("Intent", "inside");
                    onApPressed("", "");
                }
            }
        };
        registerReceiver(receiver, filter);
        startService(new Intent(this, ProxyService.class));
        mConnection = new ServiceConnection() {
            public void onServiceConnected(ComponentName className, IBinder service) {
                mProxyService = ((ProxyService.IProxyService) service);
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
        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client = new GoogleApiClient.Builder(this).addApi(AppIndex.API).build();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        doUnbindService();
    }

    void setupNotification() {
        Bitmap bm = Bitmap.createScaledBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.onoffon),
                getResources().getDimensionPixelSize(android.R.dimen.notification_large_icon_width),
                getResources().getDimensionPixelSize(android.R.dimen.notification_large_icon_height),
                true);
        Intent intent = new Intent("tap");
        Intent intentShow = new Intent(this, MainActivity.class);
        PendingIntent pi = PendingIntent.getBroadcast(this, 0, intent, 0);
        PendingIntent contentIntent =
                PendingIntent.getActivity(this, 1, intentShow, Intent
                        .FLAG_ACTIVITY_CLEAR_TOP);
        NotificationCompat.Action actionOFF = new NotificationCompat.Action.Builder(0,
                "Turn AP off", pi).build();
        NotificationCompat.Action actionSHOW = new NotificationCompat.Action.Builder(1, "Bring to" +
                " front", contentIntent).build();
        NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext
                ());
        builder.setContentTitle("Your AP is on.");
        builder.setTicker("Evil-AP Notification");
        builder.setSmallIcon(R.drawable.onoffon);
        builder.setLargeIcon(bm);
        builder.setAutoCancel(true);
        builder.setOngoing(true);
        builder.addAction(actionOFF);
        builder.addAction(actionSHOW);
        Notification notification = builder
                .setVisibility
                        (NotificationCompat
                                .VISIBILITY_PUBLIC)
                .build();
        NotificationManager notificationManger =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManger.notify(1, notification);
    }

    public static void cancelNotification(Context ctx, int notifyId) {
        String ns = Context.NOTIFICATION_SERVICE;
        NotificationManager nMgr = (NotificationManager) ctx.getSystemService(ns);
        nMgr.cancel(notifyId);
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
    public void onFragmentInteraction(Uri uri) {
    }

    /********************************
     * Action Center Fragment
     ****************************************/
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

    /**********************************
     * Clients Fragment
     ********************************************/
    @Override
    public ArrayList<Client> getCurrentClients() {
        return mPresenter.getCurrentClients();
    }

    /***********************************
     * Main Fragment
     *********************************************/
    @Override
    public boolean onApPressed(String SSID, String pass) {
        if (!isApOn()) {
            setupNotification();
            enableTabLayout();
        } else {
            cancelNotification(getApplicationContext(), 1);
            disableTabLayout();
        }
        return mPresenter.apBtnPressed(SSID, pass, getApplicationContext());
    }

    @Override
    public boolean isApOn() {
        return mPresenter.isApOn(getApplicationContext());
    }

    /**************************************
     * UI stuff
     ************************************************/

    private void enableTabLayout() {
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

    private void disableTabLayout() {
        final TabLayout tabLayout = (TabLayout) findViewById(R.id.tab_layout);
        final CustomViewPager viewPager = (CustomViewPager) findViewById(R.id.pager);
        viewPager.setPagingEnabled(false);
        LinearLayout tabStrip = ((LinearLayout) tabLayout.getChildAt(0));
        for (int i = 0; i < tabStrip.getChildCount(); i++) {
            tabStrip.getChildAt(i).setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    Toast.makeText(getApplicationContext(), "AP must be ON!", Toast.LENGTH_SHORT).show();
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
        final PagerAdapter adapter =
                new PagerAdapter
                        (getSupportFragmentManager(), tabLayout.getTabCount());
        viewPager.setAdapter(adapter);
        viewPager.addOnPageChangeListener(new TabLayout.TabLayoutOnPageChangeListener(tabLayout));
        disableTabLayout();
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                if (isApOn()) {
                    viewPager.setCurrentItem(tab.getPosition());
                } else {
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
        r.setBackground((getResources().getDrawable(R.drawable.bground)));
    }

    /*************************************
     * MVP stuff
     ***********************************************/
    @Override
    public void showToast(String msg) {
        Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT).show();
    }

    public IMVP.PresenterOps getPresenter() {
        return mPresenter;
    }

    /**
     * Initialize and restart the Presenter.
     * This method should be called after {@link MainActivity#onCreate(Bundle)}
     */
    public void startMVPOps() {
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
    private void initialize(IMVP.RequiredViewOps view)
            throws InstantiationException, IllegalAccessException {
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

    @Override
    public ArrayList<String> getClientServers() {
        return null;
    }

    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */
    public Action getIndexApiAction() {
        Thing object = new Thing.Builder()
                .setName("Main Page") // TODO: Define a title for the content shown.
                // TODO: Make sure this auto-generated URL is correct.
                .setUrl(Uri.parse("http://[ENTER-YOUR-URL-HERE]"))
                .build();
        return new Action.Builder(Action.TYPE_VIEW)
                .setObject(object)
                .setActionStatus(Action.STATUS_TYPE_COMPLETED)
                .build();
    }

    @Override
    public void onStart() {
        super.onStart();

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client.connect();
        AppIndex.AppIndexApi.start(client, getIndexApiAction());
    }

    @Override
    public void onStop() {
        super.onStop();

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        AppIndex.AppIndexApi.end(client, getIndexApiAction());
        client.disconnect();
    }
}
