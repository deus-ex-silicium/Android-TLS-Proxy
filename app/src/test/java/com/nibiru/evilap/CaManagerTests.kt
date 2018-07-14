package com.nibiru.evilap

import android.util.Log
import com.nibiru.evilap.pki.CaManager
import org.junit.Assert
import org.junit.Test
import org.spongycastle.cert.jcajce.JcaX509CertificateHolder
import java.security.cert.X509Certificate


class CaManagerTests {

    @Test
    fun caSaveLoadTest() {
        Log.d("Testing", "load/save functionality")
        //Security.addProvider(BouncyCastleProvider())

        val ca1 = CaManager()
        val ca1Cert = ca1.root
        ca1.save("evil-ca", "password")

        val ca2 = CaManager.load("evil-ca", "password")
        val ca2Cert = ca2.root
        val cn1 = CaManager.getCommonName(ca1Cert)
        val cn2 = CaManager.getCommonName(ca2Cert)

        Log.d("saved cert", cn1)
        Log.d("loaded cert", cn2)

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