package com.nibiru.evil_ap;

import android.support.v4.app.Fragment;

import com.nibiru.evil_ap.fragments.ACFragment;
import com.nibiru.evil_ap.fragments.ClientsFragment;
import com.nibiru.evil_ap.fragments.MainFragment;

import org.junit.Test;
import org.junit.runner.RunWith;

import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;

/**
 * Created by Wobbaf on 18/12/2016.
 */

public class ViewTest {
    IMVP.PresenterOps p;
    MainActivity mainActivity;
    MainFragment mainFragment;
    ACFragment acFragment;
    ClientsFragment clientsFragment;

    @Test
    public void mainActivityTest(){
        assertNull(mainActivity);
    }

    @Test
    public void fragmentsTest(){
        assertNull(mainFragment);
        assertNull(acFragment);
        assertNull(clientsFragment);
    }
}
