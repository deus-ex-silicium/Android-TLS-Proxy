package com.nibiru.evilap.crypto

import android.util.Log
import org.spongycastle.asn1.DERIA5String
import org.spongycastle.asn1.x500.X500Name
import org.spongycastle.asn1.x500.style.BCStyle
import org.spongycastle.asn1.x500.style.IETFUtils
import org.spongycastle.asn1.x509.*
import org.spongycastle.cert.X509CertificateHolder
import org.spongycastle.cert.X509v3CertificateBuilder
import org.spongycastle.cert.jcajce.JcaX509CertificateConverter
import org.spongycastle.cert.jcajce.JcaX509CertificateHolder
import org.spongycastle.openssl.jcajce.JcaPEMWriter
import org.spongycastle.operator.jcajce.JcaContentSignerBuilder
import java.io.FileOutputStream
import java.io.InputStream
import java.io.PrintWriter
import java.io.StringWriter
import java.math.BigInteger
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.PrivateKey
import java.security.cert.Certificate
import java.security.cert.X509Certificate
import java.util.*


// https://bouncycastle.org/docs/pkixdocs1.3/index.html
// http://www.baeldung.com/java-bouncy-castle

class CaManager(inputStream: InputStream?, pass: String?) {
    /**************************************CLASS FIELDS********************************************/
    private val TAG = javaClass.simpleName
    private lateinit var ks : KeyStore
    private lateinit var kp : KeyPair
    lateinit var root : X509Certificate
    var caAlias : String =  "evil-ca"
    /**************************************CLASS METHODS*******************************************/
    /**
     *  Empty constructor. Creates a new RSA key pair and self-signed CA X509 certificate
     */
    constructor() : this(null, null)
    /**
     * Generates a new CA or loads an existing one from a KeyStore file InputStream
     */
    init {
        if (inputStream==null || pass==null) generateCa()
        else {
            ks = KeyStore.getInstance(KEY_STORE_TYPE)
            val pwd = pass.toCharArray()
            ks.load(inputStream, pwd)
            inputStream.close()
            val kpriv = ks.getKey(caAlias, pwd) as PrivateKey
            val cert = ks.getCertificate(caAlias)
            kp = KeyPair(cert.publicKey, kpriv)
            root = certToX509(cert)
        }
    }
    /**
     * Static companion object.
     * KEY_STORE_TYPE contains the string used to identify the Keystore type
     * clientPass is the password for entries generated for clients by CA
     * load method loads an existing CA from the keystore, based on it's alias and password
     */
    companion object {
        private val KEY_STORE_TYPE : String = KeyStore.getDefaultType()
        private val clientPass : CharArray = "password".toCharArray()
        fun getCommonName(x509: X509Certificate): String {
            // https://stackoverflow.com/questions/2914521/how-to-extract-cn-from-x509certificate-in-java#7634755
            val x = JcaX509CertificateHolder(x509)
            val cn = x.subject.getRDNs(BCStyle.CN)[0]
            //TODO: what about multiple CNs ?
            return IETFUtils.valueToString(cn.first.value)
        }
    }

    private fun generateCa(){
        // http://blog.differentpla.net/blog/2013/03/24/bouncy-castle-being-a-certificate-authority
        // generate new RSA keypair
        val rsa = KeyPairGenerator.getInstance("RSA")
        rsa.initialize(2048)
        kp = rsa.generateKeyPair()
        // validity date of CA certificate
        val cal = Calendar.getInstance()
        cal.add(Calendar.MONTH, 12)
        // format public key for certificate
        val pk = kp.public.encoded
        val bcPk = SubjectPublicKeyInfo.getInstance(pk)
        val certGen = X509v3CertificateBuilder(
                X500Name("CN=FUNSEC Inc."), // X500Name representing the issuer of this certificate.
                BigInteger.ONE,             // the serial number for the certificate
                Date(),                     // date before which the certificate is not valid.
                cal.time,                   // date after which the certificate is not valid.
                X500Name("CN=FUNSEC Inc."), // X500Name representing the subject of this certificate.
                bcPk                        // the public key to be associated with the certificate.
        )
        // assert that this is a CA certificate
        certGen.addExtension(Extension.basicConstraints, true,  BasicConstraints(true))
        // add KeyUsage fields
        val ku = KeyUsage(KeyUsage.digitalSignature or KeyUsage.keyCertSign or KeyUsage.cRLSign)
        certGen.addExtension(Extension.keyUsage, true, ku)
        // add some Extended Key Usage fields
        // https://tools.ietf.org/html/rfc5280#section-4.2.1.12
        val eku = arrayOf(KeyPurposeId.anyExtendedKeyUsage,
                KeyPurposeId.id_kp_serverAuth, KeyPurposeId.id_kp_clientAuth,
                KeyPurposeId.id_kp_codeSigning, KeyPurposeId.id_kp_emailProtection,
                 KeyPurposeId.id_kp_timeStamping, KeyPurposeId.id_kp_OCSPSigning)
        certGen.addExtension(Extension.extendedKeyUsage, false, ExtendedKeyUsage(eku))
        // add Subject Alternative Names to (now wildcard) certificate
        val altName1 =  GeneralName(GeneralName.dNSName, DERIA5String("*.funsec"))
        val altNames = GeneralNames(arrayOf(altName1))
        certGen.addExtension(Extension.subjectAlternativeName, false, altNames)
        // self-sign
        val certHolder = certGen.build(JcaContentSignerBuilder("SHA256withRSA").build(kp.private))
        root = JcaX509CertificateConverter().getCertificate(certHolder)
        // load KeyStore, null for empty instance
        ks = KeyStore.getInstance(KEY_STORE_TYPE)
        ks.load(null)
    }

