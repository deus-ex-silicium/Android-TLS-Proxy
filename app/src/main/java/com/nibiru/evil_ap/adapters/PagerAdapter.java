package com.nibiru.evil_ap.adapters;

/**
 * Created by Wobbaf on 14/10/2016.
 */

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;

import com.nibiru.evil_ap.fragments.ACFragment;
import com.nibiru.evil_ap.fragments.ACHTTPFragment;
import com.nibiru.evil_ap.fragments.ACHTTPSFragment;
import com.nibiru.evil_ap.fragments.ClientsFragment;
import com.nibiru.evil_ap.fragments.MainFragment;

public class PagerAdapter extends FragmentStatePagerAdapter {
    int mNumOfTabs;

    /**
     *
     * @param fm FragmentManager which helps manage child fragments
     * @param NumOfTabs Number of tabs in layout
     */
    public PagerAdapter(FragmentManager fm, int NumOfTabs) {
        super(fm);
        this.mNumOfTabs = NumOfTabs;
    }

    /**
     *
     * @param position Position of fragment on which adapter currently operates
     * @return That fragment
     */
    @Override
    public Fragment getItem(int position) {

        switch (position) {
            case 0:
                MainFragment tab1 = new MainFragment();
                return tab1;
            case 1:
                ClientsFragment tab2 = new ClientsFragment();
                return tab2;
            case 2:
                ACFragment tab3 = new ACFragment();
                ACHTTPFragment t4 = new ACHTTPFragment();
                ACHTTPSFragment t5 = new ACHTTPSFragment();
                return tab3;
            default:
                return null;
        }
    }

    @Override
    public int getCount() {
        return mNumOfTabs;
    }
}
