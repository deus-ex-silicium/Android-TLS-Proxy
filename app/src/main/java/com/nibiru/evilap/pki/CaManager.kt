package com.nibiru.evilap.pki

import android.util.Log
import org.spongycastle.asn1.x500.X500Name
import org.spongycastle.asn1.x509.Certificate
import org.spongycastle.asn1.x509.SubjectPublicKeyInfo
import org.spongycastle.cert.X509CertificateHolder
import org.spongycastle.cert.X509v3CertificateBuilder
import org.spongycastle.cert.jcajce.JcaX509CertificateConverter
import org.spongycastle.openssl.jcajce.JcaPEMWriter
import org.spongycastle.operator.jcajce.JcaContentSignerBuilder
import java.io.StringWriter
import java.math.BigInteger
import java.security.*
import java.util.*


// https://bouncycastle.org/docs/pkixdocs1.3/index.html
// http://www.baeldung.com/java-bouncy-castle
class CaManager(cert:java.security.cert.Certificate?, kpriv: PrivateKey?) {
    /**************************************CLASS FIELDS********************************************/
    private val TAG = javaClass.simpleName
    private lateinit var ks : KeyStore
    private lateinit var kp : KeyPair
    private lateinit var certHolder : X509CertificateHolder
    private val keystorePassword = "password".toCharArray()
    private val keyPassword = "password".toCharArray()
    /**************************************CLASS METHODS*******************************************/
    constructor() : this(null, null)
    companion object {
        fun load(alias: String, pass: String) : CaManager {
            val ks = KeyStore.getInstance("PKCS12")
            ks.load(null)
            val cert = ks.getCertificate("$alias-cert")
            val priv = ks.getKey("$alias-kpriv", pass.toCharArray()) as PrivateKey
            return CaManager(cert, priv)
        }
    }
    init {
        if(cert==null || kpriv==null) generateCa()
        else{
            ks = KeyStore.getInstance("PKCS12")
            ks.load(null)
            kp = KeyPair(cert.publicKey, kpriv)
            certHolder = X509CertificateHolder(cert.encoded)
        }
    }

    private fun generateCa(){
        val rsa = KeyPairGenerator.getInstance("RSA")
        rsa.initialize(4096)
        kp = rsa.generateKeyPair()

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

        certHolder = certGen
                .build(JcaContentSignerBuilder("SHA512withRSA").build(kp.private))

        //https://www.stackoverflow.com/questions/6370368/bouncycastle-x509certificateholder-to-x509certificate#8960906
        ks = KeyStore.getInstance("PKCS12")
        ks.load(null)
    }

    fun save() {
        val x509 = JcaX509CertificateConverter().getCertificate(certHolder)
        ks.setKeyEntry("evil-ap-ca-kpriv", kp.private, keyPassword, arrayOf(x509))
        ks.setCertificateEntry("evil-ap-ca-cert", x509)
    }

    fun printCert(){
        Log(print(certHolder))
    }

    fun printPubKey(){
        Log(print(kp.public))
    }

    private fun Log(msg: String){
        Log.d(TAG, msg)
    }

    private fun print(o : Any): String{
        val textWriter = StringWriter()
        val pemWriter =  JcaPEMWriter(textWriter)
        pemWriter.writeObject(o)
        pemWriter.flush()
        return textWriter.toString()
    }

    /*private fun printANS1() {
        val input = ASN1InputStream(cert.encoded)
        var p: ASN1Primitive?
        while (true) {
            p = input.readObject()
            if (p == null) break
            Log.d(TAG, ASN1Dump.dumpAsString(p))
        }
    }*/
}