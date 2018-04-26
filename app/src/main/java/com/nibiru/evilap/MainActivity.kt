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
        viewPager.adapter = MyPagerAdapter(supportFragmentManager)
        viewPager.addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
            override fun onPageSelected(position: Int) {
                when(position){
                    0 -> supportActionBar?.setTitle("AP Mode")
                    1 -> supportActionBar?.setTitle("Client Mode")
                    else -> supportActionBar?.setTitle("AP Mode")
                }
            }
            override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {}
            override fun onPageScrollStateChanged(state: Int) {}
        })
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

    // Menu icons are inflated just as they were with actionbar
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.drawer_view, menu)
        return true
    }

    override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
        mService = (service as EvilApService.LocalBinder).service
    }

    override fun onServiceDisconnected(name: ComponentName?) {
        // Respect being stopped from notification action.
        finish()
    }

}