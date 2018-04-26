package com.nibiru.evilap

import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentPagerAdapter
import android.support.v4.view.ViewPager
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.Menu
import com.nibiru.evilap.fragments.FragmentApMode
import com.nibiru.evilap.fragments.FragmentClientMode
import kotlinx.android.synthetic.main.activity_main.*
import android.support.v4.view.GravityCompat
import android.view.MenuItem
import android.support.design.widget.NavigationView
import com.nibiru.evilap.R.id.ap_mode
import com.nibiru.evilap.R.id.client_mode


// https://developer.android.com/training/appbar/
// https://developer.android.com/training/implementing-navigation/nav-drawer
// https://guides.codepath.com/android/Using-the-App-ToolBar
// https://stackoverflow.com/questions/18413309/how-to-implement-a-viewpager-with-different-fragments-layouts/18413437#18413437
class MainActivity : AppCompatActivity(), ServiceConnection {
    /**************************************CLASS FIELDS********************************************/
    private val TAG = javaClass.simpleName
    private var mService: EvilApService? = null
    private val mCurrentContent: android.support.v4.app.Fragment? = null
    /**************************************CLASS METHODS*******************************************/
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(my_toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeAsUpIndicator(R.drawable.ic_menu);
        supportActionBar?.title = "AP Mode" //Set default title
        viewPager.adapter = MyPagerAdapter(supportFragmentManager)
        viewPager.addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
            override fun onPageSelected(position: Int) = when(position){
                    0 -> supportActionBar?.title = "AP Mode"
                    1 -> supportActionBar?.title = "Client Mode"
                    else -> supportActionBar?.title = "AP Mode"
            }
            override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {}
            override fun onPageScrollStateChanged(state: Int) {}
        })
        nav_view.setNavigationItemSelectedListener { menuItem ->
            menuItem.isChecked = true // set item as selected to persist highlight
            drawer_layout.closeDrawers() // close drawer when item is tapped
            when(menuItem.itemId){
                ap_mode -> viewPager.currentItem = 0
                client_mode -> viewPager.currentItem = 1
            }
            true
        }

        //mCurrentContent = if(savedInstanceState!=null)
        // supportFragmentManager.getFragment(savedInstanceState, "mCurrentContent") else

        // Start the service and make it run regardless of who is bound to it
        val serviceIntent = Intent(this, EvilApService::class.java)
        startService(serviceIntent)
        if (!bindService(serviceIntent, this, 0))
            throw RuntimeException("bindService() failed")
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy")
        unbindService(this)
    };

    private inner class MyPagerAdapter(fm: FragmentManager) : FragmentPagerAdapter(fm) {
        override fun getItem(pos: Int): Fragment {
            when (pos) {
                0 -> return FragmentApMode.newInstance()
                1 -> return FragmentClientMode.newInstance()
                else -> return FragmentApMode.newInstance()
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
        mService = (service as EvilApService.LocalBinder).service
    }

    override fun onServiceDisconnected(name: ComponentName?) {
        // Respect being stopped from notification action.
        finish()
    }

}