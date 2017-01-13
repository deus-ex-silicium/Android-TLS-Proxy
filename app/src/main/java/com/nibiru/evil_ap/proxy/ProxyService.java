package com.nibiru.evil_ap.proxy;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Binder;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;

import com.nibiru.evil_ap.MainActivity;
import com.nibiru.evil_ap.R;
import com.nibiru.evil_ap.SharedClass;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Created by Nibiru on 2016-10-14.
 */

public class ProxyService extends Service{
    /**************************************CLASS FIELDS********************************************/
    protected final String TAG = getClass().getSimpleName();
    public volatile boolean work;
    public SharedClass mSharedObj;
    // Configuration settings
    public SharedPreferences config;
    /**************************************CLASS METHODS*******************************************/
    /**
     * Class for clients to access.  Because we know this service always
     * runs in the same process as its clients, we don't need to deal with
     * IPC.
     */
    public class LocalBinder extends Binder implements IProxyService{
        @Override
        public void setSharedObj(SharedClass sharedObj) {
            mSharedObj = sharedObj;
        }
    }
    // This is the object that receives interactions from clients.  See
    // RemoteService for a more complete example.
    private final IBinder mBinder = new LocalBinder();

    /*********************************************************************************************/
    @Override
    public void onCreate() {
        // Start up the thread running the service.  Note that we create a separate thread because
        // the service normally runs in the process's main thread, which we don't want to block.
        work = true;
        config = getSharedPreferences("Config", 0);
        //start the HTTP proxy socket thread
        Thread proxyHTTP = new Thread(new ProxyHTTPMainLoop(this));
        proxyHTTP.start();
        //start the HTTPS proxy socket thread
        Thread proxyHTTPS = new Thread(new ProxyHTTPSMainLoop(
                getResources().openRawResource(R.raw.evil_ap), this));
        proxyHTTPS.start();
        setupNotification();
    }
    @Override
    public void onDestroy(){
        work = false;
        cancelNotification(getApplicationContext(), 1);
    }
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }
    @Override
    public void onTaskRemoved(Intent rootIntent) {
        cancelNotification(getApplicationContext(),1);
        stopSelf();
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
                PendingIntent.getActivity(this, 1, intentShow, Intent.FLAG_ACTIVITY_CLEAR_TOP);
        NotificationCompat.Action actionOFF = new NotificationCompat.Action.Builder(0,
                "Toggle AP", pi).build();
        NotificationCompat.Action actionSHOW = new NotificationCompat.Action.Builder(1, "Bring to" +
                " front", contentIntent).build();
        NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext
                ());
        builder.setContentTitle("Your Proxy is running");
        builder.setTicker("Evil-AP Notification");
        builder.setSmallIcon(R.drawable.onoffon);
        builder.setLargeIcon(bm);
        builder.setAutoCancel(true);
        builder.setOngoing(true);
        builder.addAction(actionOFF);
        builder.addAction(actionSHOW);
        Notification notification =
                builder.setVisibility(NotificationCompat.VISIBILITY_PUBLIC).build();
        NotificationManager notificationManger =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManger.notify(1, notification);
    }

    public static void cancelNotification(Context ctx, int notifyId) {
        String ns = Context.NOTIFICATION_SERVICE;
        NotificationManager nMgr = (NotificationManager) ctx.getSystemService(ns);
        nMgr.cancel(notifyId);
    }

    public interface IProxyService {
        void setSharedObj(SharedClass sharedObj);
    }

    private void testImgStream() {
        InputStream inFromServer = getResources().openRawResource(R.raw.pixel_skull);
        byte[] buffer = new byte[0];
        try {
            File targetFile = new File(this.getApplicationContext().getFilesDir(), "test.tmp");
            OutputStream outStream = new FileOutputStream(targetFile);

            int bytes_read;
            byte[] reply = new byte[4096];
            try {
                while ((bytes_read = inFromServer.read(reply)) != -1) {
                    outStream.write(reply, 0, bytes_read);
                    outStream.flush();
                    //TODO CREATE YOUR LOGIC HERE
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

}
