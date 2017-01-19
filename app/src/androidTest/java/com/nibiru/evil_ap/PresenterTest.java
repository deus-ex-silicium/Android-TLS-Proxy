package com.nibiru.evil_ap;

import org.junit.Before;
import org.junit.Test;

import static android.support.test.InstrumentationRegistry.getContext;
import static android.support.test.InstrumentationRegistry.getInstrumentation;
import static org.junit.Assert.*;

/**
 * Created by Wobbaf on 18/12/2016.
 */
public class PresenterTest {
    private IMVP.ModelOps m;
    @Before
    public void setUp() {
        IMVP.RequiredPresenterOps mPresenter = new IMVP.RequiredPresenterOps() {
            @Override
            public void onError(String errorMsg) {
                assertFalse(true);
            }
        };
        m = new Model(mPresenter, getInstrumentation().getContext(), true);
    }

    @Test
    public void isApOn() throws Exception {
        assertNotNull(m.isApOn(getContext()));
    }
//
//    @Test
//    public void apBtnPressed() throws Exception {
//        assertTrue(m.onApToggle("","",getInstrumentation().getContext()));
//    }

    @Test
    public void getCurrentClients() throws Exception {
        assertNotNull(m.getCurrentClients());
    }

    @Test
    public void getsetSharedPrefsInt() throws Exception {
        m.setSharedPrefsInt("testSetInt", 1);
        assertEquals(1, m.getSharedPrefsInt("testSetInt"));
    }

    @Test
    public void getsetSharedPrefsBool() throws Exception {
        m.setSharedPrefsBool("testSetBool", false);
        assertFalse(m.getSharedPrefsBool("testSetBool"));
    }

    @Test
    public void getsetSharedPrefsString() throws Exception {
        m.setSharedPrefsString("testSetString", "exampleString");
        assertEquals("exampleString", m.getSharedPrefsString("testSetString"));
    }

    @Test
    public void getSharedObj() throws Exception {
        assertNull(m.getSharedObj());
    }
}