package com.nibiru.tlsproxy.proxy

import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException
import okhttp3.ResponseBody

class InterceptorResponse : Interceptor {
    @Throws(IOException::class)
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalResponse = chain.proceed(chain.request())
        val orgBody = originalResponse.body()!!
        val newBody = ResponseBody.create(orgBody.contentType(), orgBody.bytes())
        originalResponse.close()
        return originalResponse.newBuilder()
                .removeHeader("Strict-Transport-Security")  // don't do HSTS
                .removeHeader("Content-Security-Policy")    // don't do CSP
                .removeHeader("X-XSS-Protection")           // don't do XSS protection
                .removeHeader("X-Frame-Options")            // don't do clickjacking protection
                .removeHeader("X-Content-Type-Options")     // make browser guess content
                .removeHeader("Transfer-Encoding")
                .header("Content-Length", newBody.contentLength().toString())
                .header("X-Forwarded-By", "Android-TLS-Proxy")
                .body(newBody)
                .build()
    }
}