    fun generateAndSignCert(cn: String){
        // https://security.stackexchange.com/questions/56389/ssl-certificate-framework-101-how-does-the-browser-actually-verify-the-validity
        // https://stackoverflow.com/questions/5935369/ssl-how-do-common-names-cn-and-subject-alternative-names-san-work-together
        // https://boredwookie.net/blog/bouncy-castle-add-a-subject-alternative-name-when-creating-a-cer

        // Don't generate if entry already exists
        if(ks.containsAlias(cn)) return

        val rsa = KeyPairGenerator.getInstance("RSA")
        rsa.initialize(2048)
        val newKp = rsa.generateKeyPair()
        val from = Calendar.getInstance()
        from.add(Calendar.HOUR, -1)
        val to = Calendar.getInstance()
        to.add(Calendar.MONTH, 1)
        val pk = newKp.public.encoded
        val bcPk = SubjectPublicKeyInfo.getInstance(pk)
        //val serial = BigInteger.valueOf((ks.size()+2).toLong())
        val serial = BigInteger(64, Random())
        val certGen = X509v3CertificateBuilder(
                X500Name("CN=FUNSEC Inc."), // X500Name representing the issuer of this certificate.
                serial,                     // the serial number for the certificate
                from.time,                  // date before which the certificate is not valid.
                to.time,                    // date after which the certificate is not valid.
                X500Name("CN=$cn"),         // X500Name representing the subject of this certificate.
                bcPk                        // the public key to be associated with the certificate.
        )
        // assert that this is not a CA certificate
        certGen.addExtension(Extension.basicConstraints, true,  BasicConstraints(false))
        // add KeyUsage fields
        val ku = KeyUsage(KeyUsage.digitalSignature or KeyUsage.keyEncipherment)
        certGen.addExtension(Extension.keyUsage, true, ku)
        // add some Extended Key Usage fields
        val eku = arrayOf(KeyPurposeId.id_kp_serverAuth, KeyPurposeId.id_kp_clientAuth)
        certGen.addExtension(Extension.extendedKeyUsage, false, ExtendedKeyUsage(eku))
        // add Subject Alternative Names to (now wildcard) certificate
        val altName1 =  GeneralName(GeneralName.dNSName, DERIA5String(cn))
        val altName2 =  GeneralName(GeneralName.dNSName, DERIA5String("*.$cn"))
        val altNames = GeneralNames(arrayOf(altName1, altName2))
        certGen.addExtension(Extension.subjectAlternativeName, false, altNames)
        // sign with CA and store in KeyStore
        //val certHolder = certGen.build(JcaContentSignerBuilder("SHA512withRSA").build(kp.private))
        val certHolder = certGen.build(JcaContentSignerBuilder("SHA256withRSA").build(kp.private))
        val x509new = JcaX509CertificateConverter().getCertificate(certHolder)
        ks.setKeyEntry(cn, newKp.private, clientPass, arrayOf(x509new, root))
    }

    fun getPrivKey(cn:String): PrivateKey{
        return ks.getKey(cn, clientPass) as PrivateKey
    }

    fun getCertChain(cn: String): Array<X509Certificate> {
        val chain = ks.getCertificateChain(cn)
        val res = chain.map { certToX509(it) }
        return res.toTypedArray()
    }

    fun saveKeyStore(path: String, pass: String) {
        // https://www.stackoverflow.com/questions/6370368/bouncycastle-x509certificateholder-to-x509certificate#8960906
        val pwd = pass.toCharArray()
        ks.setKeyEntry(caAlias, kp.private, pwd, arrayOf(root))

        //for unit test compatibility
        //TLSProxyApp.instance.applicationContext.openFileOutput(path, MODE_PRIVATE).use { fos -> ks.store(fos, pwd) }
        FileOutputStream(path).use{ fos -> ks.store(fos, pwd) }
    }

    fun saveRootCert(name: String){
        //for unit test compatibility
        /*TLSProxyApp.instance.applicationContext.openFileOutput(name, MODE_PRIVATE).use {
            fos -> fos.write(toPemString(root).toByteArray())
        }*/
        PrintWriter(name).use { pw -> pw.print(toPemString(root)) }
    }

    fun printCert(){
        Log(toPemString(root))
    }

    fun printPubKey(){
        Log(toPemString(kp.public))
    }

    fun toPemString(o : Any): String{
        val textWriter = StringWriter()
        val pemWriter =  JcaPEMWriter(textWriter)
        pemWriter.writeObject(o)
        pemWriter.flush()
        return textWriter.toString()
    }

    private fun certToX509(cert: Certificate): X509Certificate{
        return JcaX509CertificateConverter().getCertificate(X509CertificateHolder(cert.encoded))
    }

    private fun Log(msg: String){
        Log.d(TAG, msg)
    }


}