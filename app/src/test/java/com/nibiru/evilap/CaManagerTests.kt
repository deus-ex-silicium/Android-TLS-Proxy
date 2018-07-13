package com.nibiru.evilap

import com.nibiru.evilap.pki.CaManager
import org.junit.Assert
import org.junit.Test
import org.spongycastle.jce.provider.BouncyCastleProvider
import java.security.Security


class CaManagerTests {

    @Test
    fun caSaveLoadTest() {
        Security.addProvider(BouncyCastleProvider())

        val ca = CaManager()
        Assert.assertTrue(true)
    }

}