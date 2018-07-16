package com.nibiru.evilap.pki

import android.os.Build
import android.support.annotation.RequiresApi
import android.util.Log
import java.net.Socket
import java.security.Principal
import java.security.PrivateKey
import java.security.cert.X509Certificate
import javax.net.ssl.*

// https://idea.popcount.org/2012-06-16-dissecting-ssl-handshake/
// https://docs.oracle.com/javase/7/docs/technotes/guides/security/jsse/JSSERefGuide.html
// https://docs.oracle.com/javase/8/docs/technotes/guides/security/jsse/JSSERefGuide.html#SNIExtension
// http://www.angelfire.com/or/abhilash/site/articles/jsse-km/customKeyManager.html
// https://tools.ietf.org/html/rfc6066#page-6

class EvilKeyManager(ca: CaManager?) : X509KeyManager {
    private val TAG = javaClass.simpleName
    private var ca: CaManager = ca ?: CaManager()

    /**
     *  Empty constructor. Creates a new CA manager
     */
    constructor() : this(null)

    override fun getServerAliases(keyType: String?, issuers: Array<out Principal>?): Array<String> {
        TODO("not implemented")
    }

    @RequiresApi(Build.VERSION_CODES.N)
    override fun chooseServerAlias(keyType: String?, issuers: Array<out Principal>?, socket: Socket?): String? {
        // "Currently, the only server names (types) supported are DNS hostnames" -RFC 6066
        val sock = socket as SSLSocket
        //TODO: why sometimes null ?
        val handshake = sock.handshakeSession
        val sni = (handshake as ExtendedSSLSession).requestedServerNames[0] as SNIHostName
        Log.d(TAG, "SNI=(${sni.asciiName})")
        ca.generateAndSignCert(sni.asciiName)
        return sni.asciiName
    }

    override fun getCertificateChain(alias: String?): Array<X509Certificate> {
        return ca.getCertChain(alias!!)
    }

    override fun getPrivateKey(alias: String?): PrivateKey {
        return ca.getPrivKey(alias!!)
    }

    override fun chooseClientAlias(keyType: Array<out String>?, issuers: Array<out Principal>?, socket: Socket?): String {
        TODO("not implemented")
    }

    override fun getClientAliases(keyType: String?, issuers: Array<out Principal>?): Array<String> {
        TODO("not implemented")
    }
}