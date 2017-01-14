package com.nibiru.evil_ap.adapters;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentPagerAdapter;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Wobbaf on 16/12/2016.
 */

public class ac_adapter extends FragmentPagerAdapter {
    private final List<Fragment> mFragments = new ArrayList<>();
    private final List<String> mFragmentTitles = new ArrayList<>();

    /**
     *
     * @param fm FragmentManager used to manage child fragments
     */
    public ac_adapter(android.support.v4.app.FragmentManager fm) {
        super(fm);
    }

    /**
     *
     * @param fragment Fragment which is added to parents tab layout
     * @param title Title of a fragment which is added
     */
    public void addFragment(Fragment fragment, String title) {
        mFragments.add(fragment);
        mFragmentTitles.add(title);
    }

    /**
     *
     * @param position Integer which is a position of fragment in parents tab layout
     * @return Fragment
     */
    @Override
    public Fragment getItem(int position) {
        return mFragments.get(position);
    }

    /**
     *
     * @return Count of fragments in parents tab layout
     */
    @Override
    public int getCount() {
        return mFragments.size();
    }

    /**
     *
     * @param position Integer which is a position of fragment in parents tab layout
     * @return Title of that fragment
     */
    @Override
    public CharSequence getPageTitle(int position) {
        return mFragmentTitles.get(position);
    }
}

