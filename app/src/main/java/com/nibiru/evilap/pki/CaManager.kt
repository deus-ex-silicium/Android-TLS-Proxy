package com.nibiru.evilap.pki

import android.util.Base64
import android.util.Log
import org.spongycastle.asn1.ASN1InputStream
import org.spongycastle.asn1.ASN1Primitive
import org.spongycastle.asn1.util.ASN1Dump
import org.spongycastle.asn1.x500.X500Name
import org.spongycastle.asn1.x509.SubjectPublicKeyInfo
import org.spongycastle.cert.X509CertificateHolder
import org.spongycastle.cert.X509v3CertificateBuilder
import org.spongycastle.operator.jcajce.JcaContentSignerBuilder
import org.spongycastle.util.io.pem.PemReader
import java.io.StringReader
import java.math.BigInteger
import java.security.KeyPairGenerator
import java.util.*


// https://bouncycastle.org/docs/pkixdocs1.3/index.html
class CaManager(certStr: String?) {
    /**************************************CLASS FIELDS********************************************/
    private val TAG = javaClass.simpleName
    private lateinit var cert : X509CertificateHolder
    /**************************************CLASS METHODS*******************************************/
    constructor() : this(null)
    init {
        if(certStr==null) generateCa()
        else{
            val pem = PemReader(StringReader(certStr))
            val c = pem.readPemObject()
            if(c.type != "CERTIFICATE") throw IllegalArgumentException("Not a valid PEM certificate")
            cert = X509CertificateHolder(c.content)
            printB64()
        }
    }

    private fun generateCa(){
        val rsa = KeyPairGenerator.getInstance("RSA")
        rsa.initialize(4096)
        val kp = rsa.generateKeyPair()

        val cal = Calendar.getInstance()
        cal.add(Calendar.MONTH, 1)

        val pk = kp.public.encoded
        val bcPk = SubjectPublicKeyInfo.getInstance(pk)

        val certGen = X509v3CertificateBuilder(
                X500Name("CN=FUNSEC Inc."), // X500Name representing the issuer of this certificate.
                BigInteger.ONE, // the serial number for the certificate
                Date(), // date before which the certificate is not valid.
                cal.time, // date after which the certificate is not valid.
                X500Name("CN=FUNSEC Inc."), // X500Name representing the subject of this certificate.
                bcPk // the public key to be associated with the certificate.
        )

        val certHolder = certGen
                .build(JcaContentSignerBuilder("SHA512withRSA").build(kp.private))

        cert = certHolder
        printB64()
    }

    private fun printB64(){
        Log.d(TAG, "-----BEGIN CERTIFICATE-----\n")
        Log.d(TAG, Base64.encodeToString(cert.encoded, Base64.DEFAULT))
        Log.d(TAG, "\n-----END CERTIFICATE-----\n")
    }

    private fun printANS1() {
        val input = ASN1InputStream(cert.encoded)
        var p: ASN1Primitive?
        while (true) {
            p = input.readObject()
            if (p == null) break
            Log.d(TAG, ASN1Dump.dumpAsString(p))
        }
    }
}