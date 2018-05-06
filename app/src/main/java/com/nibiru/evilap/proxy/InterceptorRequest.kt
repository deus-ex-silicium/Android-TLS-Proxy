package com.nibiru.evilap.proxy

import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException

class InterceptorRequest : Interceptor {
    @Throws(IOException::class)
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()

        val editedReq = originalRequest.newBuilder()
                .removeHeader("Accept-Encoding") //no compression, okhttp does it transparently
                .removeHeader("Upgrade-Insecure-Requests") //stay in http
                .removeHeader("Strict-Transport-Security") //don't do hsts
                .method(originalRequest.method(), originalRequest.body())
                .build()

        return chain.proceed(editedReq)
    }
}