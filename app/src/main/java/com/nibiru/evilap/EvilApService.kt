package com.nibiru.evilap

import android.annotation.SuppressLint
import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import eu.chainfire.libsuperuser.Shell
import io.reactivex.disposables.Disposable


class EvilApService: Service() {
    /**************************************CLASS FIELDS********************************************/
    private val TAG = javaClass.simpleName
    private val NOTIFICATION_ID = 666
    private val NOTIFICATION_CHANNEL_ID = "evilap_notification_channel"
    enum class service(val action: String) {
        ACTION_STOP_SERVICE("com.nibiru.evilap.service_stop"),
        ACTION_SCAN_ACTIVE("com.nibiru.evilap.service_scan_active"),
        ACTION_DNS_SNIFF("com.nibiru.evilap.service_dns_sniff"),
    }
    private var mDisposable: Disposable? = null
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
        Log.d(TAG, "got intent = ${intent.action}")
        when(intent.action){
            service.ACTION_STOP_SERVICE.action -> exit()
        }
        setupEventBus()
        // If this service really do get killed, there is no point restarting it automatically
        return Service.START_NOT_STICKY
    }

    private fun setupEventBus(){
        if (mDisposable != null && !mDisposable!!.isDisposed) return

        mDisposable = RxEventBus.INSTANCE.busService.subscribe({
            Log.d(TAG, "got event = $it")
            when (it) {
                service.ACTION_STOP_SERVICE -> exit()
                service.ACTION_SCAN_ACTIVE -> startActiveScan("wlan0") //TODO: check wifi connectivity
                service.ACTION_DNS_SNIFF -> startDnsSniff("wlan0")
            }
        })
    }

    private fun buildNotification(): Notification {
        val notifyIntent = Intent(this, MainActivity::class.java)
        // PendingIntent#getActivity(): "Note that the activity will be started outside of the context of an existing
        // activity, so you must use the Intent.FLAG_ACTIVITY_NEW_TASK launch flag in the Intent":
        notifyIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        val pendingIntent = PendingIntent.getActivity(this, 0, notifyIntent, 0)
        val exitIntent = Intent(this, EvilApService::class.java)
                .setAction(service.ACTION_STOP_SERVICE.action)
        val builder = Notification.Builder(this)
        builder.setContentTitle(getText(R.string.app_name))
                .setContentText("${mShells.size} active shells")
                .setSmallIcon(R.drawable.ic_evilap)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .setShowWhen(false) // No need to show a timestamp
                .setColor(-0x1000000) // Background color for small notification ic_evilap
                .addAction(R.drawable.ic_exit_black_24dp, resources.getString(R.string.notification_action_exit),
                        PendingIntent.getService(this, 0, exitIntent, 0))
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder.setChannelId(NOTIFICATION_CHANNEL_ID)
        }
        return builder.build()
    }

    @SuppressLint("WrongConstant")
    private fun setupNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val importance = android.app.NotificationManager.IMPORTANCE_LOW
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

    private fun exit(){
        mWantsToStop = true
        if (mDisposable!=null && !mDisposable!!.isDisposed) mDisposable!!.dispose()
        getIdleShell().addCommand("pkill -f ${applicationInfo.dataDir}/lib/")
        for (shell in mShells)
            shell.close()
        stopSelf()
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
        mShells.add(shell)
        updateNotification()
        return shell
    }

    private fun getIdleShell(): Shell.Interactive {
        for(shell in mShells){
            if(shell.isIdle) return shell
            if(!shell.isRunning) {
                mShells.remove(shell)
                updateNotification()
            }
        }
        return openRootShell()
    }

    private fun startActiveScan(iface: String) {
        val shell = getIdleShell()
        val path = applicationInfo.dataDir
        val cmd = "LD_LIBRARY_PATH=$path/lib/ $path/lib/libscanner.so wlan0 active-arp"
        shell.addCommand(cmd, 0, object : Shell.OnCommandLineListener {
                    override fun onCommandResult(commandCode: Int, exitCode: Int) {
                        Log.i("[native]SCANNER", "$cmd \n(exit code: $exitCode)")
                    }
                    override fun onLine(line: String) {
                        Log.d("[native]SCANNER", line)
                        if(!line.contains("=>")) return
                        val elements = line.split("=>")
                        RxEventBus.INSTANCE.send(Host(elements[0],elements[1],true))
                    }
                })
    }

    private fun startDnsSniff(iface: String) {
        val whitelist = listOf("wlan0")
        if(!whitelist.contains(iface)){
            Log.e(TAG, "startDnsSniff: bad interface!")
            return
        }
        val shell = getIdleShell()
        val path = applicationInfo.dataDir
        val cmd = "LD_LIBRARY_PATH=$path/lib/ $path/lib/libdnssniff.so $iface"
        shell.addCommand(cmd, 0, object : Shell.OnCommandLineListener {
            override fun onCommandResult(commandCode: Int, exitCode: Int) {
                Log.i("ROOT", "$cmd \n(exit code: $exitCode)")
            }
            override fun onLine(line: String) {
                Log.d("ROOT", line)
            }
        })

    }

    data class Host(val ip: String, val mac: String, val present: Boolean)
}