package com.nibiru.evilap

import android.content.*
import android.net.ConnectivityManager
import android.os.Bundle
import android.os.IBinder
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentPagerAdapter
import android.support.v4.view.GravityCompat
import android.support.v4.view.ViewPager
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.MenuItem
import android.view.View
import com.nibiru.evilap.R.id.*
import com.nibiru.evilap.ui.FragmentActionCenter
import com.nibiru.evilap.ui.FragmentApMode
import com.nibiru.evilap.ui.FragmentNetwork
import com.nibiru.evilap.proxy.ProxyService
import kotlinx.android.synthetic.main.activity_main.*


// https://developer.android.com/training/appbar/
// https://developer.android.com/training/implementing-navigation/nav-drawer
// https://guides.codepath.com/android/Using-the-App-ToolBar
// https://stackoverflow.com/questions/18413309/how-to-implement-a-viewpager-with-different-fragments-layouts/18413437#18413437
// https://developer.android.com/training/animation/screen-slide
// https://www.raywenderlich.com/169885/android-fragments-tutorial-introduction-2
class MainActivity : AppCompatActivity(), ServiceConnection,
        FragmentApMode.OnFragmentInteractionListener,
        FragmentNetwork.OnFragmentInteractionListener,
        FragmentActionCenter.OnFragmentInteractionListener{
    /**************************************CLASS FIELDS********************************************/
    private val TAG = javaClass.simpleName
    private var mService: EvilApService? = null
    private val mCurrentContent: android.support.v4.app.Fragment? = null
    private val connectivityChangeReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            Log.d(TAG, "Got intent, action = " + intent?.action)
            updateUiAlerts()
        }
    }
    /**************************************CLASS METHODS*******************************************/
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(my_toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeAsUpIndicator(R.drawable.ic_menu_24dp)
        viewPager.adapter = MyPagerAdapter(supportFragmentManager)
        viewPager.addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
            override fun onPageSelected(position: Int) = when(position){
                //0 -> supportActionBar?.title = "AP Mode"
                0 -> supportActionBar?.title = "Network"
                //2 -> supportActionBar?.title = "Scanner"
                1 -> supportActionBar?.title = "Action Center"
                else -> supportActionBar?.title = "Network"
            }
            override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {}
            override fun onPageScrollStateChanged(state: Int) {}
        })
        nav_view.setNavigationItemSelectedListener { menuItem ->
            menuItem.isChecked = true // set item as selected to persist highlight
            drawer_layout.closeDrawers() // close drawer when item is tapped
            when(menuItem.itemId){
                //ap_mode -> viewPager.currentItem = 0
                network -> viewPager.currentItem = 0
                //scanner -> viewPager.currentItem = 2
                action_center -> viewPager.currentItem = 1
            }
            true
        }

        updateUiAlerts()

        // Start the service and make it run regardless of who is bound to it
        val evilAPServiceIntent = Intent(this, EvilApService::class.java)
        startService(evilAPServiceIntent)
        if (!bindService(evilAPServiceIntent, this, 0))
            throw RuntimeException("bindService() failed")
        val proxyServiceIntent = Intent(this, ProxyService::class.java)
        startService(proxyServiceIntent)
    }

    override fun onResume() {
        Log.d(TAG, "onResume")
        registerReceiver(connectivityChangeReceiver, IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION))
        super.onResume()
    }

    override fun onPause() {
        Log.d(TAG, "onPause")
        unregisterReceiver(connectivityChangeReceiver)
        super.onPause()
    }

    override fun onDestroy() {
        Log.d(TAG, "onDestroy")
        unbindService(this)
        super.onDestroy()
    }

    private inner class MyPagerAdapter(fm: FragmentManager) : FragmentPagerAdapter(fm) {
        override fun getItem(pos: Int): Fragment {
            return when (pos) {
                0 -> FragmentNetwork.newInstance()
                1 -> FragmentActionCenter.newInstance()
                else -> FragmentApMode.newInstance()
            }
        }
        override fun getCount(): Int = 2
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean =  when(item.itemId) {
            android.R.id.home -> {
                drawer_layout.openDrawer(GravityCompat.START)
                true
            }
            else -> super.onOptionsItemSelected(item)
    }

    override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
        if (service is EvilApService.LocalBinder) {mService = service.service}
        else {Log.e(TAG, "onServiceConnected, wrong interface")}
    }

    override fun onServiceDisconnected(name: ComponentName?) {
        // Respect being stopped from notification action.
        finish()
    }

    private fun updateUiAlerts(){
        if (!TLSProxyApp.instance.wifiConnected){
            tvWifiOff.visibility = View.VISIBLE
        }
        else{
            tvWifiOff.visibility = View.GONE
        }
        if (!TLSProxyApp.instance.internetConnected){
            tvInternetOff.visibility = View.VISIBLE
        }
        else{
            tvInternetOff.visibility = View.GONE
        }
    }

    /******************************** Fragment Stuff **********************************************/

}