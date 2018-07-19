package com.nibiru.evilap.pki

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

class EvilKeyManager(ca: CaManager?) : X509ExtendedKeyManager() {
    private val TAG = javaClass.simpleName
    private var ca: CaManager = ca ?: CaManager()
    var engine2Alias = HashMap<SSLEngine, String>()

    /**
     *  Empty constructor. Creates a new CA manager
     */
    constructor() : this(null)

    override fun getServerAliases(keyType: String?, issuers: Array<out Principal>?): Array<String> {
        TODO("not implemented")
    }

    override fun chooseEngineServerAlias(keyType: String?, issuers: Array<out Principal>?, engine: SSLEngine?): String {
        val server_name = engine2Alias[engine]
        Log.d(TAG, "SNI=($server_name)")
        ca.generateAndSignCert(server_name!!)
        return server_name
    }

    override fun chooseServerAlias(keyType: String?, issuers: Array<out Principal>?, socket: Socket?): String? {
        TODO("not implemented")
        // "Currently, the only server names (types) supported are DNS hostnames" -RFC 6066
        //val sock = socket as SSLSocket
        //TODO: why sometimes null ?
        // https://bitbucket.org/zmarcos/sniserversocket/wiki/Home
        // https://stackoverflow.com/questions/9622464/how-can-i-call-getsockopt-in-java-to-get-so-original-dst
        // https://stackoverflow.com/questions/10595575/iptables-configuration-for-transparent-proxy
        // https://www.programcreek.com/java-api-examples/index.php?api=javax.net.ssl.SNIMatcher
        // https://stackoverflow.com/questions/36323704/sni-client-side-mystery-using-java8
        // https://docs.oracle.com/javase/8/docs/technotes/guides/security/jsse/JSSERefGuide.html#ClientHelloParser
        //var handshake = sock.handshakeSession
        /*if (handshake==null){
            sock.startHandshake()
            handshake = sock.handshakeSession
        }*/
        //val sni = (handshake as ExtendedSSLSession).requestedServerNames[0] as SNIHostName
        //val server_name = sni.asciiName

        //val server_name = engine2Alias[socket]
/*
        Log.d(TAG, "SNI=($server_name)")
        ca.generateAndSignCert(server_name!!)
        return server_name*/
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