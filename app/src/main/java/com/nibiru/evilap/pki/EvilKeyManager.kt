package com.nibiru.evilap.pki

import android.os.Build
import android.support.annotation.RequiresApi
import android.util.Log
import java.net.Socket
import java.security.Principal
import java.security.PrivateKey
import java.security.cert.X509Certificate
import javax.net.ssl.ExtendedSSLSession
import javax.net.ssl.SNIHostName
import javax.net.ssl.SSLSocket
import javax.net.ssl.X509KeyManager

// https://idea.popcount.org/2012-06-16-dissecting-ssl-handshake/
// https://docs.oracle.com/javase/7/docs/technotes/guides/security/jsse/JSSERefGuide.html
// https://docs.oracle.com/javase/8/docs/technotes/guides/security/jsse/JSSERefGuide.html#SNIExtension
// http://www.angelfire.com/or/abhilash/site/articles/jsse-km/customKeyManager.html

class EvilKeyManager : X509KeyManager {
    private val TAG = javaClass.simpleName

    override fun getServerAliases(keyType: String?, issuers: Array<out Principal>?): Array<String> {
        TODO("not implemented")
    }

    @RequiresApi(Build.VERSION_CODES.N)
    override fun chooseServerAlias(keyType: String?, issuers: Array<out Principal>?, socket: Socket?): String {
        val sock = socket as SSLSocket
        val sni = (sock.handshakeSession as ExtendedSSLSession).requestedServerNames[0] as SNIHostName
        Log.d(TAG, "SNI=($sni)")
        return sni.asciiName
    }

    override fun getCertificateChain(alias: String?): Array<X509Certificate> {
        TODO("not implemented")
    }

    override fun getPrivateKey(alias: String?): PrivateKey {
        TODO("not implemented")
    }

    override fun chooseClientAlias(keyType: Array<out String>?, issuers: Array<out Principal>?, socket: Socket?): String {
        TODO("not implemented")
    }

    override fun getClientAliases(keyType: String?, issuers: Array<out Principal>?): Array<String> {
        TODO("not implemented")
    }
}