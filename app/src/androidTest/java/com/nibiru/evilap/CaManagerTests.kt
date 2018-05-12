package com.nibiru.evilap

import android.support.test.runner.AndroidJUnit4
import com.nibiru.evilap.pki.CaManager
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.spongycastle.jce.provider.BouncyCastleProvider
import java.io.FileOutputStream
import java.io.OutputStream
import java.security.Security


@RunWith(AndroidJUnit4::class)
class CaManagerTests {

    @Test
    fun caSaveLoadTest() {
        Security.addProvider(BouncyCastleProvider())

        val ca = CaManager()
        Assert.assertTrue(true)
    }

}