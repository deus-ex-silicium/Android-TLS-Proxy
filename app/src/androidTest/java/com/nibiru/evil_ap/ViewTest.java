package com.nibiru.evil_ap;

import com.nibiru.evil_ap.fragments.ClientsFragment;
import com.nibiru.evil_ap.fragments.MainFragment;

import org.junit.Test;

import static junit.framework.Assert.assertNull;

/**
 * Created by Wobbaf on 18/12/2016.
 */

public class ViewTest {
    IMVP.PresenterOps p;
    private MainActivity mainActivity;
    private MainFragment mainFragment;
    private ClientsFragment clientsFragment;

    @Test
    public void mainActivityTest(){
        assertNull(mainActivity);
    }

    @Test
    public void fragmentsTest(){
        assertNull(mainFragment);
        assertNull(clientsFragment);
    }
}
