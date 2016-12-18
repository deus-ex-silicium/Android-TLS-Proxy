package com.nibiru.evil_ap;

import android.content.Context;
import android.graphics.PorterDuff;
import android.test.InstrumentationTestCase;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.notification.RunListener;

import java.security.spec.ECField;

import static android.support.test.InstrumentationRegistry.getContext;
import static android.support.test.InstrumentationRegistry.getInstrumentation;
import static org.junit.Assert.*;
import static org.junit.Assert.assertFalse;

/**
 * Created by Wobbaf on 17/12/2016.
 */
public class ModelTest {
    Model m;

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
    public void getSharedObjTest(){
        assertNull(m.getSharedObj());
    }

    @Test
    public void onApToggleTest(){
        assertNotNull(m.onApToggle("","",getContext()));
    }

    @Test
    public void isApOnTest(){
        assertNotNull(m.isApOn(getContext()));
    }

    @Test
    public void isDeviceRootedTest() {
        assertNotNull(m.isDeviceRooted());
    }

}