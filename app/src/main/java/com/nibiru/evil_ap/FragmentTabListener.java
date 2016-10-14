package com.nibiru.evil_ap;

/**
 * Created by Wobbaf on 14/10/2016.
 */


import android.app.ActionBar.Tab;
import android.app.ActionBar.TabListener;
import android.app.Fragment;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.app.FragmentTransaction;

public class FragmentTabListener<T extends Fragment> implements TabListener {
    private Fragment mFragment;
    private final FragmentActivity mActivity;
    private final String mTag;
    private final Class<T> mClass;
    private final int mfragmentContainerId;
    private final Bundle mfragmentArgs;

    // This version defaults to replacing the entire activity content area
    // new FragmentTabListener<SomeFragment>(this, "first", SomeFragment.class))
    public FragmentTabListener(FragmentActivity activity, String tag, Class<T> clz) {
        mActivity = activity;
        mTag = tag;
        mClass = clz;
        mfragmentContainerId = android.R.id.content;
        mfragmentArgs = new Bundle();
    }

    // This version supports specifying the container to replace with fragment content
    // new FragmentTabListener<SomeFragment>(R.id.flContent, this, "first", SomeFragment.class))
    public FragmentTabListener(int fragmentContainerId, FragmentActivity activity,
                               String tag, Class<T> clz) {
        mActivity = activity;
        mTag = tag;
        mClass = clz;
        mfragmentContainerId = fragmentContainerId;
        mfragmentArgs = new Bundle();
    }

    // This version supports specifying the container to replace with fragment content and fragment args
    // new FragmentTabListener<SomeFragment>(R.id.flContent, this, "first", SomeFragment.class, myFragmentArgs))
    public FragmentTabListener(int fragmentContainerId, FragmentActivity activity,
                               String tag, Class<T> clz, Bundle args) {
        mActivity = activity;
        mTag = tag;
        mClass = clz;
        mfragmentContainerId = fragmentContainerId;
        mfragmentArgs = args;
    }

    /* The following are each of the ActionBar.TabListener callbacks */

    public void onTabSelected(Tab tab, android.app.FragmentTransaction ft) {
        FragmentTransaction sft = mActivity.getFragmentManager().beginTransaction();
        // Check if the fragment is already initialized
        if (mFragment == null) {
            // If not, instantiate and add it to the activity

            mFragment = Fragment.instantiate(mActivity, mClass.getName(), mfragmentArgs);
            sft.add(mfragmentContainerId, mFragment, mTag);
        } else {
            // If it exists, simply attach it in order to show it
            sft.attach(mFragment);
        }
        sft.commit();
    }

    public void onTabUnselected(Tab tab, android.app.FragmentTransaction ft) {
        FragmentTransaction sft = mActivity.getFragmentManager().beginTransaction();
        if (mFragment != null) {
            // Detach the fragment, because another one is being attached
            sft.detach(mFragment);
        }
        sft.commit();
    }

    public void onTabReselected(Tab tab, android.app.FragmentTransaction ft) {
        // User selected the already selected tab. Usually do nothing.
    }
}
