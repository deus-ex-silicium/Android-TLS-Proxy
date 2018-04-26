package com.nibiru.evilap

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.util.Patterns
import eu.chainfire.libsuperuser.Shell
import android.app.NotificationManager




class EvilApService: Service() {
    /**************************************CLASS FIELDS********************************************/
    private val TAG = javaClass.simpleName
    private val NOTIFICATION_ID = 666
    private val NOTIFICATION_CHANNEL_ID = "evilap_notification_channel"
    companion object {
        const val ACTION_STOP_SERVICE = "com.nibiru.evilap.service_stop"
        const val ACTION_PING_SWEEP = "com.nibiru.evilap.service_ping_sweep"
    }
    private var mShells: MutableList<Shell.Interactive> = ArrayList()
    var mWantsToStop = false
    // This service is only bound from inside the same process and never uses IPC.
    internal inner class LocalBinder : Binder() {
        val service = this@EvilApService
    }
    private val mBinder = LocalBinder()
    /**************************************CLASS METHODS*******************************************/
    override fun onCreate() {
        setupNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification())
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        val action = intent.action
        Log.d(TAG, "got action = $action")
        when(action) {
            ACTION_STOP_SERVICE -> {
                mWantsToStop = true
                for (shell in mShells)
                    shell.kill()
                stopSelf()
            }
            ACTION_PING_SWEEP -> {
                startShellScanActive("192.168.0.1")
            }
            else -> {
                Log.e(TAG, "Unknown EvilApService action: '$action'")
            }
        }
        // If this service really do get killed, there is no point restarting it automatically
        return Service.START_NOT_STICKY
    }

    private fun buildNotification(): Notification {
        val notifyIntent = Intent(this, MainActivity::class.java)
        // PendingIntent#getActivity(): "Note that the activity will be started outside of the context of an existing
        // activity, so you must use the Intent.FLAG_ACTIVITY_NEW_TASK launch flag in the Intent":
        notifyIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        val pendingIntent = PendingIntent.getActivity(this, 0, notifyIntent, 0)
        val builder = Notification.Builder(this)
        builder.setContentTitle(getText(R.string.app_name))
        builder.setContentText("${mShells.size} shell")
        builder.setSmallIcon(R.drawable.notification_icon_background)
        builder.setContentIntent(pendingIntent)
        builder.setOngoing(true)
        builder.setPriority(Notification.PRIORITY_LOW)
        builder.setShowWhen(false) // No need to show a timestamp
        builder.setColor(-0x1000000) // Background color for small notification icon
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder.setChannelId(NOTIFICATION_CHANNEL_ID)
        }
        val exitIntent = Intent(this, EvilApService::class.java).setAction(ACTION_STOP_SERVICE)
        builder.addAction(android.R.drawable.ic_delete, resources.getString(R.string.notification_action_exit),
                PendingIntent.getService(this, 0, exitIntent, 0))
        return builder.build()
    }

    private fun setupNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val importance = NotificationManager.IMPORTANCE_LOW
        val channel = NotificationChannel(NOTIFICATION_CHANNEL_ID, "Evil-AP", importance)
        channel.description = "Notifications from Evil-AP"
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)
    }

    private fun updateNotification() {
        // Update the shown foreground service notification after making any changes that affect it.
        (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
                .notify(NOTIFICATION_ID, buildNotification())
    }

    override fun onBind(intent: Intent?): IBinder = mBinder

    private fun startShellScanActive(ip: String) {
        if (!Patterns.IP_ADDRESS.matcher(ip).matches()){
            Log.e(TAG, "BAD IP!")
            return
        }
        val shell = openRootShell()
        val path = applicationInfo.dataDir
        val cmd = "LD_LIBRARY_PATH=$path/lib/ $path/lib/libscanactive.so $ip"
        shell?.addCommand(cmd, 0, object : Shell.OnCommandLineListener {
                    override fun onCommandResult(commandCode: Int, exitCode: Int) {
                        Log.i("ROOT", "$cmd \n(exit code: $exitCode)")
                    }
                    override fun onLine(line: String) {
                        Log.d("ROOT", line)
                    }
                })
    }

    private fun openRootShell(): Shell.Interactive {
        // start the shell in the background and keep it alive as long as the app is running
        val shell = Shell.Builder().useSU().setWantSTDERR(true)
                .setWatchdogTimeout(0).setMinimalLogging(true).open { commandCode, exitCode, output ->
            // Callback to report whether the shell was successfully started up
            if (exitCode != Shell.OnCommandResultListener.SHELL_RUNNING) {
                Log.e(TAG,"Error opening root shell: exitCode=$exitCode")
            }
            else {
                Log.d(TAG,"Root shell opened")
            }
        }
        if(shell != null && shell.isRunning){
            mShells.add(shell)
            updateNotification()
        }
        return shell
    }

}