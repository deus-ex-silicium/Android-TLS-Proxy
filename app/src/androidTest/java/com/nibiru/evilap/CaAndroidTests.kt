package com.nibiru.evilap

import android.support.test.InstrumentationRegistry
import android.support.test.runner.AndroidJUnit4
import android.util.Log
import com.nibiru.evilap.crypto.CaManager
import org.junit.Test
import org.junit.runner.RunWith


@RunWith(AndroidJUnit4::class)
class CaAndroidTests {

    @Test
    fun loadKeyStore(){
        Log.d("TESTING", "loading CA from keystore file")
        System.out.println("Working Directory = " + System.getProperty("user.dir"))

        val ctx = InstrumentationRegistry.getTargetContext()
        val ca = CaManager(ctx.resources.openRawResource(R.raw.attacker), "password")
        ca.generateAndSignCert("base.line")
        val chain = ca.getCertChain("base.line")
        Log.e("[0]", ca.toPemString(chain[0]))
        Log.e("[1]", ca.toPemString(chain[1]))
        Log.e("key", ca.toPemString(ca.getPrivKey("base.line")))
    }

}
