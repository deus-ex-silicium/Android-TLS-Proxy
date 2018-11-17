package com.nibiru.tlsproxy

import android.util.Log
import com.nibiru.tlsproxy.crypto.CaManager
import org.junit.Assert
import org.junit.Test
import java.io.FileInputStream


class CaManagerTests {

    @Test
    fun caSaveLoadTest() {
        Log.d("Testing", "load/saveKeyStore functionality")
        //Security.addProvider(BouncyCastleProvider())

        val ca1 = CaManager()
        ca1.saveKeyStore("evil_ap.ks", "password")
        val ca1Cert = ca1.root

        val inS = FileInputStream("evil_ap.ks")
        val ca2 = CaManager(inS, "password")
        val ca2Cert = ca2.root
        val cn1 = CaManager.getCommonName(ca1Cert)
        val cn2 = CaManager.getCommonName(ca2Cert)

        Log.d("saved cert", cn1)
        Log.d("loaded cert", cn2)

        ca1.printCert()

        Assert.assertEquals(cn1, cn2)
    }

    @Test
    fun generateAndSignX509(){
        Log.d("Testing", "dynamic certificate generation")
        Log.d("Testing", "generating cert chain for example.com")
        val ca = CaManager()
        ca.generateAndSignCert("example.com")
        val chain = ca.getCertChain("example.com")
        val client = chain[0]
        val root = chain[1]
        val cn1 = CaManager.getCommonName(client)
        val cn2 = CaManager.getCommonName(root)
        Log.d("cert chain 1", "CN=$cn1")
        Log.d("cert chain 2", "CN=$cn2")
        Assert.assertEquals("example.com", cn1)
        Assert.assertEquals("FUNSEC Inc.", cn2)
    }



}