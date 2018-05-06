package com.nibiru.evilap.proxy

import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException


class InterceptorResponse : Interceptor {
    @Throws(IOException::class)
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalResponse = chain.proceed(chain.request())

        return originalResponse
    }
}